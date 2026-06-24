                             # Kairos — Full Specification (extends the README)

## 1. Purpose of This Document

This document records the architectural decisions made after the original
README: the data model, scheduling semantics, the execution lifecycle, and
the first API slice (Task CRUD). Exact table fields live in
`database.md`; the development breakdown lives in
`plan.md`. This document is about *why* things are built
this way.

## 2. Core Entities

- **Destination** — a delivery endpoint (Kafka / SQS / Webhook /
  RabbitMQ). Its config is stored separately from tasks so it isn't
  duplicated.
- **Task** — the definition of work: what to deliver, where, with what
  timeout and retry policy. A stable entity that changes rarely. Supports
  soft delete. Identity is `TaskId` (wraps a UUID). Editable state is
  carried by `TaskEdit` (a record that excludes immutable fields `id`,
  `service`, `createdAt`); mutation goes through `update(TaskEdit, Instant)`.
  The `service` field is immutable for the task's lifetime. Timestamps are
  supplied by the caller (application layer via `Clock`) so the entity
  stays deterministic and testable.
- **Schedule** — the "when" rule. A single task can have multiple
  schedules (e.g. weekdays and weekends as separate rules with different
  cron expressions). Pausing works at the level of a single schedule, not
  only the whole task.
- **Retry Policy** — explicit retry steps at the task level
  (attempt → delay), stored as a separate table instead of a single JSON field.
- **Execution (current)** — the current/upcoming work plan. A small, hot
  table — the engine's claim loop (`SKIP LOCKED`) operates on it.
- **Execution History (archive)** — an append-only archive of every
  completed run. The source for "Track" in
  `Store → Wait → Trigger → Retry → Track`.

## 3. Schedule Type Semantics

| Type | Meaning | Example | Notes |
|---|---|---|---|
| `ONCE` | a single specific occurrence | `2026-06-15T10:00:00Z` | materialized as one row; no further executions once it has run |
| `CRON` | a calendar-based rule | "every Saturday at 9am" | needs a cron parser, sensitive to timezone/DST |
| `FIXED` | a plain interval | "every 90 seconds" | NOT a calendar rule — `next = last + interval`, timezone doesn't apply |

## 4. Materialization (Planner)

Instead of evaluating cron expressions on the fly on every engine tick, a
separate component (the Planner) generates rows in `executions` ahead of
time, for some horizon:

- for infrequent schedules (daily/weekly) — a long horizon (a day or
  more), cheap to materialize;
- for high-frequency `FIXED` schedules (seconds/tens of seconds),
  materializing a full day ahead produces an unacceptable volume.
  Illustration: 100 tasks with a 10-second interval = 864,000 rows/day
  from those alone. That's why `FIXED` uses **recycling** instead: a
  single row per schedule, advanced forward
  (`next_attempt_at += interval`) on each completion rather than creating
  a new row.

**Catch-up:** if a schedule is created or updated and its next occurrence
already falls inside the currently materialized window, it's materialized
immediately rather than waiting for the next planned run.

**Self-healing:** if the previous planner run crashed without clearing its
"in progress" state, the next run must detect that and retry the missed
window. The exact mechanism (lock/lease) is still open — see
`database.md`.

## 5. Execution Lifecycle

```
PENDING ──(claim, SKIP LOCKED)──> CLAIMED ──(delivery)──┬─> SUCCESS  ──> archived into execution_history
                                                          └─> FAILED ──> any retry steps left?
                                                                          ├─ yes → RETRYING (next_attempt_at = now + delay)
                                                                          └─ no  → DEAD_LETTER → archived
```

For `ONCE`/`CRON`: the completed row moves into `execution_history` and is
removed from `executions`.

For `FIXED`: the completed row is copied into `execution_history`, and the
same row in `executions` is advanced to the next interval — atomically, in
the same transaction as the archive write (otherwise a crash between
delivery and advancing the row risks skipping a tick).

`current` answers "what's happening now/soon"; `history` answers "what
happened". The size of `current` doesn't depend on history volume — only
on how much work is actually due within the near horizon.

