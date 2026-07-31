# Guided Setup Wizard (Admin UI)

## Problem

Setting up a working schedule in Kairos today means visiting three separate
screens in the right order and knowing how they relate: create a **Destination**,
create a **Task** that references it, then create a **Schedule** under that task.
A new operator has to discover this dependency chain themselves, and each screen's
form is a standalone save-and-close dialog — there is no single guided flow.

This slice adds a **Guided Setup Wizard**: one multi-step flow that walks the
operator through **Task → Destination → Schedule** in order, collecting input at
each step, showing a stepper of where they are and what's next, and committing
everything at the end with a single **Finish**. It is the natural companion to the
Dashboard's "Create Task" call-to-action (`doc/specs/dashboard-home.md`).

**For whom:** operators / admins onboarding a new scheduled task in the Kairos
admin console.

## Scope

**In:**
- A **`SetupWizard`** modal `Dialog` (large) with three ordered steps:
  1. **Task** — collect the new task's fields (always "create").
  2. **Destination** — **select an existing** destination **or create a new** one.
  3. **Schedule** — **create a new** schedule for the task (type-driven
     when-fields; always "create", no select-existing mode — see rationale
     below).
- A **top stepper**: the three steps shown as labelled tabs with a progress
  indicator (a coloured/"blue" line marking the current step). Steps are
  **display-only — not clickable**; upcoming steps are greyed to preview what's
  next. Advancement is via the footer buttons only.
- A **footer**: **Back**, **Next**, **Cancel** — with **Next** becoming
  **Finish** on the last step. Back/Next navigate between steps with **no
  side-effects** (nothing is persisted until Finish). Back is disabled on step 1.
- **Per-step validation** on **Next**: a step must be valid before advancing;
  invalid fields show inline errors (reusing `FieldValidation` / the existing
  forms' validation rules). Back never validates.
- **Commit at Finish**, resolving the dependency chain in order:
  1. If the Destination step chose "create new", POST it and capture its id;
     otherwise use the selected existing destination id.
  2. POST the Task with that destination id; capture the created task id.
  3. POST the Schedule under the task id (always — the Schedule step has no
     select-existing mode).
- **Partial-failure handling:** if a later POST fails, **stop, surface the error,
  keep the wizard open, and keep whatever was already created** (a created
  destination is valid on its own). The wizard **remembers already-created
  ids/flags** in its state — including whether the schedule POST already
  succeeded — so a repeated **Finish** **re-runs only the failed/remaining
  steps** and does not duplicate what already succeeded.
- **Launch points:** the Dashboard "Create Task" CTA opens the wizard; the Tasks
  screen gains a "Guided setup" affordance next to its existing "New task" button.
- On success: notify, close the wizard, and refresh the originating view.
- All copy (step titles, helper text, button labels, notifications) in a new
  `WizardText` constants class — **no literals in components**.
- Unit tests for the pure wizard logic (step order / navigation state, per-step
  validity gating, and the Finish commit sequencing incl. the skip-already-created
  behaviour, with the services stubbed).

**Out:**
- Any `domain` / `application` / `infrastructure` / API / schema change. This is
  `kairos-admin` UI only — it composes the **existing** create endpoints
  (`TaskService.create`, `DestinationService.create`, `ScheduleService.create`)
  and the existing list calls; **no new endpoint, no transactional multi-create
  API**. (A server-side "create task + destination + schedule atomically"
  endpoint is explicitly deferred — noted below.)
- Editing existing entities through the wizard (it creates a new task; an
  existing destination may be **selected**, not edited; the schedule is
  always newly created for the task, never selected).
- Per-task schedule listing on wizard open (see the Schedule-step rationale
  below — the "select existing schedule" mode, and the `scheduleService
  .listForTasks` fan-out it required, was removed).
- A full-page wizard route — the wizard is a modal `Dialog` over the current view.
- Clickable/jumpable stepper tabs, saving a draft, resuming a partially-filled
  wizard across sessions.

## Design

**Module / layer:** `kairos-admin` only. New code lives in a new feature package
`dev.kairos.admin.feature.wizard`, alongside `task` / `schedule` / `destination`.

**New files (indicative):**

```
feature/wizard/
  WizardText.java                 # every label, step title, helper text, button label, notification — no literals
  SetupWizard.java                # the Dialog: stepper + step body + footer; owns navigation state; runs the Finish commit
  component/
    WizardStepper.java            # top progress stepper (display-only, non-clickable, current-step highlight)
    TaskStep.java                 # step 1 body: task fields + validation (extracted from TaskForm's field/validation logic)
    DestinationStep.java          # step 2 body: select-existing / create-new toggle + fields + validation
    ScheduleStep.java             # step 3 body: always-create fields + validation (no select-existing mode)
  WizardDraft.java (or record)    # collected values across steps + captured created ids/flags (for idempotent retry)
```

The Schedule step's "when"-field validation/parsing (`validateRunAt`,
`validateInterval`, `runAtInstant`, `selectedZone`, `validateTimezone`,
`validateTimezoneIfApplicable`) is factored into a shared
`dev.kairos.admin.feature.schedule.component.ScheduleWhenFields` helper, used
by both the standalone `ScheduleForm` and `ScheduleStep`, so the two don't
duplicate the run-at-future/timezone-validity rules.

