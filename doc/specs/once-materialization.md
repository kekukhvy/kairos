# Materialize ONCE schedules into `executions`

## Problem

A `ONCE` schedule stored in `schedules` never fires. Nothing turns the rule
("run at `2026-08-01T10:00:00Z`") into a row in `executions`, which is the only
table the engine's claim loop reads. The rule is recorded and then ignored.

`ONCE` is the cheapest type to materialize: exactly one occurrence, no cron
parsing, no horizon arithmetic, no recycling. It is therefore the smallest
useful slice of the Planner (M5) and can ship before cron support exists.

**Who materializes matters.** Per the composability rules in
`.claude/CLAUDE.md`, scenario **B** is "engine-only": the user runs
`kairos-engine` plus the database and writes to the documented tables directly,
without deploying `kairos-api`. If materialization lived in the API's
`CreateScheduleUseCase`, those users' `ONCE` schedules would never fire — the
engine would be incomplete on its own. Materialization belongs to
`kairos-engine`.

## Scope

**In:**
- A materialization pass in `kairos-engine` that finds active, not-yet-
  materialized `ONCE` schedules and inserts one row into `executions` for each.
- A `materialized_at` column on `schedules` marking a schedule as done, so the
  pass is idempotent across ticks and restarts.
- Insert + flag written in a single transaction.
- Fixing `DELETE /api/v1/schedules/{id}`, which this change would otherwise
  break (see Design → Delete).
- Whatever engine bootstrap is needed to run the pass (entry point, datasource
  wiring, loop).

**Out:**
- `CRON` materialization — needs a cron parser and a frequency-based horizon;
  separate slice.
- `FIXED` — uses recycling, not materialization (`doc/specification.md:91-97`).
  Explicitly not touched here.
- The claim loop, delivery, workers, retries. This spec only *creates* the row;
  nothing consumes it yet.
- Planner self-healing / lease table (`planner_runs`) — still an open question
  in `doc/database.md:270-273`.
- Materialization horizon thresholds — `ONCE` has no horizon.
- Re-materializing on schedule **update**, **pause**, or **resume** — separate
  slices (see Notes).
- Maintaining the denormalized `tasks.next_run_at` cache — separate concern,
  write semantics still open (`doc/database.md:278`).

**Prerequisite (separate issue):** the shared database plumbing
(`DataSourceFactory`, `DSLContextFactory`, `DatabaseMigrator`, JOOQ codegen)
currently lives inside `kairos-api`. `kairos-engine/build.gradle` is a stub
(`description = 'TODO'`) with no dependencies at all. That plumbing must be
extracted into a module both components can use before this slice can be built.
Duplicating it into the engine was rejected (DRY: two copies of the DB config
and two JOOQ codegen setups).

## Design

### Module and layer

`kairos-engine`. The engine owns `executions` — consistent with
`doc/database.md:184`, where `idx_schedules_active` exists explicitly "for the
planner walking live schedules".

`kairos-api` is **not** modified except for the delete fix below.
`CreateScheduleUseCase` keeps its single write to `schedules`.

### Selecting work

Materialize a schedule when all of these hold:

- `type = 'ONCE'`
- `active = true`
- `materialized_at IS NULL`

The owning task's `active` flag is **not** consulted. `tasks.active` is a
runtime kill-switch enforced by the claim loop; checking it here would duplicate
the decision in two places and produce the bug "task re-enabled, but its tick was
never created". The row is materialized regardless; the claim loop decides
whether to run it.

`runAt` in the past is not a concern — the domain already requires `ONCE.runAt`
to be strictly in the future at creation/update time
(`doc/specification.md:71-72`).

### The row written

| Column | Value |
|---|---|
| `task_id` | the schedule's `task_id` |
| `schedule_id` | the schedule's `id` |
| `scheduled_for` | the schedule's `run_at` |
| `next_attempt_at` | the schedule's `run_at` |
| `status` | `PENDING` (default) |
| `attempt` | `0` (default) |
| `recycle` | `false` — recycling is `FIXED`-only |

### Idempotency: `materialized_at` on `schedules`

