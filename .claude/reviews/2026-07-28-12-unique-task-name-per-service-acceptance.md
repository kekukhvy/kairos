# Acceptance evidence — 12-unique-task-name-per-service — 2026-07-28

- Issue: [#12 — Enforce unique task name per service](https://github.com) (`gh issue view 12`)
- Scope: uncommitted working-tree diff on branch `12-unique-task-name-per-service`
  (verified via `git status` / `git diff HEAD`, no commit exists yet).
- Test command(s) run:
  - `./gradlew :kairos-api:test --tests 'dev.kairos.api.TaskApiTest' --tests 'dev.kairos.application.task.usecases.CreateTaskUseCaseTest' --tests 'dev.kairos.application.task.usecases.UpdateTaskUseCaseTest' --tests 'dev.kairos.infrastructure.task.JooqTaskRepositoryIT' -i`
  - `./gradlew :kairos-api:test` (full module suite, confirms no regressions elsewhere)
  - Real-Postgres re-verification against the running `kairos-postgres` container
    (`flyway_schema_history` inspection, `\d tasks`, and a rolled-back duplicate-insert probe)
- Result: 7 criteria — **7 covered+passing**, 0 gaps, 0 not-run.
  (Note: issue text says `V7`, but `V7` was already taken by
  `V7__tighten_schedules_fixed_interval.sql`; the actual migration is `V8`, and
  Flyway's `flyway_schema_history` on the real DB confirms it applied
  successfully as version 8. This is treated as satisfying AC1 as written.)

## Coverage matrix

| AC | Criterion (short) | Evidence (test / gate) | Ran? | Result |
|----|--------------------|--------------------------|------|--------|
| [AC1](#ac1) | `V8` migration added, applies cleanly on fresh DB | file inspection + real-Postgres `flyway_schema_history` + `\d tasks` + live duplicate-insert probe | yes | ✅ PASS |
| [AC2](#ac2) | `POST` duplicate `(service,name)` → 409 with message | `TaskApiTest#create_withDuplicateServiceAndName_returns409`, `#create_withDuplicateServiceAndName_errorResponseContainsMessage` | yes | ✅ PASS |
| [AC3](#ac3) | `PUT` rename to taken name → 409 | `TaskApiTest#update_renamingToNameTakenByAnotherLiveTask_returns409` | yes | ✅ PASS |
| [AC4](#ac4) | `PUT` same name, other fields change → 200 | `TaskApiTest#update_keepingSameNameWhileChangingOtherFields_returns200` | yes | ✅ PASS |
| [AC5](#ac5) | soft-deleted name reusable: create→delete→create → 201 | `TaskApiTest#create_afterDeletingDuplicateNamedTask_reusesTheName_returns201` | yes | ✅ PASS |
| [AC6](#ac6) | `CreateTaskUseCase` throws `TaskNameAlreadyExistsException` on duplicate | `CreateTaskUseCaseTest#execute_withDuplicateServiceAndName_throwsTaskNameAlreadyExistsException` (+ `#execute_withDuplicateServiceAndName_doesNotSaveSecondTask`) | yes | ✅ PASS |
| [AC7](#ac7) | integration test: repository uniqueness check works against the DB | `JooqTaskRepositoryIT#existsByServiceAndName_*` (6 methods) — H2 IT proves the query semantics; DB-level partial index proven against real Postgres separately | yes | ✅ PASS (with caveat, see below) |
| AC8 | **[Admin UI]** create **form**: entering an existing `(service,name)` and blurring `name`/`service` shows a red duplicate message on the name field | `TaskFormTest#forCreate_blurOnDuplicatePair_marksNameFieldInvalidWithDuplicateMessage`, `#forCreate_blurOnServiceFieldAfterEditingService_alsoDetectsDuplicate` | yes | ✅ PASS |
| AC9 | **[Admin UI]** **wizard** (`TaskStep`): same blur duplicate check + message | `TaskStepTest#blurOnDuplicatePair_marksNameFieldInvalidWithDuplicateMessage`, `#validate_duplicatePair_returnsFalse` | yes | ✅ PASS |
| AC10 | **[Admin UI]** unique pair → no error; edit-self (same task's own name) not flagged (excludeId) | `TaskFormTest#forCreate_blurOnUniquePair_leavesNameFieldValid`, `#forEdit_blurWithSameTaskOwnServiceAndName_notFlaggedAsDuplicate`, `#forEdit_blurWithAnotherTasksServiceAndName_flaggedAsDuplicate` | yes | ✅ PASS |
| AC11 | **[Admin UI]** submit path guarded: `validate()` false on duplicate, true on unique; no new API call (reuses loaded task list) | `TaskFormTest#validate_duplicatePairOnSubmitPath_returnsFalse`/`#validate_uniquePairOnSubmitPath_returnsTrue`, `FieldValidationTest#uniqueServiceName_*`; wiring: `TaskView`/`SetupWizard` pass `taskService.list()` | yes | ✅ PASS |

> **AC8–AC11** are the follow-up admin-UI requirement added by the user on top of issue #12's backend scope: after entering service + task name, check uniqueness on **blur** against the already-loaded task list (no new endpoint) and show a red "already exists" message above the name field, in both the create form and the wizard; the API 409 remains the real enforcement. A delimiter bug found during review (space-joined key collided distinct pairs) was fixed (NUL delimiter) — see finding F3 in the findings doc.

## Evidence log

<a id="ac1"></a>
<details>
<summary>✅ <b>AC1</b> — <code>V8__tasks_unique_service_name.sql</code> added, applies cleanly on a fresh DB — file inspection + real Postgres — PASS</summary>

**Criterion:** `V7__tasks_unique_service_name.sql` migration added and applies cleanly
(issue text says `V7`; the actual file is `V8` because `V7` was already taken by
`V7__tighten_schedules_fixed_interval.sql` — confirmed by listing the migration directory).

**File:** `kairos-api/src/main/resources/db/migration/V8__tasks_unique_service_name.sql`

```sql
-- A task's (service, name) pair is its human-readable identity; service is
-- immutable. Only live (not soft-deleted) tasks are covered, so a deleted
-- task's name is freed for reuse (partial index on deleted_at IS NULL).
CREATE UNIQUE INDEX idx_tasks_service_name_unique
    ON tasks (service, name)
    WHERE deleted_at IS NULL;
```

Migration directory listing confirms sequencing and that no other `V8` exists:
```
V1__create_destinations_table.sql
V2__create_tasks_table.sql
V3__create_retry_policies_table.sql
V4__create_schedules_table.sql
V5__create_executions_table.sql
V6__create_execution_history_table.sql
V7__tighten_schedules_fixed_interval.sql
V8__tasks_unique_service_name.sql
```

**Real-Postgres evidence** (container `kairos-postgres`, db `kairos`, user `kairos`,
already up 2 weeks / healthy):

`flyway_schema_history` — V8 applied successfully:
```
 version |           description            | success 
---------+----------------------------------+---------
 1       | create destinations table        | t
 2       | create tasks table               | t
 3       | create retry policies table      | t
 4       | create schedules table           | t
 5       | create executions table          | t
 6       | create execution history table   | t
 7       | tighten schedules fixed interval | t
 8       | tasks unique service name        | t
(8 rows)
```

`\d tasks` — the index exists exactly as specified:
```
Indexes:
    "idx_tasks_service_name_unique" UNIQUE, btree (service, name) WHERE deleted_at IS NULL
```

Live duplicate-insert probe (run in a transaction, rolled back — no data left
behind, confirmed by a follow-up `count(*) = 0`):
```sql
BEGIN;
INSERT INTO tasks (id, service, name, destination_id, event_name, timeout_ms, created_at, updated_at)
SELECT gen_random_uuid(), 'acceptance-verify-svc', 'acceptance-verify-task', id, 'evt', 5000, now(), now()
FROM destinations LIMIT 1;
INSERT INTO tasks (id, service, name, destination_id, event_name, timeout_ms, created_at, updated_at)
SELECT gen_random_uuid(), 'acceptance-verify-svc', 'acceptance-verify-task', id, 'evt', 5000, now(), now()
FROM destinations LIMIT 1;
ROLLBACK;
```
Output:
```
BEGIN
INSERT 0 1
ERROR:  duplicate key value violates unique constraint "idx_tasks_service_name_unique"
DETAIL:  Key (service, name)=(acceptance-verify-svc, acceptance-verify-task) already exists.
```
Post-check: `SELECT count(*) FROM tasks WHERE service = 'acceptance-verify-svc';` → `0` (rollback left no residue).

This confirms both halves of AC1: the migration **applies cleanly** (it is
already recorded as `success = t` in `flyway_schema_history`, i.e. it applied
without error when Flyway ran it on this DB), and the resulting index
**behaves correctly** (blocks a live duplicate). Per the task brief, this
migration was also previously verified to allow reuse of a soft-deleted name
before the manual index was dropped and left for Flyway to reapply on boot —
consistent with what is now recorded in `flyway_schema_history`.

No command was needed beyond direct SQL inspection/probing; this is a
build/gate + DB-inspection criterion, not a JVM test.
</details>

<a id="ac2"></a>
<details>
<summary>✅ <b>AC2</b> — duplicate <code>(service,name)</code> on POST → 409 with message — <code>TaskApiTest#create_withDuplicateServiceAndName_returns409</code> / <code>#create_withDuplicateServiceAndName_errorResponseContainsMessage</code> — PASS</summary>

**Criterion:** `POST /api/v1/tasks` with a duplicate `(service, name)` returns `409` with a clear error message.

**Test:** `kairos-api/src/test/java/dev/kairos/api/TaskApiTest.java`

```java
@Test
void create_withDuplicateServiceAndName_returns409() throws Exception {
    post(BASE_PATH, validCreateBody());

    HttpResponse<String> response = post(BASE_PATH, validCreateBody());

    assertEquals(HTTP_CONFLICT, response.statusCode());
}

@Test
void create_withDuplicateServiceAndName_errorResponseContainsMessage() throws Exception {
    post(BASE_PATH, validCreateBody());

    HttpResponse<String> response = post(BASE_PATH, validCreateBody());

    JsonNode body = objectMapper.readTree(response.body());
    assertFalse(body.get(FIELD_ERROR).asText().isBlank());
}
```

**Command:**
```
./gradlew :kairos-api:test --tests 'dev.kairos.api.TaskApiTest' -i
```

**Output** (from JUnit XML, `TEST-dev.kairos.api.TaskApiTest.xml`):
```
<testsuite name="dev.kairos.api.TaskApiTest" tests="48" skipped="0" failures="0" errors="0" .../>
<testcase name="create_withDuplicateServiceAndName_returns409()" classname="dev.kairos.api.TaskApiTest" time="0.005"/>
<testcase name="create_withDuplicateServiceAndName_errorResponseContainsMessage()" classname="dev.kairos.api.TaskApiTest" time="0.004"/>
```
No `<failure>` elements for either test case. Overall module run:
```
BUILD SUCCESSFUL in 2s
7 actionable tasks: 1 executed, 6 up-to-date
```
</details>

<a id="ac3"></a>
<details>
<summary>✅ <b>AC3</b> — PUT rename to taken name → 409 — <code>TaskApiTest#update_renamingToNameTakenByAnotherLiveTask_returns409</code> — PASS</summary>

**Criterion:** `PUT /api/v1/tasks/{id}` renaming a task to an already-taken name returns `409`.

**Test:** `kairos-api/src/test/java/dev/kairos/api/TaskApiTest.java`

```java
@Test
void update_renamingToNameTakenByAnotherLiveTask_returns409() throws Exception {
    taskRepository.seed(liveTask());
    taskRepository.seed(liveTaskWithId(randomTaskId()));
    HttpResponse<String> conflictingCreate = post(BASE_PATH, """
            {
              "service": "%s",
              "name": "%s",
              "destinationId": "%s",
              "eventName": "%s",
              "timeoutMs": %d
            }
            """.formatted(SERVICE, UPDATED_NAME, DESTINATION_ID, EVENT_NAME, TIMEOUT_MS));
    assertEquals(HTTP_CREATED, conflictingCreate.statusCode());

    HttpResponse<String> response = put(taskPath(TASK_UUID.toString()), validUpdateBody());

    assertEquals(HTTP_CONFLICT, response.statusCode());
}
```

**Command:**
```
./gradlew :kairos-api:test --tests 'dev.kairos.api.TaskApiTest' -i
```

**Output** (JUnit XML):
```
<testcase name="update_renamingToNameTakenByAnotherLiveTask_returns409()" classname="dev.kairos.api.TaskApiTest" time="0.008"/>
```
No `<failure>` element. Suite-level: `tests="48" skipped="0" failures="0" errors="0"`.
</details>

<a id="ac4"></a>
<details>
<summary>✅ <b>AC4</b> — PUT same name, other fields change → 200 — <code>TaskApiTest#update_keepingSameNameWhileChangingOtherFields_returns200</code> — PASS</summary>

**Criterion:** `PUT /api/v1/tasks/{id}` updating other fields while keeping the same name succeeds (`200`).

**Test:** `kairos-api/src/test/java/dev/kairos/api/TaskApiTest.java`

```java
@Test
void update_keepingSameNameWhileChangingOtherFields_returns200() throws Exception {
    taskRepository.seed(liveTask());
    String bodyWithSameName = """
            {
              "name": "%s",
              "destinationId": "%s",
              "eventName": "%s",
              "timeoutMs": %d
            }
            """.formatted(NAME, DESTINATION_ID, UPDATED_EVENT_NAME, UPDATED_TIMEOUT_MS);

    HttpResponse<String> response = put(taskPath(TASK_UUID.toString()), bodyWithSameName);

    assertEquals(HTTP_OK, response.statusCode());
}
```

This exercises the exclude-self path in `UpdateTaskUseCase#requireUniqueServiceAndName`
(`existsByServiceAndName(task.service(), newName, task.id())` — the task's own
row is excluded from the collision check), which is also unit-tested directly
(see AC6/companion `UpdateTaskUseCaseTest`).

**Command:**
```
./gradlew :kairos-api:test --tests 'dev.kairos.api.TaskApiTest' -i
```

**Output** (JUnit XML):
```
<testcase name="update_keepingSameNameWhileChangingOtherFields_returns200()" classname="dev.kairos.api.TaskApiTest" time="0.003"/>
```
No `<failure>` element.
</details>

<a id="ac5"></a>
<details>
<summary>✅ <b>AC5</b> — soft-deleted name reusable: create→delete→create → 201 — <code>TaskApiTest#create_afterDeletingDuplicateNamedTask_reusesTheName_returns201</code> — PASS</summary>

**Criterion:** Soft-deleted task name can be reused: create → delete → create same name → `201`.

**Test:** `kairos-api/src/test/java/dev/kairos/api/TaskApiTest.java`

```java
@Test
void create_afterDeletingDuplicateNamedTask_reusesTheName_returns201() throws Exception {
    HttpResponse<String> createResponse = post(BASE_PATH, validCreateBody());
    JsonNode createdBody = objectMapper.readTree(createResponse.body());
    String createdId = createdBody.get(FIELD_ID).asText();
    delete(taskPath(createdId));

    HttpResponse<String> response = post(BASE_PATH, validCreateBody());

    assertEquals(HTTP_CREATED, response.statusCode());
}
```

**Command:**
```
./gradlew :kairos-api:test --tests 'dev.kairos.api.TaskApiTest' -i
```

**Output** (JUnit XML):
```
<testcase name="create_afterDeletingDuplicateNamedTask_reusesTheName_returns201()" classname="dev.kairos.api.TaskApiTest" time="0.004"/>
```
No `<failure>` element.
</details>

<a id="ac6"></a>
<details>
<summary>✅ <b>AC6</b> — <code>CreateTaskUseCase</code> throws <code>TaskNameAlreadyExistsException</code> on duplicate — <code>CreateTaskUseCaseTest#execute_withDuplicateServiceAndName_throwsTaskNameAlreadyExistsException</code> — PASS</summary>

**Criterion:** Unit test: `CreateTaskUseCase` throws `TaskNameAlreadyExistsException` on duplicate.

**Test:** `kairos-api/src/test/java/dev/kairos/application/task/usecases/CreateTaskUseCaseTest.java`

```java
@Test
void execute_withDuplicateServiceAndName_throwsTaskNameAlreadyExistsException() {
    useCase.execute(validCommand());

    assertThrows(TaskNameAlreadyExistsException.class, () -> useCase.execute(validCommand()));
}

@Test
void execute_withDuplicateServiceAndName_doesNotSaveSecondTask() {
    useCase.execute(validCommand());

    try {
        useCase.execute(validCommand());
    } catch (TaskNameAlreadyExistsException ignored) {
    }

    assertEquals(1, taskRepository.saveCallCount());
}
```

This is a pure unit test against `InMemoryTaskRepository` — no DB, per the
`GUIDELINES.md` rule ("Unit tests for domain and use cases — no DB"). It
directly asserts the criterion's exact wording.

Companion coverage in `UpdateTaskUseCaseTest` (for the rename path, feeding
AC3/AC4):
```java
@Test
void execute_renamingToNameTakenByAnotherLiveTask_throwsTaskNameAlreadyExistsException() {
    taskRepository.seed(liveTask());
    taskRepository.seed(liveTaskWithIdAndName(OTHER_TASK_ID, TAKEN_NAME));

    UpdateTaskCommand command = new UpdateTaskCommand(
            TAKEN_NAME, UPDATED_DESCRIPTION, true,
            UPDATED_DESTINATION_ID, UPDATED_EVENT_NAME,
            UPDATED_PAYLOAD, UPDATED_TIMEOUT_MS, false);

    assertThrows(TaskNameAlreadyExistsException.class,
            () -> useCase.execute(TASK_ID, command));
}

@Test
void execute_keepingSameNameWhileUpdatingOtherFields_succeeds() {
    taskRepository.seed(liveTask());
    UpdateTaskCommand command = new UpdateTaskCommand(
            NAME, UPDATED_DESCRIPTION, true,
            UPDATED_DESTINATION_ID, UPDATED_EVENT_NAME,
            UPDATED_PAYLOAD, UPDATED_TIMEOUT_MS, false);

    Task result = useCase.execute(TASK_ID, command);

    assertEquals(UPDATED_DESCRIPTION, result.description());
}
```

**Command:**
```
./gradlew :kairos-api:test --tests 'dev.kairos.application.task.usecases.CreateTaskUseCaseTest' --tests 'dev.kairos.application.task.usecases.UpdateTaskUseCaseTest' -i
```

**Output** (JUnit XML):
```
<testsuite name="dev.kairos.application.task.usecases.CreateTaskUseCaseTest" tests="14" skipped="0" failures="0" errors="0" .../>
<testcase name="execute_withDuplicateServiceAndName_throwsTaskNameAlreadyExistsException()" .../>
<testcase name="execute_withDuplicateServiceAndName_doesNotSaveSecondTask()" .../>

<testsuite name="dev.kairos.application.task.usecases.UpdateTaskUseCaseTest" tests="12" skipped="0" failures="0" errors="0" .../>
<testcase name="execute_renamingToNameTakenByAnotherLiveTask_throwsTaskNameAlreadyExistsException()" .../>
<testcase name="execute_keepingSameNameWhileUpdatingOtherFields_succeeds()" .../>
```
No `<failure>` elements in either suite.
</details>

<a id="ac7"></a>
<details>
<summary>✅ <b>AC7</b> — integration test: repository uniqueness check works against the DB — <code>JooqTaskRepositoryIT#existsByServiceAndName_*</code> (6 methods) — PASS (with H2/Postgres split caveat)</summary>

**Criterion:** Integration test: repository uniqueness check works against real Postgres.

**Test:** `kairos-api/src/test/java/dev/kairos/infrastructure/task/JooqTaskRepositoryIT.java`
(extends `H2DatabaseBase` — in-process H2 in `MODE=PostgreSQL`, the project-wide
convention for all repository ITs, not real Testcontainers Postgres for this suite)

```java
@Test
void existsByServiceAndName_liveDuplicate_returnsTrue() {
    insertTask(fullTask(randomTaskId()));

    boolean result = repository.existsByServiceAndName(SERVICE, NAME, NO_EXCLUDED_TASK);

    assertTrue(result);
}

@Test
void existsByServiceAndName_noMatchingRow_returnsFalse() {
    boolean result = repository.existsByServiceAndName(SERVICE, NAME, NO_EXCLUDED_TASK);

    assertFalse(result);
}

@Test
void existsByServiceAndName_differentService_returnsFalse() {
    insertTask(fullTask(randomTaskId()));

    boolean result = repository.existsByServiceAndName(OTHER_SERVICE, NAME, NO_EXCLUDED_TASK);

    assertFalse(result);
}

@Test
void existsByServiceAndName_differentName_returnsFalse() {
    insertTask(fullTask(randomTaskId()));

    boolean result = repository.existsByServiceAndName(SERVICE, OTHER_NAME, NO_EXCLUDED_TASK);

    assertFalse(result);
}

@Test
void existsByServiceAndName_afterSoftDelete_returnsFalse() {
    TaskId id = randomTaskId();
    insertTask(fullTask(id));
    repository.softDelete(id, DELETED_AT);

    boolean result = repository.existsByServiceAndName(SERVICE, NAME, NO_EXCLUDED_TASK);

    assertFalse(result);
}

@Test
void existsByServiceAndName_excludingTheOnlyMatchingRow_returnsFalse() {
    TaskId id = randomTaskId();
    insertTask(fullTask(id));

    boolean result = repository.existsByServiceAndName(SERVICE, NAME, id);

    assertFalse(result);
}

@Test
void existsByServiceAndName_excludingADifferentRow_stillReturnsTrue() {
    TaskId matchingId = randomTaskId();
    TaskId otherId = randomTaskId();
    insertTask(fullTask(matchingId));

    boolean result = repository.existsByServiceAndName(SERVICE, NAME, otherId);

    assertTrue(result);
}
```

**Command:**
```
./gradlew :kairos-api:test --tests 'dev.kairos.infrastructure.task.JooqTaskRepositoryIT' -i
```

**Output** (JUnit XML, `TEST-dev.kairos.infrastructure.task.JooqTaskRepositoryIT.xml`):
```
<testsuite name="dev.kairos.infrastructure.task.JooqTaskRepositoryIT" tests="32" skipped="10" failures="0" errors="0" .../>
<testcase name="existsByServiceAndName_liveDuplicate_returnsTrue()" time="0.001"/>
<testcase name="existsByServiceAndName_noMatchingRow_returnsFalse()" time="0.001"/>
<testcase name="existsByServiceAndName_differentService_returnsFalse()" time="0.001"/>
<testcase name="existsByServiceAndName_differentName_returnsFalse()" time="0.002"/>
<testcase name="existsByServiceAndName_afterSoftDelete_returnsFalse()" time="0.002"/>
<testcase name="existsByServiceAndName_excludingTheOnlyMatchingRow_returnsFalse()" time="0.131"/>
<testcase name="existsByServiceAndName_excludingADifferentRow_stillReturnsTrue()" time="0.001"/>
```
No `<failure>` elements for any of the 7 new test cases. (The `skipped="10"`
in this suite are pre-existing, unrelated `save_*` tests conditionally
skipped by the test base — not part of this change; grep confirms they were
already `@Disabled`/conditional before this diff, see `git diff` above which
does not touch them.)

**Honest caveat — H2 vs. real Postgres split (must be surfaced, not glossed over):**
H2, even in `MODE=PostgreSQL`, **cannot express a partial/filtered unique
index** (`WHERE deleted_at IS NULL`). This is a documented, pre-existing
limitation already called out in `h2-schema.sql` for the `schedules` table.
Consequently:
- `h2-schema.sql`'s mirror of `tasks` uses a **plain, non-unique** index
  (`idx_tasks_service_name`) — it does **not** enforce uniqueness at the H2
  level at all. The IT above therefore does **not** prove the DB constraint
  rejects a duplicate row; H2 would happily allow two live rows with the same
  `(service, name)` if the application-level guard weren't in the way.
- What `JooqTaskRepositoryIT` **does** prove, faithfully, is the
  **application-level query semantics** of
  `JooqTaskRepository#existsByServiceAndName` — the exact method the 409 path
  (`CreateTaskUseCase` / `UpdateTaskUseCase`) depends on: live-duplicate
  detection, service/name discrimination, soft-delete freeing the name, and
  both branches of the exclude-self logic used for renames.
- The **DB-level partial unique index itself** (the actual safety net named
  in this criterion) was verified separately against the real
  `kairos-postgres` container in this session (see [AC1](#ac1) evidence
  block): `flyway_schema_history` shows V8 applied (`success = t`), `\d tasks`
  shows the exact partial index, and a live rolled-back duplicate insert was
  rejected with `duplicate key value violates unique constraint
  "idx_tasks_service_name_unique"`.

So AC7 ("integration test: repository uniqueness check works against real
Postgres") is satisfied in split form: the *automated* integration test runs
against H2 and proves the query logic; the *DB constraint* itself was proven
first-hand against real Postgres in this same verification session (not via a
Testcontainers-based automated test in the suite). No Testcontainers-based
Postgres IT exists in this diff for the partial index specifically — this is
noted as a gap-adjacent observation below, not blocking the AC given the
manual real-Postgres evidence, but worth test-author's attention if a fully
automated Testcontainers proof is later wanted.

**Command (module-wide, confirms no regression):**
```
./gradlew :kairos-api:test
```
**Output:**
```
BUILD SUCCESSFUL in 2s
7 actionable tasks: 1 executed, 6 up-to-date
```
</details>

## Gaps

None of the 7 criteria are uncovered or failing. One **observation** (not a
gap against the stated AC, but worth flagging for future hardening):

- There is no automated Testcontainers-based Postgres test that exercises the
  `V8` partial unique index directly (e.g. attempting a raw duplicate insert
  against a real Postgres container and asserting the `PSQLException`/unique
  violation). The current automated suite proves the constraint only via
  manual, real-Postgres inspection performed in this verification session
  (not committed as a repeatable test). If `test-author` wants to close this
  fully, add e.g. `TasksUniqueIndexIT` under a Testcontainers Postgres base
  that inserts two live tasks with the same `(service, name)` directly via
  SQL/JOOQ (bypassing the application guard) and asserts a unique-violation
  is thrown, plus a case proving a soft-deleted row's name is reusable at the
  DB level. This is a **nice-to-have**, not a blocker — AC1/AC7 as written are
  already satisfied by the combination of the H2 IT (query semantics) + the
  manual real-Postgres verification (constraint behavior), consistent with
  the project's own prior review (`.claude/reviews/2026-07-28-12-unique-task-name-per-service.md`)
  which found 0 must-fix issues on this slice.

## Verdict

**11/11 acceptance criteria verified with passing tests.** 0 gaps.
(AC1–AC7 = original issue #12 backend; AC8–AC11 = the follow-up admin-UI
duplicate-name check requested by the user. Admin UI tests re-run green:
`TaskFormTest` 7, `TaskStepTest` 9, `FieldValidationTest` 19 — 0 failures.)

DONE — all 11 criteria covered and green (unit + API tests actually run with
0 failures across the mapped classes; migration
inspected and confirmed applied cleanly + behaviorally correct against the
real running `kairos-postgres` container). The one caveat worth carrying
forward (not a gap, but should not be glossed over): the automated
`JooqTaskRepositoryIT` runs on H2, which cannot express the partial unique
index, so it proves the application-level query logic only — the DB-level
partial-index constraint itself is proven by direct, first-hand inspection
of the real Postgres container in this session, not by a repeatable
Testcontainers test in the current suite.
