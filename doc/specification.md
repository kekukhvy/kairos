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
  duplicated. Identity is `DestinationId` (wraps a human-readable
  `String`, e.g. `booking-kafka`); equality and hashing are by id only.
  `type` (`DestinationType` enum: `KAFKA`, `SQS`, `WEBHOOK`, `RABBITMQ`)
  and `createdAt` are immutable after creation — changing the delivery
  mechanism is modelled as delete + re-create. `config` (a JSONB string)
  is mutable via `updateConfig(String)` and has a **declared per-type shape**
  enforced at the application layer. The entity is constructed via a
  `Builder`; `createdAt` is supplied by the application layer (via
  `Clock`) so the entity stays deterministic and testable. No soft-delete
  — destinations are hard-deleted.
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
  only the whole task. Identity is `ScheduleId` (wraps a UUID). `type`
  (`ScheduleType` enum: `ONCE`, `CRON`, `FIXED`) is immutable after
  creation — changing schedule type requires delete + recreate. Editable
  state is carried by `ScheduleEdit` (label, runAt, cronExpression,
  intervalSeconds, timezone); mutation goes through `update(ScheduleEdit,
  Instant)`, which applies only the "when" field belonging to the current
  type. Construction is only possible through the three factory methods
  `Schedule.once(...)`, `Schedule.cron(...)`, `Schedule.fixed(...)`, which
  enforce type invariants (see §3). The `Builder` is exposed for the
  persistence layer to rehydrate stored schedules; application code must
  use the factory methods. `createdAt` and `updatedAt` are supplied by the
  caller via `Clock`. References `Task` only by `TaskId` — it is never
  loaded through `Task`.
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

### Type invariants (enforced in domain AND DB — M3, implemented)

- **`ONCE`** — `runAt` required and must be strictly in the future at
  creation/update time. `cronExpression` and `intervalSeconds` must be null.
- **`CRON`** — non-blank `cronExpression` required; `timezone` must be a
  valid `java.time.ZoneId` string (defaults to `UTC` if omitted). Cron
  syntax is **not parsed** at this stage (deferred to a future cron builder).
  `runAt` and `intervalSeconds` must be null.
- **`FIXED`** — `intervalSeconds` required with `0 < intervalSeconds <=
  86400` (one day maximum). An interval longer than a day is a calendar
  concern and belongs to `CRON`, not a plain interval. `runAt` and
  `cronExpression` must be null. `Schedule.MAX_INTERVAL_SECONDS = 86_400`
  is the constant mirroring the DB CHECK added in `V7`.

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

## 7. Application Layer — Use Cases (M1 + M2 + M3, implemented)

The application layer orchestrates the domain via use cases in
`dev.kairos.application.task.usecases` (Task, M1),
`dev.kairos.application.destination.usecases` (Destination, M2), and
`dev.kairos.application.schedule.usecases` (Schedule, M3). Each use
case receives its port(s) and, where needed, a `java.time.Clock` through its
constructor — no field injection, no framework dependency. Timestamps are
always sourced from the injected clock so tests can run with a fixed instant.

### Repository ports

**`TaskRepository`** (`dev.kairos.domain.task`):
- `save(Task)` — upsert: insert on first save, update thereafter. The entity
  is the source of truth for every column including `createdAt`/`updatedAt`.
- `findById(TaskId)` — returns the task **regardless of soft-delete state**.
  Callers inspect `task.isDeleted()` and decide how to react (read → 404,
  repeat-delete → 409).
- `findAll(int limit, int offset)` — live (non-deleted) rows only, newest
  first. The filter is applied at the SQL level (`WHERE deleted_at IS NULL`)
  inside `JooqTaskRepository`; `ListTasksUseCase` delegates directly to the
  repository and performs no further in-memory filtering.
- `softDelete(TaskId, Instant)` — stamps `deleted_at`; the 404/409 decision
  is made by the caller before this is invoked.
- `existsByDestinationId(DestinationId)` — returns `true` if any task (including
  soft-deleted ones) still holds a reference to the given destination. Used by
  `DeleteDestinationUseCase` to enforce the referential-integrity guard.