A new migration adds `materialized_at TIMESTAMPTZ` to `schedules` (null =
not yet materialized). The selection query filters on it.

This means the pass performs **two writes**: the `INSERT` into `executions` and
the `UPDATE` of `schedules.materialized_at`. They must be committed **together**
— a crash in between leaves a row inserted with the flag unset, so the next pass
would insert a second row and the task would fire twice.

Both statements therefore run inside one `dsl.transaction(...)`. This is a local
transaction wholly inside the engine's repository code; it does not require a
cross-layer transaction port, because only one aggregate's storage is involved
per unit of work.

### Delete: an existing endpoint this change would break

`executions.schedule_id` references `schedules(id)` **without**
`ON DELETE CASCADE` — deliberately, unlike `retry_policies`/`schedules`
(`doc/database.md:208`).

`DELETE /api/v1/schedules/{id}` works today only because `executions` is always
empty. Once the engine starts inserting rows, deleting a schedule that has a
materialized execution will fail with a foreign-key violation. Fixing this is
part of this slice, not a follow-up: deleting a schedule must first remove its
rows from `executions`.

The no-cascade decision itself stays — execution rows are engine state and
should be removed explicitly, not silently by the database.

### Failure handling

A schedule that fails to materialize (e.g. its task row is gone) must not stop
the pass. Log at `WARN` with the schedule id and continue with the rest; the
transaction boundary is per schedule, so one failure rolls back only its own
insert and flag.

## Acceptance criteria

- [ ] A migration adds `materialized_at` to `schedules`; it applies cleanly on a
      fresh database, and `./gradlew :kairos-persistence:generateJooq` regenerates
      without error.
- [ ] An active `ONCE` schedule with `materialized_at IS NULL` results in exactly
      one `executions` row with `scheduled_for` = `next_attempt_at` = the
      schedule's `run_at`, `status = 'PENDING'`, `attempt = 0`,
      `recycle = false`.
- [ ] After materialization the schedule's `materialized_at` is set.
- [ ] Running the pass twice over the same schedule produces exactly one
      `executions` row (idempotent).
- [ ] A schedule with `active = false` is not materialized.
- [ ] `CRON` and `FIXED` schedules are not materialized by this pass.
- [ ] A `ONCE` schedule whose owning task has `active = false` **is** still
      materialized.
- [ ] If the transaction fails after the insert, neither the `executions` row nor
      `materialized_at` persists (verified by an integration test forcing a
      failure).
- [ ] One schedule failing to materialize does not prevent the others in the same
      pass from being materialized.
- [ ] `DELETE /api/v1/schedules/{id}` succeeds for a schedule that has a
      materialized `executions` row, and removes that row.
- [ ] Repository behavior is covered by in-process H2 integration tests
      (`H2DatabaseBase`); selection logic is covered by unit tests without a DB.
- [ ] End-to-end: create a task, create a `ONCE` schedule, run the engine pass,
      observe the `executions` row.

## Notes

- `doc/specification.md:83-107` — Materialization (Planner), including the
  catch-up rule this implements for `ONCE`.
- `doc/database.md:219-223` — per-type materialization semantics;
  `doc/database.md:208` — the deliberate absence of `ON DELETE CASCADE`.
- `doc/plan.md:267-277` — M5 Planner. This slice covers the `ONCE` portion of
  the first checkbox only; horizon-by-frequency, catch-up for `CRON`, and
  self-healing remain open.
- `.claude/CLAUDE.md` → "Delivery & packaging model" — scenario B is the reason
  this lives in the engine rather than the API.
- Deliberately deferred: re-materialization when a `ONCE` schedule is updated
  (`runAt` moved), paused, or resumed. With `materialized_at` set, an update
  would currently leave a stale `executions` row at the old time. Each needs its
  own decision about clearing the flag and the row, and each is its own slice.
- Deliberately deferred: the engine's own scheduling cadence (how often the pass
  runs, configurability) is an implementation detail here; a fixed interval is
  sufficient.

## Issue metadata (suggested)

- **Type:** feature
- **Module(s):** module:engine, module:api
- **Priority:** priority:high
- **Milestone:** M5 — Planner (Materializer)
