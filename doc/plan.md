# Kairos — Development Plan

> A development plan organized by functional slices. Starting simple: CRUD
> + soft delete for Task. Each item is a candidate for its own Issue →
    > `feature/xxx` branch → PR → `develop`, per the agreed Git Workflow.

## Architecture Approach: DDD + Hexagonal

This project is also a deliberate learning exercise in DDD (tactical
patterns) and Hexagonal Architecture (Ports & Adapters). A few ground
rules to keep that practice honest rather than cargo-culted:

- We're applying **tactical DDD** (aggregates, value objects, invariants
  enforced in entities), not the full **strategic** toolkit (bounded
  context workshops, ubiquitous-language sessions with domain experts).
  Kairos is a single-team infrastructure service with little business
  logic — strategic DDD would be ceremony without payoff here.
- **Aggregate boundaries:**
    - `Destination` — its own aggregate.
    - `Task` — aggregate root; `RetryPolicy` lives **inside** it as a
      value-object collection (a retry step has no identity or meaning
      outside its task).
    - `Schedule` — its **own** aggregate, referencing `Task` only by id.
      Pausing one schedule shouldn't require loading/locking the whole task
      and its siblings.
    - `Execution` — its own aggregate, referencing `taskId`/`scheduleId` by
      id only. Different lifecycle and concurrency profile than
      Task/Schedule (claim/lock/retry, high write frequency).
    - `ExecutionHistory` — not a rich aggregate; it's an append-only
      read-model/log (closer to a CQRS read side than a DDD aggregate).
- **Don't force richness where the domain is genuinely simple.**
  `Destination` and most of `Task`'s fields are plain CRUD — that's fine.
  Invest modeling effort where real invariants exist (see callouts below).
- **Hexagonal payoff checkpoint:** the architecture only proves itself
  once a *second* delivery adapter exists and required zero changes to
  the domain/application layers. Treat that as a concrete milestone to
  validate, not just an assumption.

---

## M1 — Task API (start here) ✅

**Goal:** a vertical slice — create, update, and (soft-)delete a task
through the REST API. No schedules, executions, or delivery yet — just a
task registry.

> **Architecture note:** this milestone is mostly about getting the
> domain/application/infrastructure layering right and defining the first
> port (`TaskRepository`). `Task` itself can stay close to a plain entity
> here — the one invariant worth enforcing explicitly is soft delete
> (`task.update(...)` should reject changes once `deletedAt` is set), not a
> generic field-by-field validator.

### Domain (pure Java, no framework dependencies)
- [x] `Task` entity (Builder pattern, per convention) — id, service, name,
  description, active, destinationId, eventName, payload, timeoutMs,
  supportsRetry, createdAt, updatedAt, deletedAt
- [x] Value objects: `TaskId`, `DestinationId`
- [x] Domain validation: `name` required, `timeoutMs > 0`, etc. (via shared
  `Validation.requireText` / `requirePositive` in `common`; invariants enforced
  in the `Task` constructor and `update()`)
- [x] `TaskRepository` port (interface): `save`, `findById`, `findAll`,
  `softDelete` — `findById` returns the task regardless of soft-delete
  state; callers inspect `isDeleted()` and decide how to react.
  `findAll(limit, offset)` returns live (non-deleted) rows only, newest
  first.
- [x] `DestinationRepository` port (interface, read-only in M1):
  `existsById(DestinationId)` — validates the destination FK before insert
  or update; the full Destinations API is deferred to M2.

### Application
- [x] `CreateTaskUseCase` — validates destination exists
  (`DestinationRepository.existsById`), else `ValidationException`; assigns
  `TaskId.newId()`; applies nullable `active`/`supportsRetry` defaults (true
  / false); saves; returns `Task`.
- [x] `UpdateTaskUseCase` — PUT (full replacement of editable fields);
  `TaskNotFoundException` if missing or soft-deleted; validates destination
  exists; builds `TaskEdit`; calls `task.update(edit, clock.instant())`;
  saves; returns updated `Task`. `active`/`supportsRetry` null → defaults
  (true / false).
- [x] `SoftDeleteTaskUseCase` — `TaskNotFoundException` if missing or
  already-deleted; else calls `task.softDelete(clock.instant())`.
- [x] `GetTaskUseCase` — `findById`, throws `TaskNotFoundException` if
  missing or soft-deleted.