**`DestinationRepository`** (`dev.kairos.domain.destination`; full CRUD as of M2):
- `existsById(DestinationId)` — validates the destination FK before a task
  is created or updated, surfacing a clean domain error instead of a raw
  SQL foreign-key failure.
- `save(Destination)` — upsert via `INSERT ... ON CONFLICT (id) DO UPDATE`.
  `createdAt` is set only on insert and is never overwritten on conflict.
- `findById(DestinationId)` — returns `Optional<Destination>`, empty if not found.
- `findAll(int limit, int offset)` — returns all destinations, newest first
  (ordered by `created_at DESC`). No soft-delete state — this always
  reflects the full live set.
- `deleteById(DestinationId)` — hard delete; removes the row permanently.
  Referential-integrity (no tasks in use) is enforced by the caller, not here.

**`ScheduleRepository`** (`dev.kairos.domain.schedule`):
- `save(Schedule)` — upsert: insert on first save, update thereafter. The
  entity is the source of truth for every column.
- `findById(ScheduleId)` — returns `Optional<Schedule>`, empty if not found.
- `findByTaskId(TaskId taskId, int limit, int offset)` — lists a task's
  schedules, newest first, paginated.
- `deleteById(ScheduleId)` — idempotent hard delete; deleting an absent
  schedule is not an error (no prior existence check, single round trip).

### Use-case contracts

**Task use cases** (`dev.kairos.application.task.usecases`):

| Use case | Inputs | Normal return | Domain exceptions |
|---|---|---|---|
| `CreateTaskUseCase` | `CreateTaskCommand`, `Clock` | `Task` | `ValidationException` (unknown destination or invalid field) |
| `UpdateTaskUseCase` | `TaskId`, `UpdateTaskCommand`, `Clock` | `Task` | `TaskNotFoundException` (missing/deleted); `ValidationException` (unknown destination or invalid field) |
| `GetTaskUseCase` | `TaskId` | `Task` | `TaskNotFoundException` (missing or soft-deleted) |
| `ListTasksUseCase` | `Pagination` | `List<Task>` | — |
| `SoftDeleteTaskUseCase` | `TaskId`, `Clock` | void | `TaskNotFoundException` (task not found); `TaskAlreadyDeletedException` (task already soft-deleted) |

**Destination use cases** (`dev.kairos.application.destination.usecases`):

| Use case | Inputs | Normal return | Domain exceptions |
|---|---|---|---|
| `CreateDestinationUseCase` | `CreateDestinationCommand`, `Clock` | `Destination` | `DestinationAlreadyExistsException` (id already taken); `InvalidDestinationTypeException` (unrecognised `destinationType` string); `ValidationException` (config missing required key, invalid JSON, or not a JSON object) |
| `GetDestinationByIdUseCase` | `DestinationId` | `Destination` | `DestinationNotFoundException` (no row for the id) |
| `ListDestinationsUseCase` | `Pagination` | `List<Destination>` | — |
| `UpdateDestinationUseCase` | `DestinationId`, `String config` | `Destination` | `DestinationNotFoundException` (no row for the id); `ValidationException` (config missing required key, invalid JSON, or not a JSON object) |
| `DeleteDestinationUseCase` | `DestinationId` | void | `DestinationInUseException` (at least one task still references the destination) |

**Schedule use cases** (`dev.kairos.application.schedule.usecases`):

| Use case | Inputs | Normal return | Domain exceptions |
|---|---|---|---|
| `CreateScheduleUseCase` | `CreateScheduleCommand`, `Clock` | `Schedule` | `TaskNotFoundException` (task missing or deleted); `ValidationException` (invalid type/field combination) |
| `GetScheduleByIdUseCase` | `ScheduleId` | `Schedule` | `ScheduleNotFoundException` (no row for the id) |
| `ListSchedulesByTaskUseCase` | `TaskId`, `Pagination` | `List<Schedule>` | — |
| `UpdateScheduleUseCase` | `ScheduleId`, `UpdateScheduleCommand`, `Clock` | `Schedule` | `ScheduleNotFoundException` (no row for the id); `ValidationException` (invalid when-field for type) |
| `DeleteScheduleUseCase` | `ScheduleId` | void | — (idempotent) |
| `SetScheduleActiveUseCase` | `ScheduleId`, `boolean active`, `Clock` | `Schedule` | `ScheduleNotFoundException` (no row for the id) |