**Denormalized run summary on the task.** So the task listing can show run
state without joining the run tables, the engine maintains three cached
columns on the parent `tasks` row: `last_status` and `last_run_at` are
updated when an execution completes (the same outcome that lands in
`execution_history`), and `next_run_at` is updated when the next occurrence
is materialized. These fields are a derived cache, not the source of truth
— `executions` / `execution_history` stay authoritative, consistent with
the denormalization-vs-source-of-truth split elsewhere in this document.
The exact write semantics (`FIXED` vs `CRON` `next_run_at`, transactional
coupling to the run write) are still open — see `database.md`.

## 6. Pause / Disable — What Lives at Which Level

- `tasks.active = false` — a full kill-switch, disables the whole task
  regardless of how many schedules it has.
- `schedules.active = false` — pauses one specific rule (e.g. just the
  weekend schedule), while the task's other schedules keep running.
- `tasks.deleted_at` — soft delete; the task is excluded from all normal
  reads but never physically removed.

**Soft-delete invariant (M1, implemented):** once `deletedAt` is set on
a `Task` entity, the entity is frozen — any call to `update()` throws
`TaskAlreadyDeletedException` (extends `DomainException`). The predicate
`isDeleted()` exposes the current state. The application layer is
responsible for calling the soft-delete operation; the domain only enforces
the "no further mutations after deletion" rule.

## 7. Application Layer — Use Cases (M1, implemented)

The application layer orchestrates the domain via five use cases, all in
`dev.kairos.application.task.usecases`. Each use case receives its port(s) and a
`java.time.Clock` through its constructor — no field injection, no framework
dependency. Timestamps are always sourced from the injected clock so tests
can run with a fixed instant.

### Repository ports

**`TaskRepository`** (`dev.kairos.domain.task`):
- `save(Task)` — upsert: insert on first save, update thereafter. The entity
  is the source of truth for every column including `createdAt`/`updatedAt`.
- `findById(TaskId)` — returns the task **regardless of soft-delete state**.
  Callers inspect `task.isDeleted()` and decide how to react (read → 404,
  repeat-delete → 409).
- `findAll(int limit, int offset)` — live (non-deleted) rows only, newest
  first; used by `ListTasksUseCase`.
- `softDelete(TaskId, Instant)` — stamps `deleted_at`; the 404/409 decision
  is made by the caller before this is invoked.

**`DestinationRepository`** (`dev.kairos.domain.destination`; read-only in M1):
- `existsById(DestinationId)` — validates the destination FK before a task
  is created or updated, surfacing a `ValidationException` instead of a raw
  SQL foreign-key failure. A full Destinations API will grow this port in M2.

### Use-case contracts

| Use case | Inputs | Normal return | Domain exceptions |
|---|---|---|---|
| `CreateTaskUseCase` | `CreateTaskCommand`, `Clock` | `Task` | `ValidationException` (unknown destination or invalid field) |
| `UpdateTaskUseCase` | `TaskId`, `UpdateTaskCommand`, `Clock` | `Task` | `TaskNotFoundException` (missing/deleted); `ValidationException` (unknown destination or invalid field) |
| `GetTaskUseCase` | `TaskId` | `Task` | `TaskNotFoundException` (missing or soft-deleted) |
| `ListTasksUseCase` | `Pagination` | `List<Task>` | — |
| `SoftDeleteTaskUseCase` | `TaskId`, `Clock` | void | `TaskNotFoundException` (missing or soft-deleted) |