- [x] `ListTasksUseCase` (paginated, excludes soft-deleted) — delegates to
  `TaskRepository.findAll`; soft-deleted rows are excluded at the SQL level
  (`WHERE deleted_at IS NULL` inside `JooqTaskRepository.findAll`); no
  in-memory filtering is performed in the use case.

All use cases receive their port(s) and a `java.time.Clock` via constructor
(no field injection, no framework); timestamps are always sourced from the
injected clock for determinism and testability.

**Commands** (`dev.kairos.application.task.commands`):
- `CreateTaskCommand` — carries raw field values (no domain types) including
  `service`; `active` and `supportsRetry` are nullable `Boolean`.
- `UpdateTaskCommand` — same editable fields, `service` intentionally absent
  (immutable); `active` and `supportsRetry` are nullable `Boolean`.

### Infrastructure
- [x] Flyway: `V1__create_destinations_table.sql`, `V2__create_tasks_table.sql`
  (plus `V3`–`V6` — all six tables from `doc/database.md` are now created,
  not just the two named here; verified to apply cleanly on a fresh
  Postgres 16)
- [x] JOOQ codegen wiring — `nu.studer.jooq` plugin pinned in
  `pluginManagement` (version from `gradle.properties`); generated sources
  at `kairos-api/src/main/generated` (package
  `dev.kairos.infrastructure.generated`) — moved to `kairos-persistence` in
  M3.5, package unchanged. JSONB columns (`tasks.payload`,
  `destinations.config`, `execution_history.result`) are typed as
  `org.jooq.JSONB`; conversion to/from `String` is handled in `TaskMapper`.
  DB connection resolves: env var → `local.properties`
  (gitignored) → hardcoded local default.
- [x] `JooqTaskRepository implements TaskRepository` — upsert via
  `INSERT ... ON CONFLICT (id) DO UPDATE`; never writes the engine-owned
  denormalized columns (`last_status`, `last_run_at`, `next_run_at`);
  transaction management delegated to the caller.
- [x] `TaskMapper` (package-private, infra layer) — bidirectional mapping
  between `TasksRecord` (jOOQ) and `Task` domain entity; JSONB↔String via
  `JSONB.data()` / `JSONB.valueOf()`; `OffsetDateTime`↔`Instant` via UTC offset.
- [x] `JooqDestinationRepository implements DestinationRepository` —
  `existsById` via `DSLContext.fetchExists` (M1 scope); full CRUD implemented
  in M2 (see below).
- [x] `DSLContextFactory` — builds a shared `DSLContext` from a `DataSource`
  (`renderSchema = false`, `renderQuotedNames = NEVER`); injected into
  repositories at startup.

### API (kairos-api)
- [x] `POST /api/v1/tasks` — create (201 + `TaskResponse`)
- [x] `GET /api/v1/tasks/{id}` — fetch (404 if missing or deleted)
- [x] `GET /api/v1/tasks` — list (excluding deleted, paginated; query params `limit`/`offset`)
- [x] `PUT /api/v1/tasks/{id}` — update (full replacement of editable fields, 200 + `TaskResponse`)
- [x] `DELETE /api/v1/tasks/{id}` — soft delete (`deleted_at = now()`, 204 no body)
- [x] DTO contracts in `common`: `CreateTaskRequest`, `UpdateTaskRequest`, `TaskResponse`,
  `PageResponse<T>`, `ErrorResponse`
- [x] Error handling: 400 (`ValidationException`, `IllegalArgumentException`),
  404 (`TaskNotFoundException`), 409 (`TaskAlreadyDeletedException`), 500 (anything else)
- [x] HTTP framework: Javalin 6.4.0; `ObjectMapperFactory` (JavaTimeModule, ISO-8601
  timestamps, `FAIL_ON_UNKNOWN_PROPERTIES=false`); `Router` + `GlobalExceptionHandler`;
  `ApplicationContext` wiring; server port from `server.port` (default 8080)

### Tests
- [ ] Unit tests for domain/use cases (no DB)
- [ ] Integration tests for the repository (in-process H2, `H2DatabaseBase`)
- [ ] API tests for all 5 endpoints, including edge cases (404 after
  delete, deleted tasks excluded from the list)

### Definition of Done
- `docker compose up -d` + `./gradlew build` — migrations apply cleanly on a fresh DB
- Full happy path: create → fetch → update → delete → verify 404 and absence from the list
- `destination_id` must reference an existing row in `destinations` at
  creation time (minimal FK check; a full Destinations API comes in M2)

---

