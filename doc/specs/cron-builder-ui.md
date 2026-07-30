# CRON Expression Builder (Admin UI)

## Problem

The Schedules admin screen (issue #20) exposes a CRON schedule's expression as a
plain text field (`ScheduleText.FIELD_CRON`) — the operator has to know cron
syntax by heart and gets no feedback until the API rejects a bad value. The
Schedule Admin UI spec explicitly deferred a visual cron editor to "a separate
future slice". This is that slice: a visual **CRON Expression Builder** dialog
that lets operators construct, validate, and preview a cron expression without
knowing the syntax, then apply it back into the schedule form.

**For whom:** operators / admins using the Kairos admin console.

## Scope

**In:**
- A `CronBuilderDialog` opened from a **"Build…"** affordance next to the CRON
  field on `ScheduleForm` (visible only when the schedule type is `CRON`).
  Prefilled with the field's current value; **Apply** writes the generated
  expression back into the field.
- **Quick Start** preset picker (`ComboBox`) — a fixed table of common schedules
  (Every minute, Every 5 / 15 minutes, Every hour, Daily midnight / 2 AM / 6 AM,
  Weekdays 9 AM, Weekly Sunday, Monthly 1st); selecting one populates all six
  builder fields.
- **Custom Expression Builder** — six field `ComboBox`es
  (seconds, minutes, hours, day-of-month, month, day-of-week) with sensible
  option lists and **custom values allowed**; each with helper text describing
  the field's syntax.
- **Generated Expression** display (read-only, monospace) + a **copy** button.
- **Human-Readable Description** ("At 02:00 every day") that updates live.
- **Next Execution Times** preview — the next 10 run times computed from the
  expression; times within the next 24 h are visually highlighted; an invalid
  expression shows an "invalid" state instead of times.
- **Live validation** via Spring's `CronExpression`; an invalid expression shows
  an inline error and blocks **Apply**.
- **Advisory warnings** (non-blocking): Feb 30/31, conflicting day-of-month +
  day-of-week, day 31 in a 30-day month.
- Footer: **Reset** (restore default), **Cancel**, **Apply**.
- Unit tests for the pure logic (validation, next-execution computation,
  description generation, preset → expression mapping, warning detection).

**Out:**
- **Server-side cron parsing / validation.** The API continues to store the cron
  string verbatim (per `doc/specs/schedule-api.md`); validation here is a UX aid
  only, client-side.
- Any `domain` / `application` / `infrastructure` / API changes — this is
  `kairos-admin` UI only, no new endpoints, no schema change.
- A 5-field (Unix) cron mode. Kairos uses **6-field Spring cron**
  (seconds-precision, `?` / `L` supported), matching `CronExpression`.
- Best-effort partial parsing of malformed input (see Design → prefill).
- Timezone handling inside the builder (timezone stays on `ScheduleForm`; the
  builder only shapes the expression string).

## Design

**Module / layer:** `kairos-admin` only (the sole module where Vaadin/Spring is
allowed). New components live under the existing
`dev.kairos.admin.feature.schedule.component` package, next to `ScheduleForm`,
`ScheduleWhen`, `ScheduleGrid` — the cron builder's only consumer is the schedule
form, so it stays feature-local rather than in `shared/ui`.

**New components:**

```
feature/schedule/
  CronText.java                     # all builder UI strings + preset table constants (no literals in components)
  component/
    CronBuilderDialog.java          # the Dialog: presets, 6 fields, generated expr, description, preview, footer
    CronPreview.java                # pure logic: validate, next-N executions, human-readable description, warnings
    CronPresets.java                # preset name → 6-field expression table
```

- **`CronBuilderDialog`** — a `Dialog` assembled from shared primitives:
  `Fields.combo` for the preset picker and the six fields (custom values enabled),
  `Buttons.primary/tertiary` for the footer, `Buttons.icon` for copy,
  `Notifications.success/error` for copy/apply feedback. It holds no cron logic —
  it delegates to `CronPreview`, wires field-change listeners to rebuild the
  expression, and calls a `Consumer<String>` on Apply. Methods ≤ 40 lines; the
  build is split into small `setup…`/`build…` helpers mirroring `ScheduleForm`.
- **`CronPreview`** — pure, Vaadin-free logic so it is unit-testable without a UI:
  - `validate(expression)` → parse via
    `org.springframework.scheduling.support.CronExpression.parse` (already on the
    classpath via `spring-boot-starter-web`; no new dependency), return a
    valid/invalid result carrying the parser message.
  - `nextExecutions(expression, count)` → the next `count` `LocalDateTime`s.
  - `describe(fields)` → the plain-English description.
  - `warnings(fields)` → the advisory warning list.
- **`CronPresets`** — the immutable name → expression table.
- **`CronText`** — every label, helper text, preset name, warning, and
  notification string (no literals scattered in components), in the style of
  `ScheduleText`.

**Integration with `ScheduleForm`:** the CRON `TextField` gains an adjacent
"Build…" button (via `Buttons`). Clicking it opens `CronBuilderDialog`
initialised from the field's current value; on Apply the returned expression is
set back into the field. The button follows the same visibility rule as the CRON
field (`showFieldsForType` → `CRON`). Note: the timezone field on the same form
is a `ComboBox<String>` (via `Fields.comboCustom`) offering a curated shortlist
of common IANA zones while accepting any free-typed valid zone — this is a
reusable pattern for timezone pickers and is also used in the setup wizard's
Schedule step.

**Cron format:** **6-field Spring cron** — `seconds minutes hours day-of-month
month day-of-week`. This matches the reference implementation and `CronExpression`
(which requires 6 fields and supports `?` and `L`). The default expression is
**Daily at 2 AM** (`0 0 2 * * ?`).

**Prefill / bad input:** when opened, if the field's current value is a parseable
6-field expression, its parts populate the six fields; otherwise the builder falls
back to the **default** (Daily at 2 AM) rather than erroring or attempting a
best-effort partial fill. This keeps the open path simple and predictable.

**Validation vs warnings:**
- **Hard errors** (block Apply): the generated expression fails
  `CronExpression.parse`. Surfaced inline with an error style + the parser message.
- **Advisory warnings** (do not block): Feb 30/31, day-of-month and day-of-week
  both specified, day 31 in a 30-day month. Shown as an info panel.

**Styling:** all CSS through `Tokens` (spacing, colours, radius, font weight,
monospace) — **no inline CSS string literals**. Any genuinely new token
(e.g. a monospace font-family, a dialog-content max-height) is added to `Tokens`;
any reusable field helper (e.g. a labelled `ComboBox` with helper text) is added
to `Fields` rather than hand-built in the dialog.

**Copy to clipboard:** via the browser clipboard API through
`getUI().getPage().executeJs(...)`, then a `Notifications.success`. The JS snippet
string lives in `CronText` (no inline literal in the component).

**No Lombok.** The reference used `@Slf4j` / Lombok; Kairos does not — plain Java,
SLF4J only where a real log statement is warranted (the `domain` framework-free
rule does not apply here since this is `kairos-admin`, but the builder is UI glue
and needs little logging).

## Acceptance criteria

- [ ] A "Build…" affordance appears next to the CRON field on `ScheduleForm`
      (only in `CRON` mode) and opens `CronBuilderDialog` prefilled from the
      field's current value, falling back to the default for unparseable input.
- [ ] Selecting a Quick Start preset populates all six fields and the generated
      expression.
- [ ] Editing any of the six fields — including a custom value — updates the
      generated expression in real time.
- [ ] The generated expression is validated live via `CronExpression`; an invalid
      expression shows an inline error and disables/blocks **Apply**.
- [ ] A human-readable description updates with the expression
      (e.g. "At 02:00 every day").
- [ ] The Next Execution Times preview lists the next 10 run times; times within
      the next 24 h are highlighted; an invalid expression shows an "invalid"
      state instead of times.
- [ ] Advisory warnings (Feb 30/31, conflicting day-of-month + day-of-week, day 31
      in a 30-day month) appear as non-blocking notices.
- [ ] **Copy** copies the generated expression (with success feedback); **Reset**
      restores the default; **Apply** writes the expression back into
      `ScheduleForm`'s CRON field and closes.
- [ ] All styling via `Tokens` (no inline CSS literals); all copy via `CronText`
      (no string literals in components); no Lombok; methods ≤ 40 lines; changes
      confined to `kairos-admin`.
- [ ] Unit tests cover `CronPreview` (validation, next-execution computation,
      description generation, warning detection) and `CronPresets`
      (name → expression mapping), in the style of `ScheduleWhenTest`.
- [ ] `./gradlew :kairos-admin:build` passes, including the new tests.

## Notes

- Builds on the Schedule Admin UI (#20) and the Schedule CRUD API (#18);
  implemented as issue #22.
- Reference implementation exists in another service but must be re-styled to
  Kairos conventions — **no Lombok, no inline CSS, no literal UI strings**, and
  shared primitives (`Fields`, `Buttons`, `Notifications`, `Tokens`) reused
  instead of hand-rolled equivalents.
- Uses `org.springframework.scheduling.support.CronExpression` (already available
  via `spring-boot-starter-web`); no new dependency. The API still stores the cron
  string as-is — see `doc/specs/schedule-api.md` ("cron syntax is not parsed
  here").
- **Deferred:** best-effort partial parsing of malformed prefill input; a 5-field
  cron mode; any server-side cron validation.

## Issue metadata (suggested)

- **Type:** feature
- **Module(s):** module:admin
- **Priority:** priority:medium
- **Milestone:** Admin UI