**Schedule creation details:** `CreateScheduleUseCase` first verifies that
the target task exists and is not soft-deleted (via `TaskRepository.findById`);
a missing or deleted task raises `TaskNotFoundException`. The raw `type` string
from `CreateScheduleCommand` is parsed by `ScheduleType.parse()`, which
raises `ValidationException` for unknown or blank values. The matching factory
method (`Schedule.once`, `Schedule.cron`, or `Schedule.fixed`) is then called,
enforcing the type-specific field invariants.

**Schedule update scope:** `UpdateScheduleUseCase` applies `ScheduleEdit` via
`Schedule.update(edit, now)`. Only the "when" field belonging to the schedule's
current type is changed (`runAt` for `ONCE`, `cronExpression` for `CRON`,
`intervalSeconds` for `FIXED`); `label` and `timezone` are also updatable.
`type` is immutable — changing type requires delete + recreate.

**Pause/resume:** `SetScheduleActiveUseCase` delegates to `schedule.pause(now)`
or `schedule.resume(now)` and saves the result. Both operations are independent
of the task's own `active` flag.

**Destination creation details:** `destinationId` is caller-supplied (human-readable,
e.g. `booking-kafka`) rather than auto-generated. A duplicate-id check via
`existsById` runs before the row is written. `createdAt` is stamped from the
injected `Clock`. The raw `destinationType` string from `CreateDestinationCommand`
is parsed to `DestinationType` via `DestinationType.valueOf()`; an unknown value
raises `InvalidDestinationTypeException`. The `config` field is validated against
the per-type schema before the destination is built (see **Config schema** below).

**Config schema:** `dev.kairos.common.destination.DestinationConfigSchema` (in the
`common` module) is the single source of truth for the shape of a destination's
`config` — which JSON object keys are required vs. optional for each delivery type.
It is kept in `common` (rather than duplicated or imported from `kairos-api`) so
that both `kairos-api` and `kairos-admin` can reach it without the admin module
depending on the API module (which would invert the dependency graph). The schema
is used by the application layer (not the domain, which remains framework-free) to
validate the `config` JSON string before it reaches the entity.

`DestinationType` lives in `common` alongside the schema, and the domain uses that
same enum — there is deliberately **one** `DestinationType`, not a domain copy plus
a mirror. A mirrored enum would be coupled to its twin only by a string
(`valueOf(name())`), so adding a constant to one and not the other would compile
cleanly and fail at runtime. Being plain Java, an enum in `common` keeps the domain
framework-free, and `domain → common` is an edge that already exists (`Validation`,
`ValidationException`).

Schema per type:
| Type | Required | Optional |
|---|---|---|
| `KAFKA` | `topic` | `key`, `headers` |
| `SQS` | `queueUrl` | `messageGroupId` |
| `WEBHOOK` | `url` | `method`, `headers` |
| `RABBITMQ` | `exchange`, `routingKey` | `headers` |

Validation semantics:
- Only key **presence** is enforced; values are never inspected. A config like
  `{"topic": ""}` is valid — the prefilled UI template must itself be submittable.
- Keys **beyond** the required ∪ optional set are **accepted and stored untouched**.
  Delivery adapters may read custom parameters from the config later (M7+).
- A config that is not a JSON object (e.g. an array or scalar) is rejected.
- The schema is reachable from both `kairos-api` and `kairos-admin` without module
  coupling, and provides templates for UI prefill (e.g. KAFKA → `{"topic": ""}`).
  This contract — that the schema is per-type, values are unchecked, and extra
  keys are accepted — is what the "second adapter, zero domain changes" hexagonal
  payoff depends on.

**Destination deletion semantics:** deletion is idempotent — no existence check
is performed before the `deleteById` call (single round trip, standard DELETE
semantics). However, deletion is blocked while any task (including soft-deleted
tasks) still references the destination; `TaskRepository.existsByDestinationId`
queries `tasks.destination_id` without filtering on `deleted_at`, so a
soft-deleted task is enough to block the delete. `DestinationInUseException` is
raised if the check returns `true`.

