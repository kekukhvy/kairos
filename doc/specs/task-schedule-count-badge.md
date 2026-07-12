# Task schedule-count badge

## Problem

An operator looking at the Tasks grid cannot tell how a task is scheduled
without leaving the screen. Two states are invisible today and both matter:

- **A task with several schedules.** Nothing on the row hints that the task
  fires on more than one schedule, so an operator editing or stopping it has no
  idea how many triggers they are affecting.
- **A task with *no* schedules at all.** This is a silent misconfiguration: the
  task exists, looks healthy, is `active` — and will never run. Today it is
  visually identical to a correctly scheduled task.

Both are answerable only by navigating to Schedules and filtering by hand.

Surface both directly on the Tasks grid: a small badge carrying the schedule
count when a task has **more than one** schedule, and a red task name when it
has **none**.

## Scope

**In:**
- `scheduleCount` added to the task listing read model, computed by the API
  (`kairos-api`, `common`).
- `TaskGrid`: a small badge next to the task name when `scheduleCount > 1`;
  clicking it opens Schedules filtered to that task.
- `TaskGrid`: the task name rendered in the error colour, with an explanatory
  tooltip, when `scheduleCount == 0`.
- `ScheduleView`: a `?task=<taskId>` query parameter that pre-selects the task
  filter, so the badge can deep-link into it.

**Out:**
- Any change to the `Schedule` aggregate, the schedules table, or the schedule
  endpoints. The count is a read-side projection only.
- Showing a badge for `scheduleCount == 1` (the common case — no badge, no
  colouring; the row stays quiet).
- A sortable "Schedules" column, or any listing of the schedules themselves on
  the Tasks grid. The badge is a signal and a link, not a summary.
- Reacting to schedule changes made elsewhere without a grid refresh — the
  count is as fresh as the last task list load, which is the existing behaviour
  of every other column.

## Design

### Where the count comes from

`scheduleCount` is added to `TaskResponse` (`common`) and populated by the API
when listing and fetching tasks. This was chosen over two alternatives:

- **Fan-out in the admin UI** — reuse `ScheduleService.listForTasks(...)`, group
  by `taskId`, count client-side. Rejected: the schedule list API is task-scoped
  (`GET /tasks/{taskId}/schedules`), so this is one HTTP call *per task* on every
  grid refresh — and `TaskView.refresh()` runs after every create, update,
  delete, and Active toggle. `ScheduleService.listForTasks` and
  `DashboardService` already pay this N+1 cost; a third caller would entrench the
  problem rather than solve it.
- **A dedicated counts endpoint** (`GET /tasks/schedule-counts`). Rejected as
  over-engineering: a new endpoint for a single badge, when the task listing is
  already the natural carrier of the number.

**Aggregate boundary.** `.claude/CLAUDE.md` states that `Schedule` is its own
aggregate and that schedules must never be loaded through `Task`. That rule holds
here: `Task` gains **no** `schedules` field and no `scheduleCount` field. The
count is assembled in the **API/read layer** — a CQRS read-side projection, which
is exactly the role `doc/specification.md` gives to read models. Concretely:

- `ScheduleRepository` (`domain/schedule`) gains a counting method —
  `countByTaskIds(Collection<TaskId>) → Map<TaskId, Long>` — implemented in
  `JooqScheduleRepository` as a single grouped `COUNT(*)`. One query for the
  whole page, not one per task.
- A list-tasks read path (use case / handler) calls it once for the page's task
  ids and hands the map to `TaskDtoMapper.toResponse(...)`, which today builds
  `TaskResponse` from a bare `Task`. The mapper takes the count as an explicit
  argument; the domain `Task` is untouched.
- A task with no schedules is absent from the grouped result and maps to `0`.

`kairos-admin`'s `TaskDto` mirrors the new field (it is a deliberate local copy
of `TaskResponse`, see its Javadoc).

### What the grid renders

The task-name cell becomes a component column:

| `scheduleCount` | Rendering |
|---|---|
| `0` | Name in `Tokens.COLOR_ERROR`, tooltip explaining the task has no schedules and will never run. No badge. |
| `1` | Plain name. Nothing else. |
| `> 1` | Plain name + a small badge showing the number, clickable. |

The badge reuses the existing `Tokens.THEME_BADGE_*` theme and the error colour
reuses `Tokens.COLOR_ERROR` — no new CSS literals, no new tokens unless a
badge-size token is genuinely missing.

The red name is paired with a tooltip rather than standing alone: colour as the
sole carrier of meaning is invisible to colour-blind users and to screen readers,
and "why is this one red?" is otherwise a guess.

### Click behaviour

Clicking the badge opens Schedules **filtered to that task**. `ScheduleView`
already has a task filter (`taskFilter`, populated from `TaskDto::label`) but no
way to preselect it from a link, so it gains a `?task=<taskId>` query parameter
and a `BeforeEnterObserver` that applies it — the same pattern `TaskView` already
uses for `?status=` when the dashboard deep-links into it
(`TaskView.beforeEnter`). An unknown or malformed task id is ignored and the
unfiltered list is shown, mirroring how `TaskView` silently ignores an unknown
`status` value.

`TaskGrid` opens `TaskDetails` on **any** row click (per issue #26), so the badge
click must stop propagation — otherwise clicking it would both navigate and open
the details dialog.

## Acceptance criteria

- [ ] `TaskResponse` (`common`) carries a `scheduleCount` field, populated by the API.
- [ ] `ScheduleRepository` exposes a count-by-task-ids method, implemented in `JooqScheduleRepository` as a **single** grouped `COUNT(*)` query — not one query per task.
- [ ] Listing tasks issues **one** schedule-count query for the whole page, regardless of how many tasks it contains.
- [ ] The domain `Task` entity is unchanged — no `schedules` collection, no count field; the count lives only in the read model.
- [ ] A task with **more than one** schedule shows a small badge with the count next to its name in `TaskGrid`.
- [ ] A task with **exactly one** schedule shows no badge and no colouring.
- [ ] A task with **zero** schedules shows its name in the error colour with a tooltip explaining it has no schedules and will never run.
- [ ] Clicking the badge navigates to Schedules with the task filter pre-selected to that task; the row's `TaskDetails` dialog does **not** open.
- [ ] `ScheduleView` accepts `?task=<taskId>` and pre-selects the task filter; an unknown id falls back to the unfiltered list.
- [ ] All new UI strings live in `TaskText` / `ScheduleText`; styling goes through `Tokens` (no literals); no Lombok; methods ≤ 40 lines.
- [ ] Unit tests cover the mapper (count 0 / 1 / N), the badge-visibility rule, and the zero-schedule name styling; a repository integration test (Testcontainers) covers the grouped count including a task with zero schedules.
- [ ] `./gradlew build` passes.

## Notes

- Aggregate rules: `.claude/CLAUDE.md` → "Aggregate boundaries"; read-model /
  CQRS rationale: `doc/specification.md`.
- Related: issue #26 (single-click row opens `TaskDetails` — the reason the badge
  must stop click propagation) and #33 (same interaction model for destinations).
- Deliberately deferred: the pre-existing N+1 fan-out in
  `ScheduleService.listForTasks` (used by `ScheduleView` and `DashboardService`).
  This spec does not fix it — but by putting the count in the task read model it
  avoids adding a third caller, and leaves the door open to a bulk schedule
  endpoint later.

## Issue metadata (suggested)

- **Type:** feature
- **Module(s):** module:admin, module:api, module:common
- **Priority:** priority:medium
- **Milestone:** Admin UI
