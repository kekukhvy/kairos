# Review findings — #57 Extract `kairos-core`

**Branch:** `57-extract-kairos-core` (base: `develop` @ `7b710c8`)
**Date:** 2026-08-31
**Scope:** staged branch diff vs `develop`

## Nature of the diff (important for reading these findings)

This is a **structural move**, not a feature. Of 88 changed files:

- **44 main + 33 test files moved via `git mv` with byte-identical content**
  (0-line diffs, packages unchanged: `dev.kairos.domain`, `dev.kairos.application`).
- **~173 genuinely inserted lines**, confined to: `kairos-core/build.gradle` (new),
  `kairos-persistence/build.gradle`, `kairos-api/build.gradle`,
  `kairos-engine/build.gradle`, `settings.gradle`, `gradle.properties`, docs,
  and **one new test** `ScheduleTypeReadinessIT.java` (99 lines).

Verified mechanically: `git diff --cached -M --numstat develop` shows no
non-build, non-doc file with a nonzero diff except the new test.

## Findings

### 1. Duplicated dependency comment — SUGGESTION (nit)
**Source:** architecture-reviewer
**Files:** `kairos-api/build.gradle`, `kairos-engine/build.gradle`

The `testFixtures(project(':kairos-persistence'))` explanatory comment is
duplicated verbatim in both modules.

**Verdict: INVALID (won't fix).** Gradle has no clean mechanism to hoist a
per-module dependency comment, and the alternative (a root-level comment far
from the declaration it explains) is worse for readers. Duplicating two lines of
prose is the lesser evil. Accepted as-is.

### 2. `ObjectMapper` imported in the `application` layer — PRE-EXISTING, OUT OF SCOPE
**Source:** architecture-reviewer
**Files:** `kairos-core/.../application/destination/usecases/{CreateDestinationUseCase,UpdateDestinationUseCase,DestinationConfigValidator}.java`

The `application` layer imports `com.fasterxml.jackson.databind.ObjectMapper`
directly. It compiles because `common` exposes Jackson via `api`.

**Verdict: VALID but OUT OF SCOPE for this PR.** Confirmed byte-identical in
`develop` — not introduced by this refactor. The module split makes it *more
visible* (a framework type reached by the layer that is supposed to be
framework-adjacent-only), which is arguably the extraction doing its job as a
design smoke detector.

**Recommendation:** file as a follow-up issue. Fixing it here would violate the
slice's own "no behavior change" constraint and inflate a structural move with
unrelated refactoring.

### 3. Test-count discrepancy in the implementation report — CORRECTED
**Source:** orchestrator verification

The implementation report claimed `ScheduleTypeReadinessIT` added **3** tests
(engine 4 → 7). The file contains **1** `@Test` method; the engine total is
**5**, confirmed from the JUnit XML. No acceptance criterion depends on this
number; corrected here for the record and in the SDLC marker.

## Security review — no findings

No new attack surface:

- **No production code added or modified.** The moved files are byte-identical;
  the only new `.java` file is a test.
- **No new inputs, endpoints, parsers, or data flows.** REST surface, DTOs, and
  JOOQ repositories untouched. Packages unchanged, so no
  reflection/serialization contract shifted.
- **No secrets, crypto, auth, or deserialization changes.**
- The new IT uses **parameterized jOOQ queries** throughout — no string
  concatenation into SQL.
- `compileOnly org.jetbrains:annotations` has **no runtime impact**: `@NotNull`
  is `CLASS`-retention and enforces nothing at runtime, before or after.

## Verification performed by the orchestrator

- Independent `./gradlew clean build`: **BUILD SUCCESSFUL**, 66 tasks executed.
- Aggregated JUnit XML across all modules: **983 tests, 0 failures, 0 errors,
  21 skipped** — including `kairos-admin` (39 result files), so no module was
  silently skipped.
- `grep -rn "project(':kairos-api')" --include=build.gradle .` → no matches.
- New IT run in isolation: 1 test, 0 failures. Not vacuous — the H2 schema
  carries a CHECK constraint requiring `FIXED` schedules to have a valid
  `interval_seconds`, so the insert genuinely exercises the shared schema.

## code-review findings (high effort) — 4 applied, 1 declined

### 4. `kairos-core` engine dependency was `implementation` but only tests use it — VALID, FIXED
`kairos-engine/build.gradle`. Verified: the engine's main source set imports only
`dev.kairos.persistence.{AppConfig,DatabaseMigrator,DataSourceFactory}` — nothing
from `kairos-core`. The sole consumer is `ScheduleTypeReadinessIT`, a test.

**Fixed:** moved to `testImplementation` with a comment explaining it should be
promoted when the planner/claim loop lands (M5/M6). Verified `kairos-core` no
longer appears on the engine's `runtimeClasspath` (was shipping into
`installDist` for nothing).