**Soft-delete visibility rules:**
- `GET` and `UPDATE` on a soft-deleted task raise `TaskNotFoundException`
  (→ HTTP 404).
- `DELETE` on a task that was already soft-deleted raises
  `TaskAlreadyDeletedException` (→ HTTP 409). The use case calls
  `taskRepository.findById` (which returns the row regardless of
  soft-delete state), then delegates to `task.softDelete(now)`. If the task
  is already deleted, `Task.softDelete()` throws `TaskAlreadyDeletedException`
  directly; the use case does **not** pre-check `isDeleted()` for the delete
  path, so the 409 surfaces naturally without an extra branch.

**Nullable boolean defaults:** `CreateTaskCommand` and `UpdateTaskCommand`
carry `active` and `supportsRetry` as nullable `Boolean`. When null the use
case falls back to the domain defaults: `active = true`,
`supportsRetry = false`.

**`service` immutability:** `CreateTaskCommand` includes `service`;
`UpdateTaskCommand` intentionally omits it — a task's owning service cannot
be changed after creation.

### Commands

**`dev.kairos.application.task.commands`:** `CreateTaskCommand` and
`UpdateTaskCommand` are plain Java records carrying raw field values (no domain
types). The API edge maps an incoming request DTO to a command; the use case
turns command fields into domain types and lets the domain validate them.

**`dev.kairos.application.destination.commands`:** `CreateDestinationCommand`
is a plain Java record with three `String` fields — `destinationId`,
`destinationType`, and `config` — matching the same pattern: the API edge maps
the request DTO to the command; the use case converts `destinationType` to
`DestinationType` and `destinationId` to `DestinationId`.

**`dev.kairos.application.schedule.commands`:**
- `CreateScheduleCommand` — plain Java record with `taskId` (`String`),
  `type` (`String`), `label` (`String`, nullable), `runAt` (`Instant`,
  nullable), `cronExpression` (`String`, nullable), `intervalSeconds`
  (`Integer`, nullable), and `timezone` (`String`, nullable). Only the
  "when" field matching `type` is expected to be non-null; the use case
  routes to the appropriate factory method.
- `UpdateScheduleCommand` — plain Java record with `label`, `runAt`,
  `cronExpression`, `intervalSeconds`, and `timezone`. `type` is absent
  (immutable); the use case builds a `ScheduleEdit` from these fields and
  delegates to `Schedule.update`.

## 8. API — V1 Scope (Task CRUD + Destination CRUD + Schedule CRUD)

The first three vertical slices cover Task, Destination, and Schedule CRUD.
The Execution API is not yet exposed (M5+, per the development plan). The
Schedule domain, application, and HTTP layers are all fully implemented as of
M3: entity with factory methods, six use cases, repository port, and the seven
REST endpoints are live.

**HTTP framework:** Javalin 6.4.0 (`io.javalin:javalin`). The server port
is read from `server.port` in `application.properties` (default `8080`).
A route overview is available at `/routes` (Javalin bundled plugin).

### Endpoints

**Tasks:**

| Method | Path | Success status | Description |
|---|---|---|---|
| `POST` | `/api/v1/tasks` | 201 | create a task |
| `GET` | `/api/v1/tasks/{id}` | 200 | fetch (404 if deleted or not found) |
| `GET` | `/api/v1/tasks` | 200 | list (excluding soft-deleted, paginated) |
| `PUT` | `/api/v1/tasks/{id}` | 200 | update (full replacement of editable fields) |
| `DELETE` | `/api/v1/tasks/{id}` | 204 | soft delete — stamps `deleted_at`, no response body |

**Destinations:**

| Method | Path | Success status | Description |
|---|---|---|---|
| `POST` | `/api/v1/destinations` | 201 | create a destination |
| `GET` | `/api/v1/destinations/{id}` | 200 | fetch (404 if not found) |
| `GET` | `/api/v1/destinations` | 200 | list all destinations, paginated |
| `PUT` | `/api/v1/destinations/{id}` | 200 | update config (only field allowed to change) |
| `DELETE` | `/api/v1/destinations/{id}` | 204 | hard delete — blocked if any task references the destination |

**Schedules:**

Create and list are nested under a task; all other operations address a
schedule directly by id.