**Soft-delete visibility rules:**
- `GET`, `UPDATE`, `SOFT_DELETE` on a soft-deleted task all raise
  `TaskNotFoundException` (→ HTTP 404). A repeat `DELETE` therefore also
  returns 404, not 409. The domain method `Task.softDelete()` itself throws
  `TaskAlreadyDeletedException` as an internal guard, but `SoftDeleteTaskUseCase`
  pre-checks `isDeleted()` and raises `TaskNotFoundException` before reaching
  that guard — making the 409 path unreachable through the use case.
  **Contradiction with the API table in §8:** that table advertises 409 for
  repeat-delete. Aligning the use case behavior with the intended 409 semantic
  (by throwing `TaskAlreadyDeletedException` instead of `TaskNotFoundException`
  in the already-deleted branch of `SoftDeleteTaskUseCase`) is deferred to
  the controller / API layer milestone — a human should resolve this before
  controllers are written.

**Nullable boolean defaults:** `CreateTaskCommand` and `UpdateTaskCommand`
carry `active` and `supportsRetry` as nullable `Boolean`. When null the use
case falls back to the domain defaults: `active = true`,
`supportsRetry = false`.

**`service` immutability:** `CreateTaskCommand` includes `service`;
`UpdateTaskCommand` intentionally omits it — a task's owning service cannot
be changed after creation.

### Commands (`dev.kairos.application.task.commands`)

`CreateTaskCommand` and `UpdateTaskCommand` are plain Java records carrying
raw field values (no domain types). The API edge will map an incoming request
DTO to a command; the use case turns command fields into domain types and lets
the domain validate them.

## 8. API — V1 Scope (Task CRUD)

