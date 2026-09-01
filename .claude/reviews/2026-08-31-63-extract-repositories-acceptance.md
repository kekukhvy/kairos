# Acceptance evidence — 63-extract-repositories — 2026-08-31

- Scope: Issue #63 (primary, AC1–AC6) + Issue #62 folded in (AC7–AC8)
- Base: `develop` @ `1974542` (`refactor: extract kairos-core so shared layers stop blocking components (#61)`)
- Changes: staged (not yet committed)
- Test command(s) run:
  - `./gradlew clean build`
  - `./gradlew :kairos-engine:test --tests 'dev.kairos.engine.schedule.ScheduleRepositoryReadinessIT' -i`
  - `./gradlew :kairos-sdk:dependencies --configuration compileClasspath` / `testCompileClasspath`
  - `./gradlew :kairos-admin:dependencies --configuration compileClasspath` / `runtimeClasspath`
  - `grep -r "project(':kairos-api')" --include="*.gradle" .`
  - `grep -rn "com.fasterxml" kairos-core/src/main`
  - `git diff --cached -M develop` + blob-hash (`git hash-object`) comparison on the 8 moved repository/mapper/IT files
- Result: 8 criteria — 8 covered+passing, 0 gaps, 0 not-run

## Coverage matrix

| AC | Criterion (short) | Evidence (test / gate) | Ran? | Result |
|----|-------------------|------------------------|------|--------|
| [AC1](#ac1) | Repos/mappers reachable from `kairos-engine` w/o `kairos-api` dep | `kairos-engine/build.gradle` graph + `ScheduleRepositoryReadinessIT` | yes | ✅ PASS |
| [AC2](#ac2) | `grep project(':kairos-api')` returns nothing | grep | yes | ✅ PASS |
| [AC3](#ac3) | `kairos-sdk` / `kairos-admin` don't get JOOQ/Hikari/Postgres | `dependencies` reports | yes | ✅ PASS |
| [AC4](#ac4) | A real `kairos-engine` test reads `schedules` via `ScheduleRepository` | `ScheduleRepositoryReadinessIT#findByTaskId_readsFixedScheduleThroughTheRepositoryPort` | yes | ✅ PASS |
| [AC5](#ac5) | Behavior unchanged: existing tests pass, no assertion edits | blob-hash diff on 8 moved files + full test run | yes | ✅ PASS |
| [AC6](#ac6) | `./gradlew build` green | `./gradlew clean build` | yes | ✅ PASS |
| [AC7](#ac7) | `application` layer no longer imports Jackson | `grep -rn com.fasterxml kairos-core/src/main` | yes | ✅ PASS |
| [AC8](#ac8) | Destination config validation identical (non-object config, missing key) | `CreateDestinationUseCaseTest#execute_withConfigNotAJsonObject_throwsValidationException` / `#execute_withConfigMissingRequiredKey_throwsValidationException` | yes | ✅ PASS |

## Evidence log

<a id="ac1"></a>
<details>
<summary>✅ <b>AC1</b> — JOOQ repos/mappers reachable from <code>kairos-engine</code> without a <code>kairos-api</code> dependency — dependency graph + <code>ScheduleRepositoryReadinessIT</code> — PASS</summary>

**Criterion:** JOOQ-репозитории и мапперы доступны из `kairos-engine` без зависимости на `kairos-api`

**Evidence 1 — full `project(':...')` dependency graph** (`grep -rn "project(':" --include="*.gradle" .`, developer only, no gradle output edited):

```
kairos-admin/build.gradle:43:    implementation project(':common')
kairos-api/build.gradle:13:    implementation project(':common')
kairos-api/build.gradle:14:    implementation project(':kairos-core')
kairos-api/build.gradle:15:    implementation project(':kairos-persistence')
kairos-core/build.gradle:8:    api project(':common')
kairos-persistence/build.gradle:28:    api project(':kairos-core')
kairos-engine/build.gradle:12:    implementation project(':kairos-persistence')
kairos-engine/build.gradle:22:    testImplementation project(':kairos-core')
kairos-engine/build.gradle:25:    testImplementation testFixtures(project(':kairos-persistence'))
```

Dependency direction: `common ← kairos-core ← kairos-persistence ← {kairos-api, kairos-engine}`. Nobody depends on `kairos-api`. The `Jooq*Repository`/`*Mapper` classes physically live at
`kairos-persistence/src/main/java/dev/kairos/infrastructure/{task,schedule,destination}/*.java`
(confirmed moved via `git status`: `renamed: kairos-api/...→kairos-persistence/...`), a module `kairos-engine` already depends on.

**Evidence 2 — real usage from the engine module**, see AC4 below (`ScheduleRepositoryReadinessIT`), which imports `dev.kairos.infrastructure.schedule.JooqScheduleRepository` directly from `kairos-engine/src/test` and constructs it — this would not even compile if the class weren't visible from `kairos-engine`'s classpath without `kairos-api`.

**Command:**
```
grep -rn "project(':" --include="*.gradle" .
```
(real output pasted above, captured this session)
</details>

<a id="ac2"></a>
<details>
<summary>✅ <b>AC2</b> — <code>grep -r "project(':kairos-api')"</code> still returns nothing — grep — PASS</summary>

**Criterion:** `grep -r "project(':kairos-api')"` по-прежнему ничего не возвращает

**Command:**
```
grep -r "project(':kairos-api')" --include="*.gradle" .
```

**Output:**
```
(no output, exit code 1)
```
</details>

<a id="ac3"></a>
<details>
<summary>✅ <b>AC3</b> — <code>kairos-sdk</code> / <code>kairos-admin</code> get no JOOQ/Hikari/Postgres on classpath — <code>./gradlew :kairos-sdk:dependencies</code> / <code>:kairos-admin:dependencies</code> — PASS</summary>

**Criterion:** `kairos-sdk` и `kairos-admin` не получают JOOQ / Hikari / драйвер Postgres на classpath (проверить через `./gradlew :kairos-sdk:dependencies`)

**Honesty note on `kairos-sdk`:** `kairos-sdk/build.gradle` is a stub (`description = 'TODO'`), zero source files, zero declared dependencies. The check passes **trivially** — there's nothing to pull anything in yet. Confirmed for completeness, not as a strong signal.

**`kairos-sdk` command + output:**
```
./gradlew :kairos-sdk:dependencies --configuration compileClasspath
...
compileClasspath - Compile classpath for source set 'main'.
No dependencies
BUILD SUCCESSFUL in 1s

./gradlew :kairos-sdk:dependencies --configuration testCompileClasspath
...
testCompileClasspath - Compile classpath for source set 'test'.
No dependencies
BUILD SUCCESSFUL in 4s
```

**`kairos-admin` (has real Vaadin+Spring code) — the real check:**
```
./gradlew :kairos-admin:dependencies --configuration compileClasspath | grep -iE 'jooq|hikari|postgres'
./gradlew :kairos-admin:dependencies --configuration runtimeClasspath | grep -iE 'jooq|hikari|postgres'
```

**Output:** both greps returned no matches (exit code 1), against a 288-line runtime dependency tree. `kairos-admin/build.gradle` only depends on `project(':common')`, Vaadin, and Spring Boot web — no `kairos-persistence`/`kairos-core` dependency at all.
</details>

<a id="ac4"></a>
<details>
<summary>✅ <b>AC4</b> — one real <code>kairos-engine</code> test reads <code>schedules</code> via <code>ScheduleRepository</code> — <code>ScheduleRepositoryReadinessIT#findByTaskId_readsFixedScheduleThroughTheRepositoryPort</code> — PASS</summary>

**Criterion:** Один реальный тест в `kairos-engine` читает `schedules` через `ScheduleRepository`

**Test:** `kairos-engine/src/test/java/dev/kairos/engine/schedule/ScheduleRepositoryReadinessIT.java:66`

```java
class ScheduleRepositoryReadinessIT extends H2DatabaseBase {
    ...
    // Declared as the port type, not the jOOQ class — the assertion below only
    // ever calls through ScheduleRepository.
    private ScheduleRepository scheduleRepository;

    @BeforeEach
    void setUp() {
        scheduleRepository = new JooqScheduleRepository(dslContext);
        dslContext.execute("DELETE FROM schedules");
        dslContext.execute("DELETE FROM tasks");
        dslContext.execute("DELETE FROM destinations");
    }

    @Test
    void findByTaskId_readsFixedScheduleThroughTheRepositoryPort() {
        TaskId taskId = seedTaskWithDestination();
        ScheduleId scheduleId = ScheduleId.newId();
        seedFixedSchedule(scheduleId, taskId);

        List<Schedule> result = scheduleRepository.findByTaskId(taskId, LIMIT_TEN, OFFSET_ZERO);

        assertEquals(1, result.size());
        Schedule loaded = result.get(0);
        assertEquals(scheduleId, loaded.id());
        assertEquals(taskId, loaded.taskId());
        assertEquals(INTERVAL_SECONDS, loaded.intervalSeconds());
        assertTrue(loaded.active());
    }
```

Seeds a `schedules` row via a plain JOOQ insert (bypassing `save()`, whose upsert needs real Postgres, per the test's own javadoc), then reads it back exclusively through the `ScheduleRepository` **port type** (field is typed `ScheduleRepository`, not `JooqScheduleRepository`), backed in-process by H2 (PostgreSQL mode) via `H2DatabaseBase` from `kairos-persistence`'s `testFixtures`.

**Command:**
```
./gradlew :kairos-engine:test --tests 'dev.kairos.engine.schedule.ScheduleRepositoryReadinessIT' -i
```

**Output:**
```
ScheduleRepositoryReadinessIT > findByTaskId_readsFixedScheduleThroughTheRepositoryPort() STANDARD_OUT
    ... HikariPool-1 - Starting...
    ... HikariPool-1 - Added connection conn0: url=jdbc:h2:mem:kairos_test user=SA
    ... HikariPool-1 - Start completed.
    ... jOOQ tip of the day ...

BUILD SUCCESSFUL in 9s
15 actionable tasks: 1 executed, 14 up-to-date
```

JUnit XML (`kairos-engine/build/test-results/test/TEST-dev.kairos.engine.schedule.ScheduleRepositoryReadinessIT.xml`):
```xml
<testsuite name="dev.kairos.engine.schedule.ScheduleRepositoryReadinessIT" tests="1" skipped="0" failures="0" errors="0" ... time="1.645">
  <testcase name="findByTaskId_readsFixedScheduleThroughTheRepositoryPort()" classname="dev.kairos.engine.schedule.ScheduleRepositoryReadinessIT" time="0.457"/>
</testsuite>
```
</details>

<a id="ac5"></a>
<details>
<summary>✅ <b>AC5</b> — behavior unchanged: existing tests pass without assertion edits — blob-hash diff + full suite run — PASS</summary>

**Criterion:** Поведение не меняется: существующие тесты проходят без правки ассертов

**Part 1 — the 8 moved repository/mapper/IT files are byte-identical to `develop`.**
`git status`/`git diff --stat --cached -M develop` already report these as pure renames with `0` changed lines. To remove any doubt about rename-heuristic false positives, each file's blob content was hashed independently on both sides:

```
old (develop, kairos-api/...)                                        new (staged, kairos-persistence/...)          result
kairos-api/.../task/JooqTaskRepository.java                        → kairos-persistence/.../task/JooqTaskRepository.java           IDENTICAL
kairos-api/.../task/TaskMapper.java                                 → kairos-persistence/.../task/TaskMapper.java                    IDENTICAL
kairos-api/.../schedule/JooqScheduleRepository.java                 → kairos-persistence/.../schedule/JooqScheduleRepository.java    IDENTICAL
kairos-api/.../schedule/ScheduleMapper.java                         → kairos-persistence/.../schedule/ScheduleMapper.java            IDENTICAL
kairos-api/.../destination/JooqDestinationRepository.java           → kairos-persistence/.../destination/JooqDestinationRepository.java IDENTICAL
kairos-api/src/test/.../task/JooqTaskRepositoryIT.java              → kairos-persistence/src/test/.../task/JooqTaskRepositoryIT.java  IDENTICAL
kairos-api/src/test/.../schedule/JooqScheduleRepositoryIT.java      → kairos-persistence/src/test/.../schedule/JooqScheduleRepositoryIT.java IDENTICAL
kairos-api/src/test/.../destination/JooqDestinationRepositoryIT.java→ kairos-persistence/src/test/.../destination/JooqDestinationRepositoryIT.java IDENTICAL
```

Command used per pair:
```bash
old_hash=$(git show develop:<old-path> | git hash-object --stdin)
new_hash=$(git show :<new-path> | git hash-object --stdin)
```
All 8 pairs: `IDENTICAL`. No assertion in any of the moved repository ITs was touched.

**Part 2 — the destination use-case tests that DID change** (`CreateDestinationUseCaseTest`, `UpdateDestinationUseCaseTest`) changed **wiring only**, not assertions (needed for AC7/#62's `ConfigKeyReader` port):

```diff
--- a/kairos-core/src/test/java/dev/kairos/application/destination/usecases/CreateDestinationUseCaseTest.java
-import com.fasterxml.jackson.databind.ObjectMapper;
...
-        useCase = new CreateDestinationUseCase(destinationRepository, FIXED_CLOCK, new ObjectMapper());
+        useCase = new CreateDestinationUseCase(destinationRepository, FIXED_CLOCK, new JacksonConfigKeyReaderForTests());

--- a/kairos-core/src/test/java/dev/kairos/application/destination/usecases/UpdateDestinationUseCaseTest.java
-import com.fasterxml.jackson.databind.ObjectMapper;
...
-        useCase = new UpdateDestinationUseCase(destinationRepository, new ObjectMapper());
+        useCase = new UpdateDestinationUseCase(destinationRepository, new JacksonConfigKeyReaderForTests());
```
That is the **entire diff** for both files (`git diff --cached -M develop -- <file>`, verified in full — only the import line and the constructor-arg line changed). No `assert*` line touched. `JacksonConfigKeyReaderForTests` (new file) wraps the same `JsonConverter.topLevelKeys` Jackson call the production `JacksonConfigKeyReader` uses, so real JSON-parsing behavior is preserved in the tests, only routed through the new port instead of a raw `ObjectMapper`.

**Part 3 — pre-existing `@Disabled` count is unchanged (not a regression from this move).**

```
develop  kairos-api/.../task/JooqTaskRepositoryIT.java         @Disabled(UPSERT_NEEDS_POSTGRES) count = 10
develop  kairos-api/.../schedule/JooqScheduleRepositoryIT.java @Disabled(UPSERT_NEEDS_POSTGRES) count = 6
develop  kairos-api/.../destination/JooqDestinationRepositoryIT.java @Disabled(UPSERT_NEEDS_POSTGRES) count = 5
staged   kairos-persistence/.../task/JooqTaskRepositoryIT.java         @Disabled(UPSERT_NEEDS_POSTGRES) count = 10
staged   kairos-persistence/.../schedule/JooqScheduleRepositoryIT.java @Disabled(UPSERT_NEEDS_POSTGRES) count = 6
staged   kairos-persistence/.../destination/JooqDestinationRepositoryIT.java @Disabled(UPSERT_NEEDS_POSTGRES) count = 5
```
10+6+5 = 21, matching the aggregated `skipped=21` in the full build (see AC6) exactly — same reason (`UPSERT_NEEDS_POSTGRES`, upsert semantics H2 can't express), same files, present in `develop` before this slice. Confirmed: **not a regression**.

**Part 4 — full suite passes.** See AC6 for the full `./gradlew clean build` run and the aggregated JUnit XML (989 tests, 0 failures, 0 errors, 21 skipped — up from 983 on `develop`; the +6 new tests are `ScheduleRepositoryReadinessIT` (1) + `DestinationConfigValidatorTest` (5)).

**Commands:**
```
git diff --cached -M develop --stat -- 'kairos-persistence/src/main/java/dev/kairos/infrastructure/*' 'kairos-persistence/src/test/java/dev/kairos/infrastructure/*'
git diff --cached -M develop -- kairos-core/src/test/java/dev/kairos/application/destination/usecases/CreateDestinationUseCaseTest.java kairos-core/src/test/java/dev/kairos/application/destination/usecases/UpdateDestinationUseCaseTest.java
grep -c "@Disabled(UPSERT_NEEDS_POSTGRES)" <each file, both branches>
```
</details>

<a id="ac6"></a>
<details>
<summary>✅ <b>AC6</b> — <code>./gradlew build</code> is green — <code>./gradlew clean build</code> — PASS</summary>

**Criterion:** `./gradlew build` зелёный

**Command:**
```
./gradlew clean build
```

**Output (tail):**
```
> Task :kairos-adapters:webhook:build

[Incubating] Problems report is available at: file:///Users/vladyslavkekukh/Developer/Java/kairos/build/reports/problems/problems-report.html

BUILD SUCCESSFUL in 35s
66 actionable tasks: 66 executed
```

**Aggregated JUnit XML across all modules** (`common`, `kairos-admin`, `kairos-api`, `kairos-core`, `kairos-engine`, `kairos-persistence` — every module with a `test` task):
```
common          {'tests': 27,  'failures': 0, 'errors': 0, 'skipped': 0}
kairos-admin    {'tests': 372, 'failures': 0, 'errors': 0, 'skipped': 0}
kairos-api      {'tests': 184, 'failures': 0, 'errors': 0, 'skipped': 0}
kairos-core     {'tests': 305, 'failures': 0, 'errors': 0, 'skipped': 0}
kairos-engine   {'tests': 8,   'failures': 0, 'errors': 0, 'skipped': 0}
kairos-persistence {'tests': 93, 'failures': 0, 'errors': 0, 'skipped': 21}
TOTAL           {'tests': 989, 'failures': 0, 'errors': 0, 'skipped': 21}
```
Matches the pre-established fact (989 tests, 0 failures, 0 errors, 21 skipped) exactly, re-derived independently from a fresh `clean build`.
</details>

<a id="ac7"></a>
<details>
<summary>✅ <b>AC7</b> — <code>application</code> layer no longer imports Jackson — grep — PASS</summary>

**Criterion:** The `application` layer no longer imports Jackson: `grep -rn "com.fasterxml" kairos-core/src/main` must return NOTHING.

**Command:**
```
grep -rn "com.fasterxml" kairos-core/src/main
```

**Output:**
```
(no output, exit code 1)
```

**Design confirmation:** `kairos-core/src/main/java/dev/kairos/domain/destination/ConfigKeyReader.java` is a new framework-free port (`Set<String> topLevelKeys(String config)`); `DestinationConfigValidator`, `CreateDestinationUseCase`, `UpdateDestinationUseCase` now depend on `ConfigKeyReader`, not `ObjectMapper`. The Jackson-backed implementation (`JacksonConfigKeyReader`) lives in `kairos-api/src/main/java/dev/kairos/infrastructure/destination/JacksonConfigKeyReader.java` and is wired in `kairos-api/src/main/java/dev/kairos/ApplicationContext.java`:
```java
import dev.kairos.domain.destination.ConfigKeyReader;
import dev.kairos.infrastructure.destination.JacksonConfigKeyReader;
...
ConfigKeyReader configKeyReader = new JacksonConfigKeyReader(objectMapper);
```
</details>

<a id="ac8"></a>
<details>
<summary>✅ <b>AC8</b> — destination config validation behaves identically (non-object config, missing required key) — <code>CreateDestinationUseCaseTest#execute_withConfigNotAJsonObject_throwsValidationException</code> / <code>#execute_withConfigMissingRequiredKey_throwsValidationException</code> — PASS</summary>

**Criterion:** Destination config validation behaves identically — same `ValidationException` on a non-object config and on a missing required key.

**Test:** `kairos-core/src/test/java/dev/kairos/application/destination/usecases/CreateDestinationUseCaseTest.java:108` and `:151` — driven through the real Jackson-backed `JacksonConfigKeyReaderForTests` (not a stub), so the JSON parsing behavior under test is real, not mocked.

```java
@Test
void execute_withConfigMissingRequiredKey_throwsValidationException() {
    CreateDestinationCommand cmd = new CreateDestinationCommand(
            DEFAULT_ID, DestinationType.SQS.name(), "{}");

    assertThrows(ValidationException.class, () -> useCase.execute(cmd));
}

@Test
void execute_withConfigMissingRequiredKey_messageNamesMissingKey() {
    CreateDestinationCommand cmd = new CreateDestinationCommand(
            DEFAULT_ID, DestinationType.SQS.name(), "{}");

    ValidationException ex = assertThrows(ValidationException.class, () -> useCase.execute(cmd));

    assertTrue(ex.getMessage().contains("queueUrl"));
}
...
@Test
void execute_withConfigNotAJsonObject_throwsValidationException() {
    CreateDestinationCommand cmd = new CreateDestinationCommand(
            DEFAULT_ID, DestinationType.KAFKA.name(), "[\"topic\"]");

    assertThrows(ValidationException.class, () -> useCase.execute(cmd));
}
```

`UpdateDestinationUseCaseTest` covers the missing-required-key path for updates too (`execute_withConfigMissingRequiredKeyForStoredType_throwsValidationException`, `..._messageNamesMissingKey`, `..._doesNotSave`), same `ValidationException` contract, same assertion text (`"config is missing required key(s): topic"`), unchanged from before this slice (see AC5 Part 2 diff — wiring-only change).

**Command:**
```
./gradlew clean build
```
(these tests are part of the standard `kairos-core:test` task run inside the full build; no separate run needed since assertions are unchanged from `develop`, but individually confirmed below.)

**Output — JUnit XML** (`kairos-core/build/test-results/test/TEST-dev.kairos.application.destination.usecases.CreateDestinationUseCaseTest.xml`):
```xml
<testcase name="execute_withConfigNotAJsonObject_throwsValidationException()" classname="dev.kairos.application.destination.usecases.CreateDestinationUseCaseTest" time="0.055"/>
<testcase name="execute_withConfigMissingRequiredKey_throwsValidationException()" classname="dev.kairos.application.destination.usecases.CreateDestinationUseCaseTest" time="0.003"/>
<testcase name="execute_withConfigMissingRequiredKey_messageNamesMissingKey()" classname="dev.kairos.application.destination.usecases.CreateDestinationUseCaseTest" .../>
<testcase name="execute_withConfigMissingRequiredKey_doesNotSave()" classname="dev.kairos.application.destination.usecases.CreateDestinationUseCaseTest" .../>
```
No `<failure>` child elements on any of the four testcases — confirmed programmatically (`failures: 0` for each, parsed from the XML). All PASS.
</details>

## Gaps

None. All 8 criteria (6 from #63, 2 from #62) are backed by a real test run, a real grep, or a real dependency-report inspection.

## Notes / honesty caveats

- **AC3 / `kairos-sdk`:** the check passes trivially — `kairos-sdk` is an empty stub module (`description = 'TODO'`, no source, no declared dependencies at all). It cannot leak JOOQ/Hikari/Postgres because it depends on nothing. `kairos-admin` (real Vaadin+Spring code) is the meaningful half of this criterion and was verified against its actual 288-line `runtimeClasspath` dependency tree — zero matches for `jooq|hikari|postgres`.
- **AC5:** confirmed both by git's own rename detection (`git status` / `git diff --stat`, 0 changed lines on all 8 moved files) and independently by direct blob-hash comparison (`git hash-object`) between the `develop` copy and the staged copy — belt and suspenders, no reliance on heuristic rename detection alone.
- **Skipped test count:** all 21 skips are pre-existing `@Disabled(UPSERT_NEEDS_POSTGRES)` annotations (10 task + 6 schedule + 5 destination), present with identical counts on `develop`. Not a regression from this move.

## Verdict

`8/8 acceptance criteria verified with passing tests. 0 gaps.`
DONE (all covered & green) — `./gradlew clean build`: BUILD SUCCESSFUL, 66/66 tasks; 989 tests, 0 failures, 0 errors, 21 pre-existing skips (unchanged from `develop`).