| Method | Path | Success status | Description |
|---|---|---|---|
| `POST` | `/api/v1/tasks/{taskId}/schedules` | 201 | create a schedule for a task (404 if task missing or deleted) |
| `GET` | `/api/v1/tasks/{taskId}/schedules` | 200 | list a task's schedules, paginated |
| `GET` | `/api/v1/schedules/{id}` | 200 | fetch one schedule (404 if not found) |
| `PUT` | `/api/v1/schedules/{id}` | 200 | update when-field + label + timezone; type immutable |
| `DELETE` | `/api/v1/schedules/{id}` | 204 | hard delete — idempotent, no 404 on absent id |
| `PATCH` | `/api/v1/schedules/{id}/pause` | 200 | set `active = false`; returns updated `ScheduleResponse` |
| `PATCH` | `/api/v1/schedules/{id}/resume` | 200 | set `active = true`; returns updated `ScheduleResponse` |

### Exception → HTTP status mapping (`GlobalExceptionHandler`)

| Exception | HTTP status | Notes |
|---|---|---|
| `ValidationException` | 400 | field-level constraint violations (blank name, invalid type, missing/forbidden when-field, past `runAt`, `intervalSeconds` out of bounds, invalid timezone; also destination config missing required keys, invalid JSON, or not a JSON object) |
| `IllegalArgumentException` | 400 | malformed path param (e.g. non-UUID `{id}`) |
| `InvalidDestinationTypeException` | 400 | unrecognised `destinationType` string on destination create |
| `JacksonException` | 400 | malformed or unparseable request body |
| `TaskNotFoundException` | 404 | task missing or already soft-deleted (GET/PUT on task; POST schedule to deleted/missing task) |
| `DestinationNotFoundException` | 404 | destination not found (GET/PUT) |
| `ScheduleNotFoundException` | 404 | schedule not found (GET/PUT/PATCH/DELETE on schedule) |
| `TaskAlreadyDeletedException` | 409 | repeat DELETE on an already-deleted task |
| `DestinationAlreadyExistsException` | 409 | destination id already taken on create |
| `DestinationInUseException` | 409 | at least one task still references the destination on delete |
| any other `Exception` | 500 | logged server-side; body is `{"error":"Internal server error"}` |

All error bodies use `ErrorResponse(String error)` — a single `error` field
with a human-readable message.

### DTO contracts (`common` module)

**`CreateTaskRequest`** — body for `POST /api/v1/tasks`:

| Field | Java type | Notes |
|---|---|---|
| `service` | `String` | owning service name |
| `name` | `String` | required (validated by domain) |
| `description` | `String` | nullable |
| `active` | `Boolean` | nullable → domain default `true` |
| `destinationId` | `String` | must reference an existing destination |
| `eventName` | `String` | required; machine-readable, versioned event identifier used by the consumer for routing/handler selection (e.g. `booking.expire.v1`); deliberately distinct from the human-readable `name` |
| `payload` | `JsonNode` | any valid JSON value; nullable → stored as JSONB |
| `timeoutMs` | `int` | must be > 0 |
| `supportsRetry` | `Boolean` | nullable → domain default `false` |

**`UpdateTaskRequest`** — body for `PUT /api/v1/tasks/{id}`:

Same fields as `CreateTaskRequest` **minus `service`** — the owning service
is immutable after creation. `active` and `supportsRetry` nullable with the
same defaults.

**`TaskResponse`** — body for all successful task reads (200, 201):

| Field | Java type | Notes |
|---|---|---|
| `id` | `UUID` | |
| `service` | `String` | |
| `name` | `String` | |
| `description` | `String` | nullable |
| `active` | `boolean` | |
| `destinationId` | `String` | |
| `eventName` | `String` | machine-readable, versioned event identifier for consumer routing (e.g. `booking.expire.v1`) |
| `payload` | `JsonNode` | embedded as a real JSON node, not an escaped string; null if not set |
| `timeoutMs` | `int` | |
| `supportsRetry` | `boolean` | |
| `createdAt` | `Instant` | ISO-8601 string |
| `updatedAt` | `Instant` | ISO-8601 string |

`deletedAt` is intentionally absent — deleted tasks are never returned;
callers receive 404 instead.