## M2 — Destinations (minimal registry)

**Goal:** `task.destination_id` needs something real to point to.

- [x] `destinations` table (already created as a prerequisite in M1 — `V1`)
- [x] `Destination` entity — `DestinationId` (human-readable `String`),
  `DestinationType` enum (`KAFKA`, `SQS`, `WEBHOOK`, `RABBITMQ`), `config`
  (JSONB string, mutable via `updateConfig`), `createdAt` (immutable).
  Builder construction; equality by id only; `type` and `createdAt` immutable
  after creation.
- [x] `DestinationType` enum (was a stub class in M1)
- [x] Destination exceptions (all in `dev.kairos.domain.destination.exceptions`):
  `DestinationAlreadyExistsException`, `DestinationNotFoundException`,
  `InvalidDestinationTypeException`, `DestinationInUseException`
- [x] `DestinationRepository` port extended to full CRUD: `save` (upsert),
  `findById`, `findAll(limit, offset)`, `deleteById`
- [x] `TaskRepository` extended with `existsByDestinationId(DestinationId)` —
  used to block deletion of destinations still referenced by tasks
- [x] Five destination use cases: `CreateDestinationUseCase`,
  `GetDestinationByIdUseCase`, `ListDestinationsUseCase`,
  `UpdateDestinationUseCase`, `DeleteDestinationUseCase`
- [x] `CreateDestinationCommand` record (`destinationId`, `destinationType`,
  `config` — all raw `String`)
- [x] `JooqDestinationRepository` — full CRUD implementation; `toDomain`
  mapper; upsert `created_at` set on insert only
- [x] Delete blocked while any task references the destination
  (`DestinationInUseException`); delete is otherwise idempotent (no prior
  existence check)
- [x] `Validation.requireText(value, field)` two-argument overload added to
  `common` (used by `Destination.validateConfig`)
- [x] `POST /api/v1/destinations` (201 + `DestinationResponse`),
  `GET /api/v1/destinations` (200 + `PageResponse<DestinationResponse>`, `hasNext` via over-fetch),
  `GET /api/v1/destinations/{id}` (200 + `DestinationResponse`),
  `PUT /api/v1/destinations/{id}` (200 + `DestinationResponse`; returns updated destination),
  `DELETE /api/v1/destinations/{id}` (204 no body) — HTTP wiring done
- [x] `DestinationHandler`, `DestinationDtoMapper` wired in `ApplicationContext` and `Router`
- [x] `GlobalExceptionHandler` extended: 404 `DestinationNotFoundException`;
  409 `DestinationAlreadyExistsException`, `DestinationInUseException`;
  400 `InvalidDestinationTypeException`
- [x] `UpdateDestinationUseCase.execute` returns `Destination` (was `void`) so
  the handler can serialise the updated entity
- [x] `PageResponse<T>` extended with `boolean hasNext`; over-fetch pattern
  (`limit + 1`) applied in both `TaskHandler` and `DestinationHandler`
- [x] Destination DTO contracts in `common`: `CreateDestinationRequest` (destinationId, destinationType, config JsonNode),
  `UpdateDestinationRequest` (config JsonNode), `DestinationResponse` (destinationId, destinationType, config, createdAt)
- [x] Validate basic `config` shape per type via `DestinationConfigSchema` (in
  `common`): required/optional keys per type, key presence enforced, values
  unchecked, extra keys allowed; templates for UI prefill

## M3 — Schedules ✅

**Goal:** attach "when" to a task, supporting multiple rules per task.

> **Architecture note:** `Schedule` is its own aggregate (see notes above)
> with its own repository — don't load it through `Task`. Use
> type-specific factory methods (`Schedule.once(...)`, `Schedule.cron(...)`,
> `Schedule.fixed(...)`) instead of one generic constructor, so an invalid
> combination (e.g. `ONCE` without `run_at`) can't be constructed at all.

- [x] `schedules` table (`V4` — includes the type-fields CHECK so an
  invalid combination, e.g. `ONCE` without `run_at`, is rejected at the DB level)
- [x] `V7__tighten_schedules_fixed_interval.sql` — tightens
  `schedules_type_fields_check` to add `interval_seconds <= 86400` for
  `FIXED`, keeping the DB CHECK consistent with `Schedule.MAX_INTERVAL_SECONDS`
