# Sync-agent run markers — 57-extract-kairos-core

- test-author | 2026-08-31 | NOT NEEDED as a separate run: this is a structural move, so the existing suite IS the specification — 21 test classes moved byte-identical (`git diff -M --numstat` shows `0 0` for every one), which is the criterion. The one genuinely new test, `ScheduleTypeReadinessIT`, was written test-first by tdd-implementer (RED confirmed by stashing the engine build.gradle deps → 9 compile errors; GREEN after restoring). Verified non-vacuous: the H2 schema carries a CHECK constraint requiring `FIXED` schedules to have a valid `interval_seconds`, so the insert genuinely exercises the shared schema | files: 1
- spec-keeper | 2026-08-31 | ran: `.claude/CLAUDE.md` (module map gains `kairos-core` + testFixtures note), `README.md` (project-structure tree), `doc/plan.md` (new `M3.6 — Core Module Extraction` section, matching the M3.5 historical-log precedent; later corrected to state the engine dependency is `testImplementation` until the planner lands) | files: 3
- user-docs-writer | 2026-08-31 | NOT NEEDED: no endpoint, DTO, request/response field, error code, or SDK surface changed. The REST surface is byte-identical to develop — verified by `git diff -M develop -- kairos-api/src/main/java/dev/kairos/api/` returning empty. `doc/usage/` describes how client services call Kairos; a Gradle module boundary is invisible to them | files: 0
- javadoc-writer | 2026-08-31 | NOT NEEDED: no new public production type or member. The 44 moved main files kept their existing Javadoc unchanged (byte-identical). The single new type is a test class, `ScheduleTypeReadinessIT`, which already carries a class-level Javadoc explaining precisely which two structural facts it proves and why | files: 0
- logging-instrumenter | 2026-08-31 | NOT NEEDED: no production code was added or modified — the moved files are byte-identical, so every existing log statement moved with its class at its existing level. No new non-domain branch exists to instrument, and nothing may be logged in `domain` (it must stay framework-free) | files: 0

## Notes
- architecture-reviewer: 0 must-fix. One nit (duplicated dependency comment across
  two build files — declined, Gradle has no clean way to hoist a per-module
  dependency comment). One pre-existing observation: the `application` layer
  imports `ObjectMapper` directly. Confirmed byte-identical in `develop`, so not
  introduced here — the module split merely makes it more visible, which is
  arguably the extraction working as a design smoke detector. **Filed as a
  follow-up rather than fixed in-slice**, since fixing it would violate this
  slice's own no-behavior-change constraint.
- code-review (high): 0 correctness bugs; 6 build-hygiene findings, 4 applied:
  1. `kairos-core` was `implementation` on the engine but used only by its tests
     → moved to `testImplementation`; verified it no longer ships on the engine's
     `runtimeClasspath`.
  2. Duplicate H2 declaration in the engine (also arriving via testFixtures) →
     removed; now consistent with how `kairos-api` was written.
  3. `org.jetbrains:annotations` + its version pin had been added solely to keep
     two no-op `@NotNull`s compiling on `toString()` overrides — and contradicted
     the "zero framework dependencies" claim this same diff restates. Both
     annotations, the dependency, and the pin deleted;
     `:kairos-core:dependencies` now resolves to exactly `project :common`.
  4. `H2DatabaseBase`'s comment claimed per-class DB isolation that its hardcoded
     URL does not provide. Corrected to state the real contract and name the
     sequential-execution assumption — important now that promoting the class to
     a shared fixture widens its blast radius beyond `kairos-api`.
  Declined: switching `kairos-api`'s core dependency to `api` — no consumer
  exists, and criterion 8 requires none appear; speculative per GUIDELINES.md.
- security-review: no findings. No production code added or modified (moved files
  are byte-identical), no new inputs/endpoints/parsers, no secrets or crypto. The
  new IT uses parameterized jOOQ queries throughout.
- acceptance-verifier: 9/9 PASS, 0 gaps. Evidence:
  `.claude/reviews/2026-08-31-57-extract-kairos-core-acceptance.md`.
- **Issue-text error to correct before closing:** criterion 6 says "All 52
  existing test classes pass". The real figures, independently re-derived, are
  **76 → 77 test classes repo-wide** (21 moved byte-identical into `kairos-core`,
  plus 1 new). The criterion's *intent* — moved tests pass with no assertion
  changes — is fully proven; only the number is wrong. An earlier implementation
  report also miscounted `ScheduleTypeReadinessIT` as 3 tests; it has 1.
