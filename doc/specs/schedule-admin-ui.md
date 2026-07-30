# Schedule Admin UI

## Problem

The Schedule CRUD API (`/api/v1/tasks/{taskId}/schedules` + `/api/v1/schedules/{id}`,
issue #18) has no user-facing UI. Operators can currently create, list, edit,
pause/resume and delete schedules only via raw HTTP. This slice adds a
`kairos-admin` (Vaadin) screen for managing schedules, mirroring the existing
Destinations and Tasks tabs so the admin experience is consistent.

**For whom:** operators / admins using the Kairos admin console.

## Scope

**In:**
- A new top-level **Schedules** side-nav tab (`MainLayout`), alongside Tasks and
  Destinations.
- A `SchedulesView` with a **task picker** (ComboBox at the top). Because the
  list API is task-scoped (`GET /api/v1/tasks/{taskId}/schedules`), the grid is
  empty until a task is chosen; picking a task loads that task's schedules.
- A grid of schedules for the selected task: columns for type, label, the
  resolved "when" (cron / runAt / interval), timezone, active status, and a
  row-actions column (view, edit, pause/resume toggle, delete).
- A create/edit **schedule form** (Dialog) with a **Type select**
  (ONCE / CRON / FIXED) that **shows only the relevant "when" field**:
  - `ONCE` → date-time picker (`runAt`)
  - `CRON` → plain cron **text field** + timezone (no cron-builder yet)
  - `FIXED` → interval-seconds field
- Client-side filtering/search over the loaded schedules (reuse `FilterBar`),
  consistent with Tasks/Destinations.
- A read-only **details** dialog (view action), like `TaskDetails`.
- Wiring: `ScheduleService` (REST client), `ScheduleText` (all UI strings /
  no literals), `ScheduleRoutes`, DTOs, and a `scheduleEndpoint` on
  `ApiProperties`.

**Out:**
- **Cron builder / visual cron editor** — deferred; cron is a plain text field
  for now (explicit user decision).
- Changing a schedule's **type** on edit — `UpdateScheduleRequest` omits `type`
  ("changing type means delete + recreate"). The Type select is **read-only in
  edit mode**, matching the immutable-`service` convention on `TaskForm`.
- Any new API endpoints or domain changes — this is UI-only against the
  existing API.
- Server-side pagination in the UI beyond what Tasks/Destinations already do
  (list is loaded and filtered client-side).

## Design

**Module / layer:** `kairos-admin` only (the sole module where Spring/Vaadin is
allowed). No changes to `domain` / `application` / `infrastructure`. New feature
package `dev.kairos.admin.feature.schedule` mirroring
`feature.task` / `feature.destination`:

```
feature/schedule/
  ScheduleRoutes.java          # route + page-title constants
  ScheduleService.java         # @Service REST client
  ScheduleText.java            # all UI strings (no string literals in components)
  ScheduleView.java            # @Route("schedules"), task picker + grid + toolbar
  component/
    ScheduleGrid.java          # Grid<ScheduleDto>, row actions via callbacks
    ScheduleForm.java          # Dialog, create/edit, type-driven field visibility
    ScheduleDetails.java       # read-only view dialog
  dto/
    ScheduleDto.java           # mirrors common ScheduleResponse
    CreateScheduleRequest.java # mirrors common CreateScheduleRequest
    UpdateScheduleRequest.java # mirrors common UpdateScheduleRequest
    SchedulePage.java          # PageResponse<ScheduleDto> shape
```
(DTOs follow the existing admin convention of local records per feature, as
`feature.task` / `feature.destination` already do, rather than importing
`common` DTOs directly.)

**Navigation:** new `SideNavItem` in `MainLayout` → `ScheduleView`
(`VaadinIcon.CALENDAR` or `CLOCK`). `ScheduleView` uses a `ComboBox<TaskDto>`
(or task-id) populated from `TaskService.list()`. The grid + toolbar's "New
schedule" button are disabled/empty until a task is selected; the selected
task's id is threaded into all task-scoped calls.

**Type-driven form:** `ScheduleForm` holds a `Select<ScheduleType>` and one
field per "when":
- `DateTimePicker runAt`, `TextField cronExpression`, `IntegerField intervalSeconds`,
  `ComboBox<String> timezone` (searchable picker with common IANA zones + custom
  value support, default `UTC`), `TextField label`.
- A `valueChangeListener` on the Type select toggles field visibility so only
  the active type's field(s) show. On edit, the Type select is `setReadOnly(true)`
  and prefilled. The timezone field is hidden for `FIXED` schedules (they have no
  wall-clock meaning).
- Validation: required "when" field present for the chosen type; `intervalSeconds`
  in `(0, 86400]`; `runAt` in the future; `timezone` is blank or a valid `ZoneId`
  (any IANA zone, not just the shortlist) — mirror the API's rules for a good UX,
  but the API remains the source of truth (surface API 400s via `Notifications`).

**Service / client:** `ScheduleService` uses `KairosApiClient` like
`TaskService`. Endpoints:
- create: `POST {taskEndpoint}/{taskId}/schedules`
- list:   `GET  {taskEndpoint}/{taskId}/schedules`
- get:    `GET  {scheduleEndpoint}/{id}`
- update: `PUT  {scheduleEndpoint}/{id}`
- delete: `DELETE {scheduleEndpoint}/{id}`
- pause:  `PATCH {scheduleEndpoint}/{id}/pause`
- resume: `PATCH {scheduleEndpoint}/{id}/resume`

Add `scheduleEndpoint` to `ApiProperties` (`kairos.api.schedule-endpoint`,
e.g. `/api/v1/schedules`) and set it in `application.properties`. The nested
create/list reuse the existing `taskEndpoint` + `/{taskId}/schedules`.

**Toggle action:** the grid's pause/resume toggle maps to `PATCH .../pause`
vs `.../resume` based on `active`, mirroring `TaskGrid`'s start/stop toggle.

**Reuse:** `FilterBar`, `Fields`, `Buttons`, `Badges`, `Dialogs`,
`Notifications`, `ViewActions`, `StyleConfig`/`Tokens`, `Strings`,
`FieldValidation` — no new shared UI primitives expected (add only if a genuine
gap appears, e.g. a `DateTimePicker` helper on `Fields`).

**Error codes:** none new. UI surfaces API errors (400 validation, 404 not
found) as notifications; the API's `GlobalExceptionHandler` already maps them.

## Acceptance criteria

- [ ] A **Schedules** tab appears in the side-nav and routes to `ScheduleView`.
- [ ] Selecting a task in the picker loads and displays that task's schedules;
      no task selected → empty grid and disabled "New schedule".
- [ ] Creating a schedule of each type works end-to-end:
      ONCE (runAt), CRON (cron text + timezone), FIXED (intervalSeconds), and
      the new row appears after refresh.
- [ ] The form shows **only** the "when" field(s) for the selected type; switching
      type swaps the visible field(s).
- [ ] Editing a schedule prefills the form, **locks the Type select**, and
      persists label / when / timezone / active changes.
- [ ] Pause/resume toggle flips `active` and the status badge updates.
- [ ] Delete asks for confirmation (`Dialogs.confirmDelete`) then removes the row.
- [ ] Client-side search/filter over loaded schedules works like Tasks/Destinations.
- [ ] No string literals in components (all via `ScheduleText`/`UiText`); methods
      ≤ 40 lines; layer boundaries respected (changes confined to `kairos-admin`).
- [ ] **Timezone picker** offers a curated shortlist of common IANA zones
      (UTC, Europe/London, Europe/Berlin, Europe/Kyiv, America/New_York,
      America/Los_Angeles, Asia/Tokyo, Asia/Singapore, Australia/Sydney) but
      still accepts any free-typed valid `ZoneId`; invalid zones are marked
      invalid and block submission.
- [ ] API validation errors (e.g. past `runAt`, interval out of range) surface as
      error notifications rather than silent failures.
- [ ] `./gradlew :kairos-admin:build` passes, including any new `*Test`s
      (service + form save-logic tests in the style of
      `TaskServiceTest` / `DestinationDetailsSaveLogicTest`).

## Notes

- Depends on the Schedule CRUD API from issue #18 (spec: `doc/specs/schedule-api.md`).
- Common DTOs: `dev.kairos.common.dto.schedule.{CreateScheduleRequest,
  UpdateScheduleRequest,ScheduleResponse}`; API handler:
  `dev.kairos.api.schedule.ScheduleHandler`.
- Pattern references: `feature.task.*` (closest — nested/child concepts) and
  `feature.destination.*` (details/save-logic dialog pattern, PR #17).
- **Deferred:** cron-builder UI (separate future slice); an inline
  "schedules per task" affordance from the Task grid could be added later but is
  intentionally out of scope here.

## Issue metadata (suggested)
- **Type:** feature
- **Module(s):** module:kairos-admin
- **Priority:** priority:medium
- **Milestone:** Admin UI / Schedule management