**`PageResponse<T>`** — wrapper for all paginated list endpoints (`GET /api/v1/tasks`, `GET /api/v1/destinations`):

```json
{
  "items": [ ...TaskResponse or DestinationResponse... ],
  "limit": 20,
  "offset": 0,
  "hasNext": true
}
```

Query parameters: `limit` (nullable, default applied by `Pagination`) and
`offset` (nullable, default `0`).

`hasNext` is computed by over-fetching `limit + 1` rows from the repository.
If the result set size exceeds `limit`, `hasNext` is `true` and the extra row
is stripped before the response is serialised. This avoids a separate
`COUNT(*)` query.

**`CreateDestinationRequest`** — body for `POST /api/v1/destinations`:

| Field | Java type | Notes |
|---|---|---|
| `destinationId` | `String` | caller-supplied human-readable id (e.g. `booking-kafka`); must be unique |
| `destinationType` | `String` | case-sensitive enum name: `KAFKA`, `SQS`, `WEBHOOK`, `RABBITMQ` |
| `config` | `JsonNode` | any valid JSON value; serialised to JSONB |

**`UpdateDestinationRequest`** — body for `PUT /api/v1/destinations/{id}`:

| Field | Java type | Notes |
|---|---|---|
| `config` | `JsonNode` | replacement connectivity config; only field allowed to change |

**`DestinationResponse`** — body for all successful destination reads (200, 201):

| Field | Java type | Notes |
|---|---|---|
| `destinationId` | `String` | human-readable id |
| `destinationType` | `String` | enum name (`KAFKA`, `SQS`, `WEBHOOK`, `RABBITMQ`) |
| `config` | `JsonNode` | embedded as a real JSON node, not an escaped string |
| `createdAt` | `Instant` | ISO-8601 string; immutable |

**`CreateScheduleRequest`** — body for `POST /api/v1/tasks/{taskId}/schedules`
(`dev.kairos.common.dto.schedule`):

| Field | Java type | Notes |
|---|---|---|
| `type` | `String` | required; `ONCE`, `CRON`, or `FIXED` (case-sensitive enum name) |
| `label` | `String` | optional human-readable name (e.g. `weekday-morning`) |
| `runAt` | `Instant` | required for `ONCE`; must be in the future; null for other types |
| `cronExpression` | `String` | required for `CRON`; null for other types |
| `intervalSeconds` | `Integer` | required for `FIXED` (0 < n <= 86400); null for other types |
| `timezone` | `String` | optional; any valid `ZoneId` string; defaults to `UTC` |

**`UpdateScheduleRequest`** — body for `PUT /api/v1/schedules/{id}`
(`dev.kairos.common.dto.schedule`):

| Field | Java type | Notes |
|---|---|---|
| `label` | `String` | nullable |
| `runAt` | `Instant` | applied only if schedule type is `ONCE` |
| `cronExpression` | `String` | applied only if schedule type is `CRON` |
| `intervalSeconds` | `Integer` | applied only if schedule type is `FIXED` |
| `timezone` | `String` | any valid `ZoneId` string; required (validated) |

**`ScheduleResponse`** — body for all successful schedule reads (200, 201)
(`dev.kairos.common.dto.schedule`):

| Field | Java type | Notes |
|---|---|---|
| `id` | `UUID` | |
| `taskId` | `UUID` | owning task |
| `type` | `String` | enum name: `ONCE`, `CRON`, or `FIXED` |
| `label` | `String` | nullable |
| `runAt` | `Instant` | non-null for `ONCE`; null for `CRON`/`FIXED` |
| `cronExpression` | `String` | non-null for `CRON`; null for `ONCE`/`FIXED` |
| `intervalSeconds` | `Integer` | non-null for `FIXED`; null for `ONCE`/`CRON` |
| `timezone` | `String` | `ZoneId` string; always present (default `UTC`) |
| `active` | `boolean` | whether this rule is currently active |
| `createdAt` | `Instant` | ISO-8601 string |
| `updatedAt` | `Instant` | ISO-8601 string |

**`ErrorResponse`** — body for all 4xx/5xx:

```json
{ "error": "human-readable message" }
```

### ObjectMapper configuration (`ObjectMapperFactory`)