The first vertical slice covers Task only — no Schedule/Execution API yet
(those land in M3+ per the development plan).

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/tasks` | create a task |
| GET | `/api/v1/tasks/{id}` | fetch (404 if deleted/not found) |
| GET | `/api/v1/tasks` | list (excluding soft-deleted, paginated) |
| PUT | `/api/v1/tasks/{id}` | update |
| DELETE | `/api/v1/tasks/{id}` | soft delete |

Example create request body:

```json
{
  "service": "booking-service",
  "name": "expire-booking",
  "description": "Cancel reservation after timeout",
  "destinationId": "booking-kafka",
  "messageType": "booking.expire.v1",
  "payload": { "bookingId": "123" },
  "timeoutMs": 5000,
  "supportsRetry": true
}
```

The response is the same body plus `id`, `active: true`, `createdAt`,
`updatedAt`, `deletedAt: null`.

Errors: `400` — validation (e.g. missing `name` or a non-existent
`destinationId`), `404` — task not found or already soft-deleted, `409` —
attempting to delete an already-deleted task.

## 9. Architecture: DDD + Hexagonal

Kairos deliberately pairs Domain-Driven Design (tactical patterns) with
Hexagonal Architecture (Ports & Adapters) — partly as a learning exercise.
A few decisions worth recording so they don't get re-litigated later.

**Tactical, not strategic, DDD.** Kairos is a single-team infrastructure
service with little inherent business complexity — it explicitly doesn't
execute business logic. The strategic side of DDD (bounded-context
workshops, ubiquitous-language sessions with domain experts, context maps
between teams) would be ceremony without payoff here. The tactical side —
aggregates as consistency boundaries, value objects, invariants enforced
inside entities — earns its keep because it solves concrete problems we
already ran into, not because the domain itself is complex.

**Domain exception hierarchy (implemented in `common`):**
- `DomainException` (abstract) — base `RuntimeException` for all broken
  invariants and invalid-state errors. Lets the API layer catch one type
  and translate domain failures to HTTP responses.
- `ValidationException extends DomainException` — thrown by the shared
  `Validation` utility (`requireText`, `requirePositive`) when field-level
  constraints are violated (e.g. blank name, non-positive `timeoutMs`).
- `TaskAlreadyDeletedException extends DomainException` — thrown by
  `Task.update()` when the task is already soft-deleted.

The `Validation` utility class (`dev.kairos.common.util.helpers.Validation`)
provides two static guards used across the domain:
- `requireText(value, field, maxLength)` — rejects null/blank and values
  exceeding `maxLength`; returns the validated value for inline assignment.
- `requirePositive(value, field)` — rejects values `<= 0`; returns the
  validated value.

**Aggregate boundaries:**
- `Destination` — its own aggregate; just connectivity config.
- `Task` — the aggregate root. `RetryPolicy` lives inside it as a
  value-object collection — a retry step has no identity or meaning
  outside its task.
- `Schedule` — its own aggregate, referencing `Task` only by id. Keeping
  it separate matters in practice: pausing one schedule
  (`PATCH .../schedules/{id}/pause`) shouldn't require loading and locking
  the whole task plus every sibling schedule.
- `Execution` — its own aggregate, referencing `taskId`/`scheduleId` by id
  only. It has a different lifecycle and concurrency profile
  (claim/lock/retry, high write frequency) than Task/Schedule, and its
  state machine (§5) is the strongest case for rich domain behavior in the
  whole system.
- `ExecutionHistory` — not a rich aggregate. It's append-only with no
  mutation, so it's better understood as a read-model/log — closer to the
  read side of CQRS than a DDD aggregate.

**Hexagonal Architecture** governs dependency direction, not domain
modeling: the domain/application layers define ports (`TaskRepository`,
the Delivery Adapter interface) and know nothing about Postgres, Kafka, or
HTTP; infrastructure adapters implement those ports and depend inward.
This is what makes "swap Kafka, swap Postgres" from the original README
actually true, and it's why `kairos-adapters/*` can grow new delivery
mechanisms without touching `kairos-engine`. Onion Architecture and Clean
Architecture describe the same underlying idea — dependencies point
inward, domain at the center — under different names.

The Hexagonal boundary isn't proven by writing one adapter; it's proven
the first time a *second* adapter ships with zero changes to the domain or
application layers. Treat that as a concrete checkpoint (see M7 in the
development plan), not an assumption.

**Layer separation as realized in M1.** The `application` package
(`dev.kairos.application.task`) now physically exists alongside
`dev.kairos.domain.task`. Use cases and command records live in the
application package; the domain package contains only entities, value
objects, ports, and domain exceptions — no orchestration logic. The
infrastructure layer (`dev.kairos.infrastructure`) provides the concrete
port implementations:

- `JooqTaskRepository` (`dev.kairos.infrastructure.task`) implements
  `TaskRepository` using jOOQ. `save()` is an upsert
  (`INSERT ... ON CONFLICT (id) DO UPDATE`) covering all editable fields
  plus `updated_at` and `deleted_at`. The upsert deliberately never writes
  the engine-owned denormalized columns (`last_status`, `last_run_at`,
  `next_run_at`) — those are exclusively managed by the engine. Transaction
  management is left to the caller; the repository has no opinion on it.
- `TaskMapper` (`dev.kairos.infrastructure.task`, package-private) converts
  between `TasksRecord` (jOOQ-generated) and the `Task` domain entity. It
  lives in the infrastructure package so the domain stays free of any jOOQ
  types. JSONB columns are converted via `JSONB.valueOf(string)` /
  `jsonb.data()`; timestamps are converted between `OffsetDateTime` (jOOQ
  record) and `Instant` (domain entity) via UTC offset.
- `JooqDestinationRepository` (`dev.kairos.infrastructure.destination`)
  implements `DestinationRepository.existsById` via `DSLContext.fetchExists`.
- `DSLContextFactory` (`dev.kairos.infrastructure`) builds a shared
  `DSLContext` from a `DataSource` with `renderSchema = false` and
  `renderQuotedNames = NEVER`, and is injected into every repository at
  startup. Transaction management belongs to the application/API layer.

## 10. Future Work (Explicitly Out of V1)

- `execution_history.result` — the service's response to a delivered
  message. Will need a `correlation_id` attached by Kairos at delivery
  time, plus a way to match an async reply back to a specific execution.
- A full API for `destinations`, `schedules`, `retry_policies` — per the
  plan, M2–M4.
- Multi-tenancy, Admin UI, metrics — unchanged from the original Roadmap
  (V4–V5 in the README).

## 11. Related Documents

- `database.md` — exact table fields
- `plan.md` — the development plan by slice, with M1 (Task
  CRUD) as the starting point