**Why the Schedule step is create-only (design change vs. the original spec):**
the wizard always creates a **new** `Task`, and `Schedule` is its own
aggregate that references a task **by id** (`.claude/CLAUDE.md` — a schedule
is never loaded/reused through a task); so a schedule for this new task must
always be newly created, there is nothing existing to attach it to. The
original "select an existing schedule's shape" mode never actually created or
copied anything — it was a pure skip marker — and to populate its picker the
wizard had to call `scheduleService.listForTasks(...)`, which loops one
paged request **per existing task** on every wizard open (an N+1 fan-out
flagged in review). Since the mode did nothing useful and cost a real N+1
round-trip, it was dropped: the Schedule step now always shows the
create-new fields, and `SetupWizard` no longer lists tasks or schedules on
open at all. The Destination step is unaffected — a `Destination` **is** a
genuinely reusable aggregate (many tasks can target one), so its
select-existing-or-create toggle stays.

- **`SetupWizard`** — a `Dialog` (`Tokens.DIALOG_WIDTH_L`) holding the
  `WizardStepper`, a swappable step body, and the Back/Next/Cancel footer. It owns
  the **current-step index** and the **`WizardDraft`** (collected field values +
  any created ids). `Next` validates the current step, copies its values into the
  draft, advances; `Back` just decrements; the last step's button reads
  **Finish** and calls the commit sequence. Methods ≤ 40 lines; assembled from
  small `build…`/`show…` helpers mirroring `ScheduleForm`.
- **Step panels** (`TaskStep` / `DestinationStep` / `ScheduleStep`) — each is a
  plain layout (not a `Dialog`) exposing `boolean validate()` and a
  `read into draft` method. Their fields and validation **reuse the existing
  forms' rules** (`Fields.*`, `FieldValidation.*`, the same required-field checks
  as `TaskForm` / `DestinationForm` / `ScheduleForm`). Where practical, the shared
  field/validation logic is factored so the existing dialog forms and the wizard
  steps don't duplicate it; but the existing save-and-close dialogs stay as they
  are for the standalone screens.
  - `DestinationStep` carries a **toggle** (e.g. `RadioButtonGroup`) between
    "Use existing" (a `ComboBox` of loaded items) and "Create new" (the inline
    fields); only the active mode is validated. `ScheduleStep` has **no such
    toggle** — it always shows the create-new (type-driven when-) fields; see
    "Why the Schedule step is create-only" below for the rationale.
- **`WizardStepper`** — renders the three step labels with the current one
  highlighted and a progress line; strictly presentational, driven by the current
  index. Styling via `StyleConfig` + `Tokens` (the "blue" line is
  `Tokens.COLOR_PRIMARY`; greyed upcoming steps use `Tokens.TEXT_SECONDARY` /
  contrast tokens) — **no inline CSS literals**; any genuinely new token is added
  to `Tokens`.

**Commit sequence (Finish) & idempotent retry:**

```
onFinish():
  if draft.destinationId == null && step2 == CREATE:
      draft.destinationId = destinationService.create(...).id()   // capture, store in draft
  if draft.taskId == null:
      draft.taskId = taskService.create(withDestination(draft.destinationId)).id()
  if !draft.scheduleCreated:
      scheduleService.create(draft.taskId, ...)
      draft.scheduleCreated = true                                // guard, mirrors destination/task id capture
  notify success; refresh; close
```