One shared `ObjectMapper` instance is created at startup and injected into
the Javalin JSON mapper (`JavalinJackson`) and all handlers/mappers
(`TaskHandler`/`TaskDtoMapper`, `DestinationHandler`/`DestinationDtoMapper`):

- `JavaTimeModule` registered — `Instant` serializes as an ISO-8601 string.
- `WRITE_DATES_AS_TIMESTAMPS = false` — human-readable dates, not numeric arrays.
- `FAIL_ON_UNKNOWN_PROPERTIES = false` — forward-compatible: extra fields from
  newer clients are silently ignored.

### Payload handling

`JsonNode payload` in the request is serialized to a JSON string
(`JsonConverter.jsonToString`) before being passed to the command/domain layer,
where it is stored as JSONB. On the response path, `TaskDtoMapper.toResponse`
parses the stored string back to a `JsonNode` so the response embeds payload
as a real JSON object rather than an escaped string. A null or JSON-null node
results in a null `payload` field in the response.

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

**Domain exception hierarchy (implemented in `common` and domain packages):**
- `DomainException` (abstract, `common`) — base `RuntimeException` for all
  broken invariants and invalid-state errors. Lets the API layer catch one
  type and translate domain failures to HTTP responses.
- `ValidationException extends DomainException` (`common`) — thrown by the
  shared `Validation` utility (`requireText`, `requirePositive`) when
  field-level constraints are violated (e.g. blank name, non-positive
  `timeoutMs`).
- `TaskAlreadyDeletedException extends DomainException` — thrown by
  `Task.update()` and `Task.softDelete()` when the task is already
  soft-deleted. Maps to HTTP 409 at the API layer.
- Destination exceptions (all in `dev.kairos.domain.destination.exceptions`,
  all extending `DomainException`):
  - `DestinationAlreadyExistsException` — raised by `CreateDestinationUseCase`
    when the caller-supplied id is already in use.
  - `DestinationNotFoundException` — raised by `GetDestinationByIdUseCase` and
    `UpdateDestinationUseCase` when no row exists for the given id.
  - `InvalidDestinationTypeException` — raised by `CreateDestinationUseCase`
    when `destinationType` does not match any `DestinationType` enum constant.
  - `DestinationInUseException` — raised by `DeleteDestinationUseCase` when at
    least one task still references the destination.
- `ScheduleNotFoundException extends DomainException` (`dev.kairos.domain.schedule`)
  — raised by `GetScheduleByIdUseCase`, `UpdateScheduleUseCase`, and
  `SetScheduleActiveUseCase` when no schedule exists for the given id. Maps
  to HTTP 404 at the API layer.

The `Validation` utility class (`dev.kairos.common.util.helpers.Validation`)
provides static guards used across the domain:
- `requireText(value, field, maxLength)` — rejects null/blank and values
  exceeding `maxLength`; returns the validated value for inline assignment.
- `requireText(value, field)` — two-argument overload (no length cap); rejects
  null/blank only. Used by `Destination.validateConfig` to validate the
  `config` field.
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
port implementations, and the HTTP layer (`dev.kairos.api`) sits at the
outermost ring, depending on the application layer but unknown to it:

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
  implements the full `DestinationRepository` port: `existsById` via
  `DSLContext.fetchExists`; `save` as an `INSERT ... ON CONFLICT (id) DO UPDATE`
  that never overwrites `created_at`; `findById` and `findAll`
  (`ORDER BY created_at DESC`); `deleteById` as a hard delete. A private
  `toDomain(DestinationsRecord)` helper converts the jOOQ record to the domain
  entity — `JSONB.data()` for the config column, `OffsetDateTime.toInstant()`
  for timestamps, `DestinationType.valueOf(record.getType())` for the enum.
- `DSLContextFactory` (`dev.kairos.infrastructure`) builds a shared
  `DSLContext` from a `DataSource` with `renderSchema = false` and
  `renderQuotedNames = NEVER`, and is injected into every repository at
  startup. Transaction management belongs to the application/API layer.
- `ObjectMapperFactory` (`dev.kairos.infrastructure`) produces the single
  shared `ObjectMapper` (see §8 for configuration). It is wired into the
  Javalin JSON mapper, `TaskHandler`, and `DestinationHandler` at startup.
