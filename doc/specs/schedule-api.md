# Schedule CRUD API

## Problem

Kairos can store destinations and tasks, but there is no way for a client to
define **when** a task should fire. The `schedules` table and its invariants
already exist (migration `V4`), and the domain design (`doc/specification.md`
§3, §6; `.claude/CLAUDE.md` aggregate rules) is settled — but there is no
`Schedule` domain type, use cases, or REST surface. This adds the REST
**endpoints** so a client can create and manage the three schedule types
(`ONCE`, `CRON`, `FIXED`) for an existing task.

For: client services registering timing rules against their tasks.

## Scope

**In:**
- `Schedule` domain aggregate with type-specific factory methods
  (`Schedule.once(...)`, `Schedule.cron(...)`, `Schedule.fixed(...)`) that make
  invalid type/field combinations unrepresentable, mirroring the DB
  `schedules_type_fields_check`.
- Application use cases: create, get-by-id, list-by-task, update, delete,
  pause, resume.
- Repository port + JOOQ adapter for `schedules`.
- REST endpoints (Javalin handler + routes), common DTOs (request/response),
  error mapping via the existing `GlobalExceptionHandler`.

**Out:**
- Planner / materialization into `executions` (engine work, M4+).
- Cron-expression **parsing/validation** — deferred to a dedicated, reusable
  cron builder in a later issue. This API stores the cron string as-is after a
  basic non-blank check.
- Admin UI for schedules (separate concern, `module:admin`).
- `retry_policies` API.

## Design

**Module / layer:** `kairos-api`, following the existing Task/Destination CRUD
pattern exactly: `Router` → `ScheduleHandler` → use cases → `ScheduleRepository`
port → `JooqScheduleRepository`. DTOs live in `common/dto/schedule`. Package
root `dev.kairos`.

**Aggregate:** `Schedule` is its **own** aggregate, referencing `Task` only by
`taskId` (per `.claude/CLAUDE.md` and §9 of the spec). It is **not** loaded
through `Task`. Invariants are enforced inside the entity via factory methods —
no invalid combination should be constructible.

### Schedule type invariants (enforced in the domain, mirrored by DB CHECK)

- **`ONCE`** — requires `run_at`; `cron_expression` and `interval_seconds` null.
  `run_at` **must be in the future** at creation time (reject past timestamps
  with 400).
- **`CRON`** — requires a **non-blank** `cron_expression` and a valid
  `timezone` (parseable as a `java.time.ZoneId`); `run_at`/`interval_seconds`
  null. Cron **syntax is not parsed** here (deferred to the future cron
  builder).
- **`FIXED`** — requires `interval_seconds` with `0 < interval_seconds <= 86400`
  (max one day); `run_at`/`cron_expression` null. Rationale: an interval longer
  than a day is a calendar concern and belongs to `CRON`, not a plain interval.
  **Note:** the existing `V4` CHECK only enforces `interval_seconds > 0`; the
  new upper bound (`<= 86400`) must be added — either as a new domain-only
  invariant plus a follow-up migration tightening the CHECK, or as part of this
  work. Flagged for the implementer to decide and keep DB + domain consistent.

### Endpoints (nested under task for create/list; flat by id otherwise)

| Method | Path | Purpose | Success |
|---|---|---|---|
| POST | `/api/v1/tasks/{taskId}/schedules` | create a schedule for a task | 201 + ScheduleResponse |
| GET | `/api/v1/tasks/{taskId}/schedules` | list a task's schedules | 200 + PageResponse |
| GET | `/api/v1/schedules/{id}` | fetch one schedule | 200 + ScheduleResponse |
| PUT | `/api/v1/schedules/{id}` | update the "when" fields + `label`/`timezone` | 200 + ScheduleResponse |
| DELETE | `/api/v1/schedules/{id}` | delete a schedule | 204 |
| PATCH | `/api/v1/schedules/{id}/pause` | set `active = false` | 200 + ScheduleResponse |
| PATCH | `/api/v1/schedules/{id}/resume` | set `active = true` | 200 + ScheduleResponse |

**Update scope:** `type` is **immutable**. PUT may change only the "when" field
belonging to the current type (`run_at` for `ONCE`, `cron_expression` for
`CRON`, `interval_seconds` for `FIXED`), plus `label` and `timezone`. Changing
type = delete + recreate. Pause/resume mirror the Task `start`/`stop` pattern
(spec §9 explicitly anticipates `PATCH .../schedules/{id}/pause`).

### Error mapping (via `GlobalExceptionHandler`)

- **400** — validation: unknown `type`; missing/forbidden "when" field for the
  type; `ONCE run_at` in the past; `FIXED interval_seconds` ≤ 0 or > 86400;
  blank `cron_expression`; invalid `timezone`.
- **404** — task not found (on create/list) or schedule not found (get / update
  / delete / pause / resume).

## Acceptance criteria

- [ ] `Schedule` aggregate with `once`/`cron`/`fixed` factory methods; illegal
      type/field combinations cannot be constructed; unit tests cover each
      invariant (including `ONCE` past-date, `FIXED` bounds `0 < n <= 86400`).
- [ ] Create/get/list/update/delete/pause/resume use cases implemented, each
      doing one thing (SRP).
- [ ] `ScheduleRepository` port + JOOQ adapter; Testcontainers integration test
      against real Postgres.
- [ ] All seven endpoints live and wired in `Router`, returning the documented
      status codes; `common/dto/schedule` request/response DTOs added.
- [ ] API tests for every endpoint including edge cases (400 for each invalid
      "when" combination, 404 for missing task/schedule, past `ONCE`, `FIXED`
      over one day).
- [ ] DB CHECK and domain invariant for `FIXED <= 86400` are consistent
      (migration added if the CHECK needs tightening); migrations apply cleanly
      on a fresh DB.
- [ ] Happy path works end-to-end (create → get → list → pause → resume →
      update → delete).

## Notes

- Cron parsing/validation is intentionally deferred to a future **cron builder**
  issue; that builder is expected to be reused here later. This spec only stores
  the cron string.
- Planner/materialization (`executions`) is out of scope — this is the "when"
  definition surface only. See `doc/specification.md` §3–§6 and §8, and
  `doc/database.md` (`schedules` table) for the settled schema and rationale.
- Follow the existing Task/Destination CRUD as the implementation template.

## Issue metadata (suggested)

- **Type:** feature
- **Module(s):** module:api
- **Priority:** priority:high
- **Milestone:** Scheduling Core