- [x] `Schedule` domain aggregate — `ScheduleId`, `ScheduleType` enum
  (`ONCE`/`CRON`/`FIXED`), `ScheduleEdit`, `ScheduleNotFoundException`,
  `ScheduleRepository` port; factory methods `once/cron/fixed` enforce
  type invariants; `update/pause/resume` behavior methods; `Builder` for
  persistence rehydration
- [x] Type-specific invariants enforced in domain (and DB CHECK):
  `ONCE` → `runAt` required and future; `CRON` → non-blank
  `cronExpression` + valid `ZoneId` timezone (cron syntax not parsed);
  `FIXED` → `0 < intervalSeconds <= 86400`
- [x] Six use cases: `CreateScheduleUseCase`, `GetScheduleByIdUseCase`,
  `ListSchedulesByTaskUseCase`, `UpdateScheduleUseCase`,
  `DeleteScheduleUseCase`, `SetScheduleActiveUseCase` (pause/resume)
- [x] `CreateScheduleCommand` and `UpdateScheduleCommand` records in
  `dev.kairos.application.schedule.commands`
- [x] `JooqScheduleRepository implements ScheduleRepository` — upsert,
  findById, findByTaskId (newest first), idempotent deleteById
- [x] `ScheduleMapper` (package-private, infra layer) — `SchedulesRecord` ↔
  `Schedule` domain entity
- [x] Seven REST endpoints wired: `POST /api/v1/tasks/{taskId}/schedules`,
  `GET /api/v1/tasks/{taskId}/schedules`, `GET /api/v1/schedules/{id}`,
  `PUT /api/v1/schedules/{id}`, `DELETE /api/v1/schedules/{id}`,
  `PATCH /api/v1/schedules/{id}/pause`, `PATCH /api/v1/schedules/{id}/resume`
- [x] `ScheduleHandler`, `ScheduleDtoMapper` wired in `ApplicationContext`
  and `Router.registerScheduleRoutes`
- [x] `GlobalExceptionHandler` extended: 404 `ScheduleNotFoundException`
- [x] Schedule DTO contracts in `common/dto/schedule`: `CreateScheduleRequest`,
  `UpdateScheduleRequest`, `ScheduleResponse`
- [ ] Cron expression parsing/validation (a Java library) — deferred to a
  future dedicated cron builder issue
- [ ] Unit tests for domain/use cases (no DB)
- [ ] Integration tests for the repository (in-process H2, `H2DatabaseBase`)
- [ ] API tests for all seven endpoints, including edge cases (400 for each
  invalid "when" combination, 404 for missing task/schedule, past `ONCE`,
  `FIXED` over one day)

## M3.5 — Persistence Module Extraction & Schema Ownership ✅

**Goal:** extract all DB plumbing into a shared `kairos-persistence` module;
transfer schema ownership from `kairos-api` to `kairos-engine`.

- [x] New `kairos-persistence` module — owns `AppConfig`, `DataSourceFactory`,
  `DatabaseMigrator`, `DSLContextFactory`, `SchemaReadinessCheck`
- [x] Migrate `V1`–`V8` from `kairos-api/src/main/resources/db/migration/` to
  `kairos-persistence/src/main/resources/db/migration/`
- [x] Move JOOQ codegen plugin config to `kairos-persistence`; generated
  sources remain in package `dev.kairos.infrastructure.generated`
- [x] Expose persistence types as `api` (transitive) so consumers get
  `DataSource`/`DSLContext` on their compile classpath; only `kairos-api` and
  `kairos-engine` depend on the module. `kairos-sdk`, `kairos-admin` and
  `common` simply don't — the module is deliberately separate from `common`
  so client JARs never drag the persistence stack
- [x] `kairos-engine` owns and runs Flyway on startup via `EngineBootstrap`
  → `DatabaseMigrator.migrate(...)`; exposes health signal at
  `engine.health.file` (path from config), readable only after migration
  completes