- `Router` (`dev.kairos.api`) creates the `Javalin` instance, registers the
  `GlobalExceptionHandler`, and exposes `registerTaskRoutes`,
  `registerDestinationRoutes`, and `registerScheduleRoutes` to attach handler
  method references. Routes are registered as method references
  (`taskHandler::list`, `destinationHandler::create`, `scheduleHandler::create`,
  etc.), keeping handlers free of Javalin types except
  `io.javalin.http.Context`.
- `TaskHandler` (`dev.kairos.api.task`) translates HTTP context to commands
  and delegates to the use cases. It holds a reference to `ObjectMapper`
  solely for `JsonConverter.jsonToString` (request → command) and
  `TaskDtoMapper.toResponse` (domain entity → response DTO).
- `TaskDtoMapper` (`dev.kairos.api.task`, package-private) performs the
  `Task` → `TaskResponse` mapping, including the payload string → `JsonNode`
  conversion for the response.
- `DestinationHandler` (`dev.kairos.api.destination`) translates HTTP context
  to the five destination use-case calls. Holds an `ObjectMapper` reference
  for `JsonConverter.jsonToString` (config `JsonNode` → `String` for the
  command) and `DestinationDtoMapper.toResponse`. The `list` handler
  over-fetches `limit + 1` rows, computes `hasNext`, strips the extra item,
  and serialises a `PageResponse<DestinationResponse>`.
- `DestinationDtoMapper` (`dev.kairos.api.destination`, package-private)
  performs the `Destination` → `DestinationResponse` mapping, including the
  config string → `JsonNode` conversion via `JsonConverter.parseJson`.
- `JsonConverter` (`dev.kairos.common.util.helpers`) serializes an inbound
  `JsonNode` to a JSON string for the domain/storage layer. A null or
  JSON-null node returns null; a serialization failure throws
  `ValidationException`.
- `JooqScheduleRepository` (`dev.kairos.infrastructure.schedule`) implements
  `ScheduleRepository` using jOOQ. `save()` is an upsert
  (`INSERT ... ON CONFLICT (id) DO UPDATE`). `findByTaskId` orders by
  `created_at DESC`. `deleteById` is a plain `DELETE` with no prior existence
  check. `ScheduleMapper` (package-private, infra layer) converts between
  `SchedulesRecord` (jOOQ-generated) and the `Schedule` domain entity via the
  `Schedule.builder()`, mapping `OffsetDateTime` ↔ `Instant` via UTC offset
  and `ScheduleType.valueOf(record.getType())` for the enum.
- `ScheduleHandler` (`dev.kairos.api.schedule`) translates HTTP context to
  the six schedule use-case calls. `ScheduleDtoMapper` (package-private,
  API layer) performs the `Schedule` → `ScheduleResponse` mapping. Routes are
  registered via `Router.registerScheduleRoutes`.
- `ApplicationContext` (`dev.kairos`) wires every layer in order:
  infrastructure → repositories → use cases → handlers → HTTP. `TaskHandler`,
  `DestinationHandler`, and `ScheduleHandler` are constructed here and their
  routes are registered via `Router.registerTaskRoutes`,
  `Router.registerDestinationRoutes`, and `Router.registerScheduleRoutes`. The
  Javalin server port is read from `config.getIntProperty("server.port", 8080)`.
  `KairosApplication.main` loads `AppConfig`, calls
  `ApplicationContext.build(config).start()`, and exits normally — the
  Javalin thread keeps the process alive.

## 10. Future Work (Explicitly Out of Current Scope)

- `execution_history.result` — the service's response to a delivered
  message. Will need a `correlation_id` attached by Kairos at delivery
  time, plus a way to match an async reply back to a specific execution.
- REST endpoints for `retry_policies` — M4.
- Cron expression parsing/validation — deferred to a dedicated cron builder
  (follow-up issue); currently the `CRON` schedule type stores the expression
  as-is after a non-blank check.
- Planner / materialization into `executions` — M5.
- Multi-tenancy, Admin UI, metrics — unchanged from the original Roadmap
  (V4–V5 in the README).

## 11. Related Documents

- `database.md` — exact table fields
- `plan.md` — the development plan by slice, with M1 (Task
  CRUD) as the starting point