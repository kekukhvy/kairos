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
  soft delete.
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

## 7. API — V1 Scope (Task CRUD)

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

## 8. Architecture: DDD + Hexagonal

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

## 9. Future Work (Explicitly Out of V1)

- `execution_history.result` — the service's response to a delivered
  message. Will need a `correlation_id` attached by Kairos at delivery
  time, plus a way to match an async reply back to a specific execution.
- A full API for `destinations`, `schedules`, `retry_policies` — per the
  plan, M2–M4.
- Multi-tenancy, Admin UI, metrics — unchanged from the original Roadmap
  (V4–V5 in the README).

## 10. Related Documents

- `database.md` — exact table fields
- `plan.md` — the development plan by slice, with M1 (Task
  CRUD) as the starting point