- [x] `kairos-api` no longer migrates; calls `SchemaReadinessCheck.verify`
  instead (fails fast with clear message if engine hasn't migrated yet)
- [x] Compose orchestration will use `depends_on: condition: service_healthy`
  on the engine (not yet containerized, so this is out of scope for this
  slice)

## M3.6 — Core Module Extraction ✅

**Goal:** extract `domain/` and `application/` out of `kairos-api` into a new
`kairos-core` library, so any runnable component can embed the scheduling
domain without pulling in HTTP/JOOQ.

- [x] New `kairos-core` module — pure Java library, zero framework
  dependencies, depends only on `common`
- [x] Moved `domain/` and `application/` (main + tests) from `kairos-api` to
  `kairos-core` with `git mv`; package names unchanged
  (`dev.kairos.domain`, `dev.kairos.application`)
- [x] `kairos-api` now holds only `api/` (REST handlers) and
  `infrastructure/` (JOOQ repositories); depends on `kairos-core`
- [x] `kairos-engine` depends on `kairos-core` (currently `testImplementation`,
  since only its tests reference the domain until the planner lands in M5/M6)
  and can reference `Schedule` / `ScheduleType`
- [x] `H2DatabaseBase` published as a `kairos-persistence` `testFixtures`
  source set (`java-test-fixtures` plugin) so repository-style integration
  tests in any module — not just `kairos-api` — can depend on
  `testFixtures(project(':kairos-persistence'))`

## M4 — Retry Policy

**Goal:** configurable retry steps per task.

> **Architecture note:** `RetryPolicy` is a value-object collection inside
> the `Task` aggregate, not its own aggregate — see the boundary rationale
> above.

- [x] `retry_policies` table (`V3` — `UNIQUE(task_id, attempt_number)`
  already enforced at the DB level)
- [ ] Step CRUD: `POST/GET/PUT/DELETE /api/v1/tasks/{taskId}/retry-policy`
- [ ] Validation: `attempt_number` is unique and contiguous within a task

## M5 — Planner (Materializer)

**Goal:** turn schedules into rows in `executions`, with a horizon by frequency.

- [ ] Component in `kairos-engine`: walks active schedules, generates
  executions for the upcoming horizon
- [ ] Horizon depends on type/frequency (infrequent — a day or more;
  high-frequency `FIXED` — a short horizon)
- [ ] Catch-up: materialize immediately on schedule create/update if the
  next occurrence already falls within the current window
- [ ] Self-healing: detect an unfinished/crashed previous planner run and retry it
- [ ] Recycle logic for `FIXED` (one row, advanced in place, never duplicated)

## M6 — Engine: claim & dispatch

**Goal:** the core `SELECT ... FOR UPDATE SKIP LOCKED` loop, workers claiming work.

> **Architecture note:** this is the primary place to practice rich domain
> modeling. Model the `Execution` lifecycle as explicit methods with guard
> clauses — `execution.claim(workerId)`, `execution.succeed()`,
> `execution.fail(retryPolicy)` — rather than ad hoc `UPDATE ... SET status`
> statements scattered through the application layer. Illegal transitions
> (e.g. completing an execution that was never claimed) should be
> impossible to express, not just checked at runtime.

- [ ] Claim query against `executions` (`status IN (PENDING, RETRYING) AND next_attempt_at <= now()`)
- [ ] Reaper for stuck `CLAIMED` rows (via `locked_at`)
- [ ] Hand off to `kairos-worker` for delivery

## M7 — Kafka Adapter (first adapter)

> **Architecture note:** this is the first real Hexagonal port/adapter
> pair. Don't consider the boundary validated yet — the real test comes
> once a *second* adapter (e.g. Webhook) ships and the domain/application
> layers needed zero changes. Consider writing a fake/in-memory
> `DeliveryAdapter` for tests early — a clean way to prove the port
> abstraction actually holds without spinning up Kafka for every test.

- [ ] `kairos-adapters/kafka` — implements the Delivery Adapter port
- [ ] Config via `destinations.config` (topic, etc.)
- [ ] Delivery timeout = `tasks.timeout_ms`

## M8 — Retry execution & Dead Letter

- [ ] On failure — read `retry_policies`, set `next_attempt_at` per the
  delay, status `RETRYING`
- [ ] If no further retry steps exist — `DEAD_LETTER`
- [ ] Record the result of every attempt in `execution_history`

## M9 — Execution History / Archive pipeline

> **Architecture note:** keep `execution_history` simple — it's a
> read-model/log (closer to the read side of CQRS), not a rich aggregate.
> No domain behavior belongs here; a thin repository for inserts and
> queries is enough.

- [ ] Atomically move completed `executions` (`ONCE`/`CRON`) into `execution_history`
- [ ] For `FIXED` — write the copy before recycling, in the same transaction
- [ ] `GET /api/v1/tasks/{taskId}/history` — paginated run history

## M10+ — rest of the original Roadmap

SQS/RabbitMQ/Webhook adapters, multi-tenancy, Admin UI (Vaadin), metrics —
unchanged from the original README (V3–V5).