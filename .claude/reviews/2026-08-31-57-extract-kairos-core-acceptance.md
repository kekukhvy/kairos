# Acceptance evidence — 57-extract-kairos-core — 2026-08-31

- Issue: #57 "Extract `kairos-core`"
- Base: `develop` @ `7b710c8` — changes are **staged**, not committed
- Test command(s) run: `./gradlew clean build`, plus targeted re-runs
  (`./gradlew :kairos-core:dependencies`, `:kairos-engine:test --tests
  'dev.kairos.engine.schedule.ScheduleTypeReadinessIT' -i`), plus `git diff
  --cached -M develop --numstat` for the move-integrity proof.
- Result: 9 criteria — 8 covered+passing (PASS), 1 covered+passing but with a
  **factual correction required in the issue text** (AC6 — see below).

**Important — AC6 correction.** The issue text says "All 52 existing test
classes pass without modification." That figure is wrong. Verified counts:

| | test classes (repo-wide, `src/test/java`) |
|---|---|
| Before (`develop` @ `7b710c8`) | **76** |
| After (staged) | **77** (+1 new: `ScheduleTypeReadinessIT`, 1 `@Test` method) |
| Moved `kairos-api` → `kairos-core` | 21 (16 `application` + 5 `domain`) |
| Moved `kairos-api` → `kairos-persistence` (testFixtures) | `H2DatabaseBase.java` + `h2-schema.sql` |

"52" matches nothing observed — not the before count, the after count, the
moved-file count, or the `kairos-api` test-class count (31, per the task's own
"before" tally). The **real intent** — that every moved test passes with no
modification to its assertions — is proven below and holds. The criterion
should be rewritten to say that, with the correct numbers, rather than "52".

## Coverage matrix