### 5. Duplicate H2 declaration in the engine — VALID, FIXED
`testImplementation "com.h2database:h2"` was redundant with the H2 arriving via
`testFixturesApi` through the test fixtures. `kairos-api` had correctly dropped
its copy; the engine kept both. **Fixed** — removed, modules now consistent.

### 6. `org.jetbrains:annotations` added for two no-op annotations — VALID, FIXED
The new dependency + `gradle.properties` version pin existed solely to keep
`@NotNull` compiling on the `toString()` overrides of `ScheduleId` and
`DestinationId` — where it conveys nothing (`toString()` is non-null by
contract) and nothing reads it.

This also **contradicted the "zero framework dependencies" claim this same diff
restates** in `.claude/CLAUDE.md`, and formalized what the build comment itself
admitted was an "incidental transitive leak" from Javalin's Kotlin stdlib.

**Fixed:** deleted both annotations, both imports, the `compileOnly` dependency,
and the version pin. `:kairos-core:dependencies --configuration compileClasspath`
now resolves to exactly `project :common` — strengthening acceptance criterion 1
from "compileOnly caveat" to literally pure.

### 7. `H2DatabaseBase` comment claimed isolation it does not provide — VALID, FIXED
The comment read "Class name in the URL keeps separate IT classes isolated," but
the URL is hardcoded `jdbc:h2:mem:kairos_test`. Every subclass shares one
database and each `@BeforeAll` recreates the schema — safe only because no
`maxParallelForks` is set anywhere in the build.

Worth fixing precisely because promoting this class to a shared `testFixtures`
artifact **widens its blast radius** from three co-located api ITs to any module.

**Fixed:** comment corrected to state the real contract and name the sequential
-execution assumption, rather than changing behavior inside a no-behavior-change
slice. Making the URL per-class is the right follow-up *if* parallel forks are
ever enabled.

### 8. `implementation` vs `api` for `kairos-api` → `kairos-core` — DECLINED
Suggestion that `kairos-api` should expose `kairos-core` via `api` since core
types appear on its signatures.

**Verdict: INVALID for now.** Nothing consumes `kairos-api` as a library — and
criterion 8 requires that stays true. Changing the scope for a hypothetical
future consumer is exactly the speculative abstraction `GUIDELINES.md` forbids
("no abstraction without a second caller"). Revisit if a real consumer appears.

## Post-fix verification

- `./gradlew clean build` re-run after all fixes: **BUILD SUCCESSFUL**, 66 tasks.
- **983 tests, 0 failures, 0 errors, 21 skipped** — unchanged, confirming the
  fixes are behavior-preserving.
- `:kairos-core:dependencies` → exactly `project :common`.
- `:kairos-engine:runtimeClasspath` → contains no `kairos-core`.

## Summary

| Source | Findings | Must-fix | Applied |
|---|---|---|---|
| architecture-reviewer | 1 suggestion + 1 pre-existing note | 0 | 0 (both declined w/ rationale) |
| code-review (high) | 6 (2 medium-cluster, 3 low, 1 latent) | 0 | 4 fixed, 1 declined, 1 = arch note |
| security-review | 0 | 0 | — |
| orchestrator verification | 1 correction (reporting only) | 0 | — |

**Net effect of fixes:** one dependency and one version pin removed, one
dependency correctly scoped out of a production runtime classpath, one duplicate
removed, and one actively misleading comment corrected. No behavior change.
