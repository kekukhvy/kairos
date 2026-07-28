# Review findings — 34-schedule-count-badge — 2026-07-28

- Scope: working-tree diff for issue #34 (`git diff HEAD -- common kairos-api kairos-admin`), uncommitted on branch `34-schedule-count-badge`.
- Sources run: code-review (high), security-review, architecture-reviewer
- Summary: **0 must-fix. 0 security findings.** 4 low-severity suggestions/notes, several pre-existing or by-design.

## Validation summary

Validated inline during review (every cited `file:line` was read in-context). Counts:
- VALID must-fix: **0**
- VALID suggestion: **1** (F4 — test-only literal)
- INVALID / by-design / out-of-scope: **3** (F1 pre-existing convention; F2, F3 by-design, documented)
- NEEDS-HUMAN: **0**

Only F4 is worth applying, and it is a trivial test-readability fix. F1–F3 are deliberate, correct choices — no action.

## Findings

### F1 — Repository IT uses H2, not Testcontainers+Postgres
- Source: code-review + architecture-reviewer
- File: kairos-api/src/test/java/dev/kairos/infrastructure/schedule/JooqScheduleRepositoryIT.java (extends `H2DatabaseBase`)
- Severity (as reported): suggestion
- Detail: AC #12 and CLAUDE.md call for "Testcontainers + real Postgres" for repository integration tests. This IT (and all others in the repo) run on in-memory H2 in PostgreSQL-compat mode. A Postgres-specific behavior of the grouped `COUNT(*)` / partial-index path could differ from H2 and go uncaught.
- Proposed fix: none for this slice — it matches the **project-wide, pre-existing** convention (`JooqTaskRepositoryIT`, `JooqDestinationRepositoryIT` also use `H2DatabaseBase`). Migrating to Testcontainers is a repo-wide test-infra decision, out of scope for issue #34.
- Verdict: **INVALID (out of scope)** — pre-existing project-wide convention; this slice only adds test methods to an already-H2-based IT class. Not a defect of #34. Worth a separate repo-wide ticket if the team wants Testcontainers, but not here.

### F2 — `start()`/`stop()` re-query the active-schedule count needlessly
- Source: code-review
- File: kairos-api/src/main/java/dev/kairos/api/task/TaskHandler.java:155-169 (`start`, `stop`)
- Severity (as reported): suggestion
- Detail: Toggling a **task's** `active` flag cannot change how many of its **schedules** are active, yet both handlers issue a `countActiveByTaskIds` round-trip to populate the response. One avoidable DB query per toggle.
- Proposed fix: acceptable trade-off — reusing `activeScheduleCountFor(id)` keeps a single count code path and returns an always-accurate count. If avoided, `start`/`stop` would need a separate "count unchanged" path. Leave as-is unless the toggle path is shown to be hot.
- Verdict: **INVALID (by design)** — the single count code path (KISS) is worth one cheap indexed query; the response stays authoritative. No action.

### F3 — `create()` hardcodes `activeScheduleCount = 0`
- Source: code-review
- File: kairos-api/src/main/java/dev/kairos/api/task/TaskHandler.java:113-116 (`create`)
- Severity (as reported): suggestion
- Detail: Correct today (a task cannot have schedules at creation time, which is why the query is skipped — a deliberate optimization). It does couple the handler to that invariant: if a future create path created schedules atomically, the response would report 0 while the DB has active schedules.
- Proposed fix: none needed now; the inline comment already documents the assumption. Revisit only if create ever composes schedule creation.
- Verdict: **INVALID (by design)** — correct today, the assumption is documented inline, and skipping the query is a deliberate optimization. No action.

### F4 — Test asserts against literal `"badge"` instead of a `Tokens` constant
- Source: architecture-reviewer
- File: kairos-admin/src/test/java/dev/kairos/admin/feature/task/component/TaskGridTest.java:217 (`badgeIn` helper)
- Severity (as reported): suggestion (test-only)
- Detail: `span.getElement().getThemeList().contains("badge")` hardcodes `"badge"` rather than deriving it from `Tokens.THEME_BADGE_CONTRAST` (`"badge contrast"`). If the theme name ever changes, the assertion drifts silently from the production constant. Low severity, test-only.
- Proposed fix: reference the constant — e.g. filter on a shared `Tokens.THEME_BADGE` token used by both `TaskGrid.scheduleBadge` and the test, or assert `Tokens.THEME_BADGE_CONTRAST.contains(themeToken)`.
- Verdict: **VALID (suggestion)** — cheap test-readability improvement; apply. `Tokens.THEME_BADGE_CONTRAST` is `"badge contrast"`, a two-token theme string, so introduce a `Tokens.THEME_BADGE = "badge"` base token, compose `THEME_BADGE_CONTRAST` from it or reuse it in `TaskGrid`, and have the test filter on `Tokens.THEME_BADGE`.
- Outcome: **FIXED** — added `Tokens.THEME_BADGE = "badge"`; `THEME_BADGE_SUCCESS`/`THEME_BADGE_CONTRAST` now compose from it; `TaskGridTest.badgeIn` filters on `Tokens.THEME_BADGE` instead of the literal `"badge"`. `TaskGridTest` reruns green.