Each `draft.*Id` is written **only after** its POST succeeds, so a failure leaves
earlier ids populated; the guards (`== null`) mean a repeated Finish skips what
already succeeded and retries only the failed/remaining call. The schedule step
is guarded the same way, via a `draft.scheduleCreated` flag rather than a
captured id (the schedule create response isn't otherwise needed by a later
step) — set only after the schedule POST succeeds, so a retry after a
transient failure (e.g. the POST succeeded server-side but the response was
lost) does not double-create the schedule. A failure surfaces via
`Notifications.error` and leaves the wizard open. This mirrors the
"stop, show error, keep created" decision from discussion.

**Dependency-order note:** although the *step order shown to the user* is
**Task → Destination → Schedule** (as requested), the *commit order* is
**Destination → Task → Schedule**, because `CreateTaskRequest` requires a
`destinationId` (see `TaskForm.validate()` — destination is mandatory). The wizard
collects everything first, then commits in dependency order at Finish. This is why
commit-at-end (not commit-per-step) is required.

**Styling / conventions:**
- All CSS via `StyleConfig` + `Tokens`; no inline CSS literals; new tokens if
  needed (stepper line colour reuse, step-label states, dialog step-body min
  height).
- No Lombok. SLF4J only where a real log statement helps (e.g. logging a commit
  failure before surfacing it).
- Methods ≤ 40 lines; SRP — stepper renders, each step collects+validates one
  entity, `SetupWizard` orchestrates.
- Reuse shared primitives (`Fields`, `Buttons`, `Notifications`, `Dialogs`,
  `FieldValidation`) — no hand-rolled equivalents.

## Acceptance criteria

- [ ] A "Create Task" CTA on the Dashboard and a "Guided setup" affordance on the
      Tasks screen open the `SetupWizard` modal.
- [ ] The wizard shows three steps — **Task → Destination → Schedule** — with a
      top stepper: current step highlighted with a coloured progress line,
      upcoming steps greyed, **stepper tabs not clickable**.
- [ ] Footer shows **Back / Next / Cancel**; **Next** becomes **Finish** on the
      last step; **Back** is disabled on step 1; **Cancel** closes with nothing
      persisted.
- [ ] Back/Next navigation persists nothing to the API; a step must pass its
      validation before **Next** advances; **Back** never validates.
- [ ] The **Destination** step lets the operator **select an existing**
      destination **or create a new** one (only the active mode is validated);
      the **Schedule** step always **creates a new** schedule for the task
      (no select-existing mode).
- [ ] **Finish** commits in dependency order (destination → task → schedule),
      substituting the created/selected destination id into the task and the
      created task id into the schedule.
- [ ] If a commit step fails, the wizard **stays open, shows the error, keeps what
      was already created**, and a repeated **Finish** does not duplicate the
      already-created destination/task **or schedule**.
- [ ] On success the wizard notifies, closes, and the originating view refreshes.
- [ ] The Schedule step's **timezone picker** is built by the shared
      `ScheduleWhenFields.timezoneField()` — the same shortlist
      (`ScheduleText.TIMEZONE_OPTIONS`) and the same current-UTC-offset labels as
      the standalone `ScheduleForm`, so the two cannot drift — and still accepts
      any free-typed valid `ZoneId`; invalid zones are marked invalid and block
      advancing.
- [ ] No literals in components (all copy via `WizardText`); styling via
      `StyleConfig` + `Tokens` only; no Lombok; methods ≤ 40 lines; changes
      confined to `kairos-admin`; no domain/application/infrastructure/API/schema
      changes.
- [ ] Unit tests cover navigation/step-gating and the Finish commit sequencing
      (including skip-already-created on retry) with services stubbed.
- [ ] `./gradlew :kairos-admin:build` passes, including the new tests.

## Notes

- Companion to the Dashboard (`doc/specs/dashboard-home.md`) — the wizard is what
  the Dashboard's "Create Task" CTA launches.
- Reuses existing create endpoints only; the aggregate boundaries in
  `.claude/CLAUDE.md` are respected (Destination, Task, Schedule are separate
  aggregates created via their own endpoints — the wizard just sequences the
  client calls).
- **Deferred:** a server-side transactional "create destination + task + schedule
  in one call" endpoint (would remove the partial-failure window, but is an
  `kairos-api` concern and a separate spec). A full-page wizard route; draft
  save/resume; editing via the wizard.
- Task step is always "create" (this is a *new-task* wizard); an existing
  destination may be selected, not edited. The Schedule step is always
  "create" too — it has no select-existing mode (see the Design section's
  "Why the Schedule step is create-only" for the rationale).

## Issue metadata (suggested)

- **Type:** feature
- **Module(s):** module:admin
- **Priority:** priority:medium
- **Milestone:** Admin UI
