# Review findings — 24-add-dashboard-home-landing-page-to-admin-ui — 2026-07-12

- Scope: `kairos-admin/src/main/java/dev/kairos/admin/feature/dashboard/**` +
  `feature/task/TaskRoutes.java` + `feature/task/TaskView.java` (staged dashboard slice, 14 files)
- Sources run: code-review (high), security-review, architecture-reviewer
- Status: **fixed** — F1+F2 applied (DashboardService.java, build+tests green); F3 skipped (INVALID).

## Validation summary

- Total findings: 3 — VALID: 2 (0 must-fix / 2 suggestion), INVALID: 1, NEEDS-HUMAN: 0
- VALID must-fix: none
- VALID suggestions: F1 (`DashboardService.java:72` — `rankBy` takes a `Stream` param instead of `List`), F2 (`DashboardService.java:72` — inline FQCN `java.util.stream.Stream`)
- INVALID:
  - Already compliant / taste-only (no rule violated): F3 (`DashboardText.java:38` — preview literals already centralized as named constants; reviewer's own detail confirms no violation)

## Source results summary

| Source | Result |
|---|---|
| architecture-reviewer | ✅ complete — 0 must-fix, 2 suggestions (F1, F2) |
| security-review | ✅ complete — no HIGH/MEDIUM findings on the dashboard slice (admin UI, no untrusted-input sink, no injection/authz surface introduced) |
| code-review (high) | ✅ complete — findings F3 (see below); recall-biased, to be validated |

## Findings

### F1 — `DashboardService.rankBy` takes a `Stream` parameter instead of a `List`
- Source: architecture-reviewer
- File: `kairos-admin/src/main/java/dev/kairos/admin/feature/dashboard/DashboardService.java:72`
- Severity (as reported): suggestion
- Detail: `rankBy(Stream<TaskDto>, …)` passes a Stream as a parameter (streams are meant to be consumed inline, not passed around). Its only caller `build` already holds a `List<TaskDto>`. Minor readability smell; inconsistent with `rankSchedulesByTask` which takes the collection.
- Proposed fix: change signature to `rankBy(List<TaskDto> tasks, Function<TaskDto,String> classifier)` and call `.stream()` inside the body, mirroring `rankSchedulesByTask`.
- Verdict: **VALID**
- Reason: Confirmed at `DashboardService.java:72` — `rankBy(Stream<TaskDto> tasks, ...)` has exactly one caller (`build`, line 68: `rankBy(tasks.stream(), TaskDto::service)`), which already holds `List<TaskDto> tasks` and only calls `.stream()` to satisfy the parameter type. The sibling method `rankSchedulesByTask` (line 77) takes the `List` directly and streams internally, so the current signature is inconsistent with the file's own established pattern for the same purpose — a real (if minor) readability/consistency smell, not speculative.
- Severity: suggestion

### F2 — Fully-qualified `java.util.stream.Stream` used inline instead of an import
- Source: architecture-reviewer
- File: `kairos-admin/src/main/java/dev/kairos/admin/feature/dashboard/DashboardService.java:72`
- Severity (as reported): suggestion
- Detail: inline FQCN `java.util.stream.Stream` breaks the file's import style; harder to scan. Becomes moot if F1 is applied (the parameter type disappears).
- Proposed fix: add `import java.util.stream.Stream;` (or apply F1, which removes the type).
- Verdict: **VALID**
- Reason: Confirmed — the import block (lines 3-15) imports `List`, `Map`, `UUID`, `Function`, `Collectors` but not `Stream`; line 72 uses the bare FQCN `java.util.stream.Stream<TaskDto>` instead, breaking the file's own import convention. Minor but real; resolved as a side effect of fixing F1 (preferred) or by adding the import.
- Severity: suggestion

### F3 — DashboardText preview literals left as flat constants (noted, not a violation)
- Source: architecture-reviewer / code-review
- File: `kairos-admin/src/main/java/dev/kairos/admin/feature/dashboard/DashboardText.java:38`
- Severity (as reported): suggestion (explicitly "no action needed" per KISS)
- Detail: preview/placeholder literals (`"in 3m"`, `"1,204"`, `"99.2%"`, failure labels) are centralized named constants with TODO comments — compliant with the "no inline literals" rule. Grouping into a record would add speculative structure for one caller (`DashboardView.previewFailures()`).
- Proposed fix: none — recorded so it isn't mistaken for an oversight. Leave as-is per KISS guardrail.
- Verdict: **INVALID**
- Reason: Already handled / already compliant. Confirmed at `DashboardText.java:38-50` — every preview literal (`"in 3m"`, `"1,204"`, `"99.2%"`, the three failure labels/counts) is a `private`-scope-free `public static final` named constant with a TODO explaining the future replacement (GUIDELINES.md "No literals in code — ever", satisfied). `DashboardView.previewFailures()` (`DashboardView.java:149-153`) consumes only these named constants, no inline literals. The finding is self-described by its own source as "not a violation, no action needed" — there is no defect to fix, so it does not warrant even a suggestion entry; recording a record/grouping refactor for a single caller would be speculative generality, contradicting KISS (GUIDELINES.md Hard rule 4).
- Severity: n/a (INVALID)

## Acceptance verification (separate report)

See `2026-07-12-24-add-dashboard-home-landing-page-to-admin-ui-acceptance.md`
(spec reconciled to code by spec-keeper; test gaps AC1/AC3 to be closed by
test-author; DashboardSettingsDialog toggle/reset untestable without a Vaadin
UI-mock harness).

## Next step

Findings are **validated**: 2 VALID (both suggestion, F1+F2 — same line,
fixable together), 1 INVALID (F3 — already compliant, no action). Run
`/fix-findings` (VALID only) when ready. This slice has **0 must-fix** —
only low-priority readability suggestions.