| AC | Criterion (short) | Evidence (test / gate) | Ran? | Result |
|----|-------------------|------------------------|------|--------|
| [AC1](#ac1) | `kairos-core` module, registered, no framework deps | `settings.gradle` + `:kairos-core:dependencies` (compile & runtime classpaths) | yes | ✅ PASS |
| [AC2](#ac2) | `domain/`+`application/` moved, no copies left | `find` + `git diff -M --numstat` | yes | ✅ PASS |
| [AC3](#ac3) | `kairos-api` depends on `kairos-core`, compiles, same REST surface | `kairos-api/build.gradle` + `TaskApiTest`/`DestinationApiTest`/`ScheduleApiTest` (140 tests) + diff vs `develop` | yes | ✅ PASS |
| [AC4](#ac4) | `kairos-engine` depends on `kairos-core`, references `Schedule`/`ScheduleType` | `ScheduleTypeReadinessIT` | yes | ✅ PASS |
| [AC5](#ac5) | `H2DatabaseBase` consumable from another module's tests | `ScheduleTypeReadinessIT extends H2DatabaseBase` (in `kairos-engine`) | yes | ✅ PASS |
| [AC6](#ac6) | "52" existing test classes pass unmodified | **issue figure wrong** — true story: 76→77 classes, moved files byte-identical, all pass | yes | ⚠️ PASS (issue text needs correction) |
| [AC7](#ac7) | `./gradlew build` green across every module | full build log | yes | ✅ PASS |
| [AC8](#ac8) | no module depends on `kairos-api` | `grep -r "project(':kairos-api')"` | yes | ✅ PASS |
| [AC9](#ac9) | `doc/` + `.claude/CLAUDE.md` module maps updated | `git diff --cached develop -- doc/ .claude/CLAUDE.md README.md` | yes | ✅ PASS |

## Evidence log

<a id="ac1"></a>
<details>
<summary>✅ <b>AC1</b> — <code>kairos-core</code> module, registered, no framework deps — <code>settings.gradle</code> + <code>:kairos-core:dependencies</code> — PASS</summary>

**Criterion:** A `kairos-core` module exists, registered in `settings.gradle`,
with no framework dependencies (pure Java + `common`)

**Evidence:** `settings.gradle:13` → `include 'kairos-core'`.

`kairos-core/build.gradle`:
```groovy
dependencies {
    api project(':common')

    // Nullability annotation on domain value objects (@NotNull). Previously
    // available only as an incidental transitive leak from Javalin's Kotlin
    // stdlib on kairos-api's classpath; declared explicitly here since
    // kairos-core must not rely on another module's transitive dependencies.
    compileOnly "org.jetbrains:annotations:${jetbrainsAnnotationsVersion}"
    ...
}
```

**Command:**
```
./gradlew :kairos-core:dependencies --configuration compileClasspath
./gradlew :kairos-core:dependencies --configuration runtimeClasspath
```

**Output (compileClasspath):**
```
compileClasspath - Compile classpath for source set 'main'.
+--- project :common
|    +--- com.fasterxml.jackson.core:jackson-databind:2.18.2  (transitive, via common)
|    +--- com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2
|    \--- org.slf4j:slf4j-api:2.0.17
\--- org.jetbrains:annotations:26.0.2

BUILD SUCCESSFUL in 398ms
```

**Output (runtimeClasspath — `compileOnly` deps are absent here):**
```
runtimeClasspath - Runtime classpath of source set 'main'.
\--- project :common
     +--- com.fasterxml.jackson.core:jackson-databind:2.18.2
     +--- com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2
     \--- org.slf4j:slf4j-api:2.0.17

BUILD SUCCESSFUL in 261ms
```

**Assessment:** The resolved dependency graph is exactly `common` (Jackson
data-binding + SLF4J façade — both pre-existing, inherited transitively from
`common`'s own `build.gradle`, unrelated to this PR) plus, on the *compile*
classpath only, `org.jetbrains:annotations` (declared `compileOnly`). No
Spring, no JOOQ, no HTTP framework, no servlet API anywhere in the graph.
`org.jetbrains:annotations` is a `CLASS`-retention nullability annotation with
zero runtime footprint (confirmed absent from `runtimeClasspath`) — it does not
introduce a framework dependency in any practical sense (no DI container, no
lifecycle, no reflection-based behavior). "Pure Java + `common`" holds.
</details>

<a id="ac2"></a>
<details>
<summary>✅ <b>AC2</b> — domain/application moved, no copies left behind — <code>find</code> + <code>git diff -M --numstat</code> — PASS</summary>

**Criterion:** `domain/` and `application/` are moved there from
`kairos-api`; no copies are left behind

**Command:**
```
find kairos-api -type d \( -name domain -o -name application \)
git diff --cached -M --stat -- kairos-api kairos-core kairos-persistence | tail -100
```

**Output:**
```
kairos-api/out/test/classes/dev/kairos/application
kairos-api/out/test/classes/dev/kairos/domain
kairos-api/out/production/classes/dev/kairos/application
kairos-api/out/production/classes/dev/kairos/domain
```
The only hits are under `kairos-api/out/` — gitignored, untracked IDE build
output (`.gitignore:32: **/out/`), confirmed with
`git check-ignore -v kairos-api/out/production/classes/dev/kairos/domain` →
matched, and `git ls-files kairos-api/out` shows no `.java`/`.class` product
tracked there. No source-tree copy remains.

`git diff --cached -M --stat` confirms 44 main-source files + 34 test-source
files renamed from `kairos-api/src/...` into `kairos-core/src/...`, every one
at `0` lines changed (see AC6 for the full move-integrity proof).

`kairos-core/src/main/java/dev/kairos` now contains exactly `application/` and
`domain/`; `kairos-api/src/main/java/dev/kairos` now contains exactly `api/`
and `infrastructure/` (verified via `find kairos-api/src/main/java/dev/kairos
-maxdepth 1 -type d`).
</details>

<a id="ac3"></a>
<details>
<summary>✅ <b>AC3</b> — kairos-api depends on kairos-core, compiles, same REST surface — build + TaskApiTest/DestinationApiTest/ScheduleApiTest — PASS</summary>

**Criterion:** `kairos-api` depends on `kairos-core` and still compiles,
exposing the same REST surface

**Dependency declaration** (`kairos-api/build.gradle`):
```groovy
dependencies {
    implementation project(':common')
    implementation project(':kairos-core')
    implementation project(':kairos-persistence')
    ...
}
```

**Concrete "same REST surface" proof — source diff, not just compilation:**
```
git diff --cached -M develop -- kairos-api/src/main/java/dev/kairos/api
```
→ **empty output**. The `api/` package (all REST handlers) is byte-for-byte
identical to `develop`; only `domain/`/`application/` left the module, and only
`build.gradle` gained the new dependency line. The REST surface is not merely
"same by inspection" — it is the literal unchanged source.

**Command (compiles + runs the real endpoint test suites):**
```
./gradlew clean build
```

**Output — per-class results from `kairos-api/build/test-results/test/*.xml`:**
```
dev.kairos.api.DestinationApiTest                            tests=36 failures=0 errors=0 skipped=0
dev.kairos.api.ScheduleApiTest                                tests=56 failures=0 errors=0 skipped=0
dev.kairos.api.task.TaskDtoMapperTest                          tests=17 failures=0 errors=0 skipped=0
dev.kairos.api.TaskApiTest                                     tests=48 failures=0 errors=0 skipped=0
```
`@Test` counts in these three files are identical to `develop`
(`TaskApiTest`=48, `DestinationApiTest`=36, `ScheduleApiTest`=56 — verified via
`git show develop:...` vs current file, byte match), and all 140 of those
tests pass against the classes now living in `kairos-core`. This is the
concrete "same REST surface, still works" proof, not just a green compile.
</details>

<a id="ac4"></a>
<details>
<summary>✅ <b>AC4</b> — kairos-engine depends on kairos-core, references Schedule/ScheduleType — <code>ScheduleTypeReadinessIT</code> — PASS</summary>

**Criterion:** `kairos-engine` depends on `kairos-core` and can reference
`Schedule` / `ScheduleType`

**Dependency declaration** (`kairos-engine/build.gradle`):
```groovy
dependencies {
    implementation project(':kairos-core')
    implementation project(':kairos-persistence')
    ...
}
```

**Test:** `kairos-engine/src/test/java/dev/kairos/engine/schedule/ScheduleTypeReadinessIT.java:35`

```java
class ScheduleTypeReadinessIT extends H2DatabaseBase {
    ...
    @Test
    void fixedScheduleBuiltFromKairosCoreRoundTripsThroughTheSharedH2Schema() {
        TaskId taskId = seedTaskWithDestination();
        Schedule fixedSchedule = Schedule.fixed(ScheduleId.newId(), taskId, SCHEDULE_LABEL, INTERVAL_SECONDS, NOW);

        insertSchedule(fixedSchedule);

        String storedType = readScheduleType(fixedSchedule.id());
        assertEquals(ScheduleType.FIXED, ScheduleType.parse(storedType));
    }
    ...
}
```
Imports `dev.kairos.domain.schedule.Schedule`, `ScheduleId`, `ScheduleType`
from `kairos-core`, builds a `Schedule` through the domain factory
`Schedule.fixed(...)`, and asserts on `ScheduleType`.

**Command:**
```
./gradlew :kairos-engine:test --tests 'dev.kairos.engine.schedule.ScheduleTypeReadinessIT' -i
```

**Output (`kairos-engine/build/test-results/test/TEST-dev.kairos.engine.schedule.ScheduleTypeReadinessIT.xml`):**
```xml
<testsuite name="dev.kairos.engine.schedule.ScheduleTypeReadinessIT" tests="1" skipped="0" failures="0" errors="0" ...>
  <testcase name="fixedScheduleBuiltFromKairosCoreRoundTripsThroughTheSharedH2Schema()"
            classname="dev.kairos.engine.schedule.ScheduleTypeReadinessIT" time="0.045"/>
  ...
</testsuite>
```
```
BUILD SUCCESSFUL in 907ms
```
1 test, 0 failures — genuine (HikariCP pool started, jOOQ banner logged, real
H2 insert/select executed, not a no-op).
</details>

<a id="ac5"></a>
<details>
<summary>✅ <b>AC5</b> — H2DatabaseBase consumable from another module's tests — <code>ScheduleTypeReadinessIT extends H2DatabaseBase</code> — PASS</summary>

**Criterion:** `H2DatabaseBase` is consumable from another module's tests
(verified by one engine test using it)

**Move + publication:**
```
{kairos-api/src/test => kairos-persistence/src/testFixtures}/java/dev/kairos/infrastructure/H2DatabaseBase.java   0 0
{kairos-api/src/test => kairos-persistence/src/testFixtures}/resources/h2-schema.sql                               0 0
```
(byte-identical move, confirmed via `git diff --cached -M develop --numstat`)

`kairos-persistence/build.gradle` applies `java-test-fixtures` and exposes it:
```groovy
plugins {
    id 'nu.studer.jooq'
    id 'java-test-fixtures'
}
...
    testFixturesApi platform("org.junit:junit-bom:${junitBomVersion}")
    testFixturesApi 'org.junit.jupiter:junit-jupiter'
    testFixturesApi "com.h2database:h2:${h2Version}"
```

`kairos-engine/build.gradle` consumes it:
```groovy
    testImplementation testFixtures(project(':kairos-persistence'))
```

**Test:** `ScheduleTypeReadinessIT extends H2DatabaseBase` (see AC4 block for
full source) — instantiated and run from `kairos-engine`'s test source set,
not `kairos-api`'s.

**Command / Output:** same run as AC4 — `tests="1" failures="0" errors="0"`,
with the H2 in-process pool (`HikariPool-1`, `jdbc:h2:mem:kairos_test`)
starting inside `kairos-engine`'s test JVM, proving the fixture is truly
consumed cross-module, not just declared.
</details>

<a id="ac6"></a>
<details>
<summary>⚠️ <b>AC6</b> — "52 existing test classes pass unmodified" — issue figure is WRONG; true story verified and holds — PASS (text needs correction)</summary>

**Criterion (verbatim):** All 52 existing test classes pass without
modification to their assertions

**This number is factually wrong.** Verified, independently re-derived counts:

```
git ls-tree -r develop --name-only | grep -E '/src/test/(java)/.*(Test|IT)\.java$' | wc -l
      76
find . -path "*/src/test/java/*" \( -name "*Test.java" -o -name "*IT.java" \) -not -path "*/out/*" | wc -l
      77
```

Full `diff` of the before/after file lists shows exactly: 21 files moved
`kairos-api` → `kairos-core` (16 `application` + 5 `domain`), plus 1 new file
`kairos-engine/.../ScheduleTypeReadinessIT.java`. Nothing else changed. 76 + 1
new = 77. "52" doesn't match the before count (76), the after count (77), the
moved-file count (21, or 23 including `H2DatabaseBase`+resource), or even the
`kairos-api`-only count (31, stated as the "before" figure in the task
prompt). No plausible reading makes 52 correct — it should be struck from the
issue.

**The real intent — no modification to moved tests' assertions — is proven
here, independently:**

```
git diff --cached -M develop --numstat -- '*/src/test/java/*'
```
```
0  0  {kairos-api => kairos-core}/src/test/java/dev/kairos/application/destination/usecases/CreateDestinationUseCaseTest.java
0  0  {kairos-api => kairos-core}/src/test/java/dev/kairos/application/destination/usecases/DeleteDestinationUseCaseTest.java
... (all 21 moved test files, every one 0/0)
99 0  kairos-engine/src/test/java/dev/kairos/engine/schedule/ScheduleTypeReadinessIT.java   <- new file, not a modification
```
```
git diff --cached -M develop --numstat -- \
  kairos-api/src/test/java/dev/kairos/infrastructure/H2DatabaseBase.java \
  kairos-persistence/src/testFixtures/java/dev/kairos/infrastructure/H2DatabaseBase.java
0  0  {kairos-api/src/test => kairos-persistence/src/testFixtures}/java/dev/kairos/infrastructure/H2DatabaseBase.java
```
Every moved test file (21 application/domain tests + `H2DatabaseBase.java` +
`h2-schema.sql`) shows `0 0` — git's own byte-level diff confirms zero lines
changed, so no assertion was touched. The only non-zero test-source diff in
the whole staged changeset is the one genuinely new file.

**Command (that all 77 test classes pass):**
```
./gradlew clean build
```

**Output — aggregated from all `build/test-results/test/*.xml`:**
```
files: 77
tests: 983  failures: 0  errors: 0  skipped: 21
```
(One name collision inflates a bare-name tally to "76 unique names" —
`dev.kairos.common.util.helpers.JsonConverterTest` exists as two distinct,
pre-existing source files, one in `common/src/test`, one in
`kairos-api/src/test`, testing two different `ObjectMapperFactory`s under the
same package/class name. Confirmed both are real, current-run XML outputs, not
stale artifacts, via `stat` timestamps and `grep` of their independent
source. File-count 77 is the correct class count.)

**Per-module breakdown (test classes / all passing):**
```
common:            2
kairos-persistence: 2
kairos-core:       21   (moved from kairos-api, 280 tests, 0 failures)
kairos-api:        10   (140 API-endpoint tests + others, 0 failures)
kairos-engine:      3   (incl. the 1 new ScheduleTypeReadinessIT test, 0 failures)
kairos-admin:      39
                   ---
                   77
```

**Verdict:** the underlying behavior this criterion is meant to protect — the
move introduces no behavior change and all pre-existing tests keep passing
unmodified — is **fully proven**. The **issue text's "52" is wrong** and
should be rewritten to state the true, verified numbers (76 → 77, 21 moved
files, 0 lines of assertion diff) rather than an invented figure.
</details>

<a id="ac7"></a>
<details>
<summary>✅ <b>AC7</b> — <code>./gradlew build</code> green across every module — full build log — PASS</summary>

**Criterion:** `./gradlew build` passes

**Command:**
```
./gradlew clean build
```

**Output (tail):**
```
> Task :kairos-adapters:webhook:build

[Incubating] Problems report is available at: file:///Users/vladyslavkekukh/Developer/Java/kairos/build/reports/problems/problems-report.html

BUILD SUCCESSFUL in 11s
66 actionable tasks: 66 executed
```
All 66 tasks (spanning `common`, `kairos-persistence`, `kairos-core`,
`kairos-api`, `kairos-engine`, `kairos-worker`, `kairos-adapters:{kafka,sqs,
webhook,rabbitmq}`, `kairos-admin`, `kairos-sdk`) executed successfully on a
fresh (`clean`) build; no module skipped or cached-stale.
</details>

<a id="ac8"></a>
<details>
<summary>✅ <b>AC8</b> — no module depends on kairos-api — <code>grep -r "project(':kairos-api')"</code> — PASS</summary>

**Criterion:** No module depends on `kairos-api`; `grep -r
"project(':kairos-api')"` returns nothing

**Command:**
```
grep -rn "project(':kairos-api')" --include=build.gradle .
```

**Output:** no matches (grep exit code 1 / empty). Confirmed no
`build.gradle` in the repository declares a dependency on `kairos-api`; it
remains a leaf/entry-point module as intended (only `settings.gradle` still
lists `include 'kairos-api'`, which is expected — it's still a runnable
component, just no longer a dependency target).
</details>

<a id="ac9"></a>
<details>
<summary>✅ <b>AC9</b> — doc/ and .claude/CLAUDE.md module maps reflect new layout — diff vs develop — PASS</summary>

**Criterion:** `doc/` and `.claude/CLAUDE.md` module maps reflect the new
layout

**Command:**
```
git diff --cached develop --numstat -- doc/ .claude/CLAUDE.md README.md
```

**Output:**
```
11  1  .claude/CLAUDE.md
1   0  README.md
20  0  doc/plan.md
```

`.claude/CLAUDE.md` module map now lists `kairos-core` first with its role
described, and a new paragraph explains the `domain`/`application` split and
the `testFixtures` publication:
```
kairos-core/        # Domain + application layers (entities, use cases) — shared library
kairos-api/         # REST API (entry point): api/ + infrastructure/ only
...
`domain/` and `application/` (see "Architecture" above) live in `kairos-core`, a pure
Java library with zero framework dependencies...
```

`README.md`'s tree diagram gained the `kairos-core/` line; `doc/plan.md`
gained a new milestone section `M3.6 — Core Module Extraction ✅` documenting
every structural change made.

`doc/database.md` and `doc/specification.md` were checked and correctly left
untouched — their `kairos-api`/`common` references concern the destination
config-schema contract, unrelated to where `domain`/`application` physically
live, and remain accurate (`kairos-api` still exists as the REST entry point).
</details>

## Gaps

None. All 9 criteria are covered by a real, passing test run or a directly
inspected build/dependency-graph/diff artifact.

## Note for the issue

AC6's "52" is a factual error and should be corrected when the issue is
updated post-verification. Suggested replacement text:

> All pre-existing test classes pass without modification to their
> assertions (76 → 77 test classes repo-wide: 21 moved `domain`/`application`
> test files land in `kairos-core` byte-identical, plus 1 new engine test
> `ScheduleTypeReadinessIT`; verified via `git diff -M --numstat`, every
> moved file shows 0 lines changed).

## Verdict

9/9 acceptance criteria verified with passing tests or direct inspection.
0 gaps, 0 fails, 0 not-run.
DONE (all covered & green) — but flag AC6's issue-text figure ("52") as
factually wrong; recommend `spec-keeper`/issue-edit correct it to the verified
76→77 numbers before closing the issue.
