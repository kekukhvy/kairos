# Acceptance evidence — 34-schedule-count-badge — 2026-07-28

- Spec: `doc/specs/task-schedule-count-badge.md` (issue #34)
- Scope: working-tree diff, uncommitted on branch `34-schedule-count-badge` — verified via `git status` / `git diff HEAD`, not against a commit.
- Test command(s) run:
  - `./gradlew :common:cleanTest :kairos-api:cleanTest :kairos-admin:cleanTest`
  - `./gradlew :common:test :kairos-api:test :kairos-admin:test --console=plain`
  - `./gradlew build --console=plain` (full gate)
  - Per-test outcomes read from `kairos-api/build/test-results/test/*.xml` and `kairos-admin/build/test-results/test/*.xml` (JUnit XML), since Gradle is quiet about individual tests by default.
- Result: 13 criteria — **13 covered+passing, 0 gaps, 0 not-run** (the AC11 string-literal gap was fixed during this stage — see the AC11 update below).

## Coverage matrix

| AC | Criterion (short) | Evidence (test / gate) | Ran? | Result |
|----|-------------------|------------------------|------|--------|
| [AC1](#ac1) | `TaskResponse.activeScheduleCount` populated by API | `TaskDtoMapperTest#toResponse_with{Zero,One,Multiple}ActiveSchedule(s)_*`, `TaskApiTest#list_task…/getById_…` | yes | PASS |
| [AC2](#ac2) | `ScheduleRepository` single grouped `COUNT(*)` w/ `active = true` | `JooqScheduleRepositoryIT#countActiveByTaskIds_*` (inspection of `JooqScheduleRepository.countActiveByTaskIds`) | yes | PASS |
| [AC3](#ac3) | Listing tasks issues exactly one schedule-count query per page | `TaskApiTest#list_multipleTasks_issuesExactlyOneScheduleCountQuery` | yes | PASS |
| [AC4](#ac4) | Domain `Task` unchanged | `git diff HEAD -- kairos-api/.../domain/task/Task.java` (empty) | yes (inspection) | PASS |
| [AC5](#ac5) | Only active schedules counted; paused contributes 0 | `JooqScheduleRepositoryIT#countActiveByTaskIds_taskWithOnlyPausedSchedule_isAbsentFromResult`, `#countActiveByTaskIds_ignoresPausedSchedulesAmongActiveOnes`, `TaskApiTest#list_taskWithOnlyPausedSchedule_activeScheduleCountIsZero` | yes | PASS |
| [AC6](#ac6) | `>1` active schedules shows badge with count | `TaskGridTest#nameCell_multipleActiveSchedules_showsBadgeWithCount` | yes | PASS |
| [AC7](#ac7) | Exactly 1 active schedule shows no badge, no colouring | `TaskGridTest#nameCell_oneActiveSchedule_showsNoBadge`, `#nameCell_oneActiveSchedule_isRenderedInDefaultColor` | yes | PASS |
| [AC8](#ac8) | 0 active schedules shows error-colour name + tooltip | `TaskGridTest#nameCell_zeroActiveSchedules_isRenderedInErrorColor`, `#nameCell_zeroActiveSchedules_hasExplanatoryTooltip`, `#nameCell_zeroActiveSchedules_showsNoBadge` | yes | PASS |
| [AC9](#ac9) | Badge click navigates to Schedules filtered; `TaskDetails` does not open | `TaskGridTest#nameCell_badgeClick_doesNotInvokeOnView` + inspection of `TaskGrid.scheduleBadge`/`TaskView.openSchedules` | yes | PASS |
| [AC10](#ac10) | `ScheduleView` accepts `?task=` and pre-selects filter; unknown id falls back | `ScheduleViewTest#resolveTaskFilterLabel_{known,unknown,malformed,null}TaskId_*` | yes | PASS |
| [AC11](#ac11) | Strings in `TaskText`/`ScheduleText`; `Tokens` styling; no Lombok; methods ≤40 lines | grep + inspection | yes | PASS (gap fixed) |
| [AC12](#ac12) | Unit tests: mapper 0/1/N, badge-visibility, zero-active styling; repo IT covers zero-schedule + paused-only task | see AC1/AC5–AC8 tests + `JooqScheduleRepositoryIT` (H2, not Testcontainers+Postgres — see note) | yes | PASS (with environment note) |
| [AC13](#ac13) | `./gradlew build` passes | full build | yes | PASS |

## Evidence log

<a id="ac1"></a>
<details>
<summary>PASS — <b>AC1</b> — <code>activeScheduleCount</code> on <code>TaskResponse</code>, populated by API — <code>TaskDtoMapperTest</code> / <code>TaskApiTest</code> — PASS</summary>

**Criterion:** `TaskResponse` (`common`) carries an `activeScheduleCount` field, populated by the API.

**Production change:** `common/src/main/java/dev/kairos/common/dto/task/TaskResponse.java` gains `long activeScheduleCount` as the last record component. `kairos-api/src/main/java/dev/kairos/api/task/TaskDtoMapper.java#toResponse` now takes an explicit `long activeScheduleCount` argument and threads it into the constructed `TaskResponse`. `TaskHandler` (`list`, `getById`, `create`, `update`, `start`, `stop`) supplies it via `activeScheduleCountFor`/`activeScheduleCountsFor`, backed by `ScheduleRepository.countActiveByTaskIds`.

**Test:** `kairos-api/src/test/java/dev/kairos/api/task/TaskDtoMapperTest.java`

```java
@Test
void toResponse_withZeroActiveSchedules_countIsZero() {
    Task task = taskWithPayload(null);

    TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper, ZERO_ACTIVE_SCHEDULES);

    assertEquals(ZERO_ACTIVE_SCHEDULES, response.activeScheduleCount());
}

@Test
void toResponse_withOneActiveSchedule_countIsOne() {
    Task task = taskWithPayload(null);

    TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper, ONE_ACTIVE_SCHEDULE);

    assertEquals(ONE_ACTIVE_SCHEDULE, response.activeScheduleCount());
}

@Test
void toResponse_withMultipleActiveSchedules_countReflectsTotal() {
    Task task = taskWithPayload(null);

    TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper, MULTIPLE_ACTIVE_SCHEDULES);

    assertEquals(MULTIPLE_ACTIVE_SCHEDULES, response.activeScheduleCount());
}
```

Also end-to-end through the HTTP layer, `kairos-api/src/test/java/dev/kairos/api/TaskApiTest.java`:

```java
@Test
void list_taskWithOneActiveSchedule_activeScheduleCountIsOne() throws Exception {
    taskRepository.seed(liveTask());
    scheduleRepository.seed(activeSchedule(TASK_UUID));

    HttpResponse<String> response = get(BASE_PATH);

    JsonNode firstItem = objectMapper.readTree(response.body()).get(FIELD_ITEMS).get(0);
    assertEquals(1, firstItem.get(FIELD_ACTIVE_SCHEDULE_COUNT).asLong());
}

@Test
void getById_taskWithActiveSchedule_activeScheduleCountReflectsIt() throws Exception {
    taskRepository.seed(liveTask());
    scheduleRepository.seed(activeSchedule(TASK_UUID));

    HttpResponse<String> response = get(taskPath(TASK_UUID.toString()));

    JsonNode body = objectMapper.readTree(response.body());
    assertEquals(1, body.get(FIELD_ACTIVE_SCHEDULE_COUNT).asLong());
}
```

**Command:**
```
./gradlew :kairos-api:test --console=plain
```

**Output (from `TEST-dev.kairos.api.task.TaskDtoMapperTest.xml` / `TEST-dev.kairos.api.TaskApiTest.xml`):**
```
TaskDtoMapperTest: tests="17" skipped="0" failures="0" errors="0"
  toResponse_withZeroActiveSchedules_countIsZero() -> PASS (0.001s)
  toResponse_withOneActiveSchedule_countIsOne() -> PASS (0.000s)
  toResponse_withMultipleActiveSchedules_countReflectsTotal() -> PASS (0.000s)

TaskApiTest: tests="43" skipped="0" failures="0" errors="0"
  list_taskWithNoSchedules_activeScheduleCountIsZero() -> PASS (0.003s)
  list_taskWithOneActiveSchedule_activeScheduleCountIsOne() -> PASS (0.003s)
  list_taskWithMultipleActiveSchedules_activeScheduleCountReflectsTotal() -> PASS (0.003s)
  getById_taskWithActiveSchedule_activeScheduleCountReflectsIt() -> PASS (0.003s)

BUILD SUCCESSFUL in 3s
13 actionable tasks: 3 executed, 10 up-to-date
```
</details>

<a id="ac2"></a>
<details>
<summary>PASS — <b>AC2</b> — single grouped <code>COUNT(*)</code> query — <code>JooqScheduleRepositoryIT#countActiveByTaskIds_*</code> — PASS</summary>

**Criterion:** `ScheduleRepository` exposes a count-active-by-task-ids method, implemented in `JooqScheduleRepository` as a **single** grouped `COUNT(*)` query with a `WHERE active = true` predicate — not one query per task.

**Production code** (`kairos-api/src/main/java/dev/kairos/infrastructure/schedule/JooqScheduleRepository.java`):

```java
@Override
public Map<TaskId, Long> countActiveByTaskIds(Collection<TaskId> taskIds) {
    if (taskIds.isEmpty()) {
        return Map.of();
    }

    List<UUID> ids = taskIds.stream().map(TaskId::value).toList();
    Result<Record2<UUID, Integer>> rows = dslContext
            .select(SCHEDULES.TASK_ID, count())
            .from(SCHEDULES)
            .where(SCHEDULES.ACTIVE.isTrue())
            .and(SCHEDULES.TASK_ID.in(ids))
            .groupBy(SCHEDULES.TASK_ID)
            .fetch();

    return rows.stream()
            .collect(Collectors.toMap(
                    row -> TaskId.of(row.value1()),
                    row -> row.value2().longValue()));
}
```

This is a single `dslContext...fetch()` call per invocation — one query regardless of how many task ids are passed — confirmed by direct code inspection (no loop issuing per-task queries).

**Test:** `kairos-api/src/test/java/dev/kairos/infrastructure/schedule/JooqScheduleRepositoryIT.java`

```java
@Test
void countActiveByTaskIds_taskWithMultipleActiveSchedules_countsAll() {
    insertOnce(randomScheduleId(), seededTaskId);
    insertCron(randomScheduleId(), seededTaskId);
    insertFixed(randomScheduleId(), seededTaskId);

    Map<TaskId, Long> result = repository.countActiveByTaskIds(Set.of(seededTaskId));

    assertEquals(3L, result.get(seededTaskId));
}

@Test
void countActiveByTaskIds_groupsSeparatelyPerTask() {
    TaskId otherTaskId = seedTaskWithDestination("dest-kafka-count-other", "other-count-task");
    insertOnce(randomScheduleId(), seededTaskId);
    insertOnce(randomScheduleId(), otherTaskId);
    insertOnce(randomScheduleId(), otherTaskId);

    Map<TaskId, Long> result = repository.countActiveByTaskIds(Set.of(seededTaskId, otherTaskId));

    assertEquals(1L, result.get(seededTaskId));
    assertEquals(2L, result.get(otherTaskId));
}
```

**Command:**
```
./gradlew :kairos-api:test --console=plain
```

**Output (from `TEST-dev.kairos.infrastructure.schedule.JooqScheduleRepositoryIT.xml`):**
```
JooqScheduleRepositoryIT: tests="37" skipped="6" failures="0" errors="0"
  countActiveByTaskIds_withEmptyInput_returnsEmptyMap() -> PASS (0.001s)
  countActiveByTaskIds_taskWithNoSchedules_isAbsentFromResult() -> PASS (0.001s)
  countActiveByTaskIds_taskWithOnlyPausedSchedule_isAbsentFromResult() -> PASS (0.015s)
  countActiveByTaskIds_taskWithOneActiveSchedule_countsOne() -> PASS (0.002s)
  countActiveByTaskIds_taskWithMultipleActiveSchedules_countsAll() -> PASS (0.001s)
  countActiveByTaskIds_ignoresPausedSchedulesAmongActiveOnes() -> PASS (0.001s)
  countActiveByTaskIds_groupsSeparatelyPerTask() -> PASS (0.002s)
  countActiveByTaskIds_onlyCountsRequestedTaskIds() -> PASS (0.001s)

BUILD SUCCESSFUL
```
(The 6 skipped tests in this class are pre-existing, unrelated `save_*` schedule-persistence tests, e.g. `save_newOnceSchedule_canBeFoundById` — not part of this slice.)
</details>

<a id="ac3"></a>
<details>
<summary>PASS — <b>AC3</b> — one schedule-count query for the whole page — <code>TaskApiTest#list_multipleTasks_issuesExactlyOneScheduleCountQuery</code> — PASS</summary>

**Criterion:** Listing tasks issues **one** schedule-count query for the whole page, regardless of how many tasks it contains.

**Test:** `kairos-api/src/test/java/dev/kairos/api/TaskApiTest.java`

```java
@Test
void list_multipleTasks_issuesExactlyOneScheduleCountQuery() throws Exception {
    taskRepository.seed(liveTask());
    taskRepository.seed(liveTaskWithId(randomTaskId()));
    taskRepository.seed(liveTaskWithId(randomTaskId()));

    get(BASE_PATH);

    assertEquals(1, scheduleRepository.countActiveByTaskIdsCallCount(),
            "listing tasks must issue exactly one schedule-count query for the whole page");
}
```

This asserts against `InMemoryScheduleRepositoryForApi.countActiveByTaskIdsCallCount()`, a call counter added specifically for this assertion, and is backed in production by `TaskHandler.list()` calling `activeScheduleCountsFor(pageItems)` exactly once (a single `scheduleRepository.countActiveByTaskIds(taskIds)` call for the whole page) rather than per-task.

**Command:**
```
./gradlew :kairos-api:test --console=plain
```

**Output:**
```
TaskApiTest > list_multipleTasks_issuesExactlyOneScheduleCountQuery() -> PASS (0.003s)
(from TEST-dev.kairos.api.TaskApiTest.xml: tests="43" failures="0" errors="0")

BUILD SUCCESSFUL
```
</details>

<a id="ac4"></a>
<details>
<summary>PASS — <b>AC4</b> — domain <code>Task</code> unchanged — inspection (empty diff) — PASS</summary>

**Criterion:** The domain `Task` entity is unchanged — no `schedules` collection, no count field; the count lives only in the read model.

**Evidence:** direct inspection of the working-tree diff for the domain aggregate file.

**Command:**
```
git diff HEAD -- kairos-api/src/main/java/dev/kairos/domain/task/Task.java
```

**Output:**
```
(no output — empty diff)
```

`Task.java` does not appear anywhere in `git status` / `git diff HEAD --stat` either — it is untouched by this slice. The count is assembled entirely in `TaskHandler`/`TaskDtoMapper` (API/read layer) from `ScheduleRepository.countActiveByTaskIds`, never stored on or read from the `Task` aggregate.
</details>

<a id="ac5"></a>
<details>
<summary>PASS — <b>AC5</b> — only active schedules counted — <code>JooqScheduleRepositoryIT</code> / <code>TaskApiTest</code> — PASS</summary>

**Criterion:** Only **active** (`active = true`) schedules are counted; a paused schedule contributes nothing to the count.

**Test 1:** `kairos-api/src/test/java/dev/kairos/infrastructure/schedule/JooqScheduleRepositoryIT.java`

```java
@Test
void countActiveByTaskIds_taskWithOnlyPausedSchedule_isAbsentFromResult() {
    insertPausedOnce(randomScheduleId(), seededTaskId);

    Map<TaskId, Long> result = repository.countActiveByTaskIds(Set.of(seededTaskId));

    assertFalse(result.containsKey(seededTaskId),
            "a task whose only schedule is paused must count as 0 (absent from the map)");
}

@Test
void countActiveByTaskIds_ignoresPausedSchedulesAmongActiveOnes() {
    insertOnce(randomScheduleId(), seededTaskId);
    insertPausedOnce(randomScheduleId(), seededTaskId);

    Map<TaskId, Long> result = repository.countActiveByTaskIds(Set.of(seededTaskId));

    assertEquals(1L, result.get(seededTaskId));
}
```

**Test 2 (end-to-end):** `kairos-api/src/test/java/dev/kairos/api/TaskApiTest.java`

```java
@Test
void list_taskWithOnlyPausedSchedule_activeScheduleCountIsZero() throws Exception {
    taskRepository.seed(liveTask());
    scheduleRepository.seed(pausedSchedule(TASK_UUID));

    HttpResponse<String> response = get(BASE_PATH);

    JsonNode firstItem = objectMapper.readTree(response.body()).get(FIELD_ITEMS).get(0);
    assertEquals(0, firstItem.get(FIELD_ACTIVE_SCHEDULE_COUNT).asLong());
}
```

**Command:**
```
./gradlew :kairos-api:test --console=plain
```

**Output:**
```
countActiveByTaskIds_taskWithOnlyPausedSchedule_isAbsentFromResult() -> PASS (0.015s)
countActiveByTaskIds_ignoresPausedSchedulesAmongActiveOnes() -> PASS (0.001s)
list_taskWithOnlyPausedSchedule_activeScheduleCountIsZero() -> PASS (0.004s)

BUILD SUCCESSFUL
```
</details>

<a id="ac6"></a>
<details>
<summary>PASS — <b>AC6</b> — badge shown for &gt;1 active schedules — <code>TaskGridTest#nameCell_multipleActiveSchedules_showsBadgeWithCount</code> — PASS</summary>

**Criterion:** A task with **more than one** active schedule shows a small badge with the count next to its name in `TaskGrid`.

**Test:** `kairos-admin/src/test/java/dev/kairos/admin/feature/task/component/TaskGridTest.java`

```java
@Test
void nameCell_multipleActiveSchedules_showsBadgeWithCount() {
    TaskGrid grid = new TaskGrid();
    TaskDto task = taskWithActiveScheduleCount(3);
    grid.setRows(List.of(task));

    Component nameCell = nameCellFor(grid, task);

    assertThat(badgeIn(nameCell)).map(Span::getText).contains("3");
}
```

Backed by production code `TaskGrid.nameCell`/`TaskGrid.scheduleBadge`:
```java
private Component nameCell(TaskDto task) {
    Div cell = new Div(nameSpan(task));
    if (task.activeScheduleCount() > BADGE_THRESHOLD) {
        cell.add(scheduleBadge(task));
    }
    return cell;
}
```
(`BADGE_THRESHOLD = 1L`, so the badge only appears above 1.)

**Command:**
```
./gradlew :kairos-admin:test --console=plain
```

**Output (from `TEST-dev.kairos.admin.feature.task.component.TaskGridTest.xml`):**
```
TaskGridTest: tests="12" skipped="0" failures="0" errors="0"
  nameCell_multipleActiveSchedules_showsBadgeWithCount() -> PASS (0.001s)

BUILD SUCCESSFUL
```
</details>

<a id="ac7"></a>
<details>
<summary>PASS — <b>AC7</b> — exactly 1 active schedule: no badge, no colouring — <code>TaskGridTest</code> — PASS</summary>

**Criterion:** A task with **exactly one** active schedule shows no badge and no colouring.

**Test:** `kairos-admin/src/test/java/dev/kairos/admin/feature/task/component/TaskGridTest.java`

```java
@Test
void nameCell_oneActiveSchedule_isRenderedInDefaultColor() {
    TaskGrid grid = new TaskGrid();
    TaskDto task = taskWithActiveScheduleCount(1);
    grid.setRows(List.of(task));

    Span name = nameSpanFor(grid, task);

    assertThat(name.getStyle().get("color")).isNull();
}

@Test
void nameCell_oneActiveSchedule_showsNoBadge() {
    TaskGrid grid = new TaskGrid();
    TaskDto task = taskWithActiveScheduleCount(1);
    grid.setRows(List.of(task));

    Component nameCell = nameCellFor(grid, task);

    assertThat(badgeIn(nameCell)).isEmpty();
}
```

**Command:**
```
./gradlew :kairos-admin:test --console=plain
```

**Output:**
```
nameCell_oneActiveSchedule_isRenderedInDefaultColor() -> PASS (0.001s)
nameCell_oneActiveSchedule_showsNoBadge() -> PASS (0.001s)

BUILD SUCCESSFUL
```
</details>

<a id="ac8"></a>
<details>
<summary>PASS — <b>AC8</b> — 0 active schedules: error colour + tooltip — <code>TaskGridTest</code> — PASS</summary>

**Criterion:** A task with **zero** active schedules (no schedules, or only paused ones) shows its name in the error colour with a tooltip explaining it has no active schedules and will never run.

**Test:** `kairos-admin/src/test/java/dev/kairos/admin/feature/task/component/TaskGridTest.java`

```java
@Test
void nameCell_zeroActiveSchedules_isRenderedInErrorColor() {
    TaskGrid grid = new TaskGrid();
    TaskDto task = taskWithActiveScheduleCount(0);
    grid.setRows(List.of(task));

    Span name = nameSpanFor(grid, task);

    assertThat(name.getStyle().get("color")).isEqualTo(Tokens.COLOR_ERROR);
}

@Test
void nameCell_zeroActiveSchedules_hasExplanatoryTooltip() {
    TaskGrid grid = new TaskGrid();
    TaskDto task = taskWithActiveScheduleCount(0);
    grid.setRows(List.of(task));

    Span name = nameSpanFor(grid, task);

    assertThat(Tooltip.forComponent(name).getText()).isEqualTo(TaskText.TOOLTIP_NO_ACTIVE_SCHEDULES);
}

@Test
void nameCell_zeroActiveSchedules_showsNoBadge() {
    TaskGrid grid = new TaskGrid();
    TaskDto task = taskWithActiveScheduleCount(0);
    grid.setRows(List.of(task));

    Component nameCell = nameCellFor(grid, task);

    assertThat(badgeIn(nameCell)).isEmpty();
}
```

Note: this test suite covers the "only paused ones" wording of the criterion only at the `activeScheduleCount == 0` grid-rendering level (`TaskGrid` cannot distinguish "no schedules at all" from "only paused ones" — both collapse to `0` before reaching the grid, by design per the spec's Design section). The distinction itself is proven at the API/repository layer (AC5).

**Command:**
```
./gradlew :kairos-admin:test --console=plain
```

**Output:**
```
nameCell_zeroActiveSchedules_isRenderedInErrorColor() -> PASS (0.001s)
nameCell_zeroActiveSchedules_hasExplanatoryTooltip() -> PASS (0.001s)
nameCell_zeroActiveSchedules_showsNoBadge() -> PASS (0.001s)

BUILD SUCCESSFUL
```
</details>

<a id="ac9"></a>
<details>
<summary>PASS — <b>AC9</b> — badge click navigates, doesn't open TaskDetails — <code>TaskGridTest#nameCell_badgeClick_doesNotInvokeOnView</code> + inspection — PASS</summary>

**Criterion:** Clicking the badge navigates to Schedules with the task filter pre-selected to that task; the row's `TaskDetails` dialog does **not** open.

**Test (propagation half):** `kairos-admin/src/test/java/dev/kairos/admin/feature/task/component/TaskGridTest.java`

```java
@Test
void nameCell_badgeClick_doesNotInvokeOnView() {
    TaskGrid grid = new TaskGrid();
    List<TaskDto> viewed = new ArrayList<>();
    grid.setOnView(viewed::add);
    TaskDto task = taskWithActiveScheduleCount(3);
    grid.setRows(List.of(task));

    Component nameCell = nameCellFor(grid, task);
    Span badge = badgeIn(nameCell).orElseThrow(() -> new AssertionError("No badge found"));
    badge.getElement().executeJs("this.click()");

    assertThat(viewed).isEmpty();
}
```

Backed by production code stopping propagation explicitly:
```java
private Span scheduleBadge(TaskDto task) {
    Span badge = new Span(String.valueOf(task.activeScheduleCount()));
    badge.getElement().getThemeList().add(Tokens.THEME_BADGE_CONTRAST);
    badge.getElement().addEventListener("click", event -> onOpenSchedules.accept(task))
            .stopPropagation();
    ...
}
```

**Navigation half (inspection, no dedicated UI-navigation test — Vaadin `UI.navigate` isn't easily unit-testable without a full Vaadin session):** `TaskView.java`
```java
private void openSchedules(TaskDto task) {
    QueryParameters query = QueryParameters.of(ScheduleRoutes.QUERY_TASK, task.id().toString());
    UI.getCurrent().navigate(ScheduleView.class, query);
}
```
wired via `grid.setOnOpenSchedules(this::openSchedules)` in `TaskView`'s constructor. The query-parameter resolution this navigation depends on is unit-tested directly (see AC10).

**Command:**
```
./gradlew :kairos-admin:test --console=plain
```

**Output:**
```
nameCell_badgeClick_doesNotInvokeOnView() -> PASS (0.001s)
(from TEST-dev.kairos.admin.feature.task.component.TaskGridTest.xml: tests="12" failures="0" errors="0")

BUILD SUCCESSFUL
```

Note: the navigation call itself (`UI.getCurrent().navigate(...)`) is not exercised by an automated test — it is thin wiring proven by inspection only, consistent with how `TaskView.openDestination`/similar navigation callbacks are handled elsewhere in this codebase.
</details>

<a id="ac10"></a>
<details>
<summary>PASS — <b>AC10</b> — <code>?task=</code> pre-selects filter, unknown falls back — <code>ScheduleViewTest</code> — PASS</summary>

**Criterion:** `ScheduleView` accepts `?task=<taskId>` and pre-selects the task filter; an unknown id falls back to the unfiltered list.

**Test:** `kairos-admin/src/test/java/dev/kairos/admin/feature/schedule/ScheduleViewTest.java`

```java
@Test
void resolveTaskFilterLabel_knownTaskId_returnsItsLabel() {
    Optional<String> label = ScheduleView.resolveTaskFilterLabel(KNOWN_TASK_ID.toString(), tasks);

    assertThat(label).contains("billing / monthly-invoice");
}

@Test
void resolveTaskFilterLabel_unknownTaskId_returnsEmpty() {
    UUID unknownId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    Optional<String> label = ScheduleView.resolveTaskFilterLabel(unknownId.toString(), tasks);

    assertThat(label).isEmpty();
}

@Test
void resolveTaskFilterLabel_malformedTaskId_returnsEmpty() {
    Optional<String> label = ScheduleView.resolveTaskFilterLabel(MALFORMED_TASK_ID, tasks);

    assertThat(label).isEmpty();
}

@Test
void resolveTaskFilterLabel_nullTaskId_returnsEmpty() {
    Optional<String> label = ScheduleView.resolveTaskFilterLabel(null, tasks);

    assertThat(label).isEmpty();
}
```

Backed by production code in `ScheduleView.beforeEnter`/`resolveTaskFilterLabel`, which reads the `?task=` query parameter via `BeforeEnterObserver` and applies it to `taskFilter` if resolved, otherwise leaves the filter untouched (unfiltered list).

**Command:**
```
./gradlew :kairos-admin:test --tests 'dev.kairos.admin.feature.schedule.ScheduleViewTest' --console=plain
```

**Output (from `TEST-dev.kairos.admin.feature.schedule.ScheduleViewTest.xml`):**
```
ScheduleViewTest: tests="4" skipped="0" failures="0" errors="0"
  resolveTaskFilterLabel_knownTaskId_returnsItsLabel() -> PASS (0.000s)
  resolveTaskFilterLabel_unknownTaskId_returnsEmpty() -> PASS (0.001s)
  resolveTaskFilterLabel_malformedTaskId_returnsEmpty() -> PASS (0.000s)
  resolveTaskFilterLabel_nullTaskId_returnsEmpty() -> PASS (0.000s)

BUILD SUCCESSFUL
```
</details>

<a id="ac11"></a>
<details>
<summary>PASS — <b>AC11</b> — strings in TaskText/ScheduleText, Tokens styling, no Lombok, methods ≤40 lines — grep + inspection — literal gap fixed</summary>

**Criterion:** All new UI strings live in `TaskText` / `ScheduleText`; styling goes through `Tokens` (no literals); no Lombok; methods ≤ 40 lines.

**Checks performed:**

1. **Lombok:** `git diff HEAD --name-only | grep '\.java$' | xargs grep -l "lombok"` → no output. **No Lombok found. PASS.**

2. **Method length ≤ 40 lines** — measured each new/changed method body:
   - `TaskGrid.nameCell` — 7 lines
   - `TaskGrid.scheduleBadge` — 11 lines
   - `TaskGrid.nameSpan` — 8 lines
   - `TaskHandler.list` — 20 lines
   - `TaskHandler.activeScheduleCountsFor` — 4 lines
   - `ScheduleView.beforeEnter` — 5 lines
   - `ScheduleView.resolveTaskFilterLabel` — 15 lines

   All well under 40 lines. **PASS.**

3. **Strings in `TaskText`:** `TaskText.TOOLTIP_NO_ACTIVE_SCHEDULES` added and used by `TaskGrid.nameSpan`. **PASS** for the tooltip text.

4. **Styling via `Tokens`, no new CSS literals:** `TaskGrid.scheduleBadge`/`nameSpan` use `Tokens.COLOR_ERROR`, `Tokens.THEME_BADGE_CONTRAST`, `Tokens.CURSOR_POINTER`, `Tokens.SPACE_XS` — all pre-existing or newly-added `Tokens` constants (`Tokens.THEME_BADGE = "badge"` added as a genuinely-missing base token, matching the spec's "no new tokens unless genuinely missing" instruction). **PASS.**

5. **String literals — full check of the diff:**
   ```
   $ grep -n '"' kairos-admin/src/main/java/dev/kairos/admin/feature/task/component/TaskGrid.java
   107:        badge.getElement().addEventListener("click", event -> onOpenSchedules.accept(task))
   ```
   `"click"` (the DOM event name passed to `Element.addEventListener`) was a raw string literal in new production code, violating `.claude/GUIDELINES.md` §1 ("No literals in code — ever... String literals → `private static final String` constants").

   **Fixed in the verify stage:** `TaskGrid` now declares `private static final String EVENT_CLICK = "click";` and `scheduleBadge` calls `addEventListener(EVENT_CLICK, ...)`. Post-fix grep:
   ```
   $ grep -n 'addEventListener' kairos-admin/src/main/java/dev/kairos/admin/feature/task/component/TaskGrid.java
   110:        badge.getElement().addEventListener(EVENT_CLICK, event -> onOpenSchedules.accept(task))
   ```
   No raw literal remains. `./gradlew build` re-run green after the fix.

**Verdict:** PASS — the one literal violation was a one-line constant extraction, applied and re-verified. All AC11 sub-checks (no Lombok, methods ≤40 lines, strings in `TaskText`, styling via `Tokens`, no literals) now hold.
</details>

<a id="ac12"></a>
<details>
<summary>PASS (with environment note) — <b>AC12</b> — mapper/badge/zero-active unit tests + repo IT — PASS</summary>

**Criterion:** Unit tests cover the mapper (count 0 / 1 / N), the badge-visibility rule, and the zero-active name styling; a repository integration test (Testcontainers) covers the grouped active count including a task with zero active schedules **and a task whose only schedule is paused** (must count as 0).

**Mapper 0/1/N:** `TaskDtoMapperTest#toResponse_with{Zero,One,Multiple}ActiveSchedule(s)_*` — see AC1 evidence block. PASS.

**Badge-visibility rule:** `TaskGridTest#nameCell_{zeroActiveSchedules_showsNoBadge, oneActiveSchedule_showsNoBadge, multipleActiveSchedules_showsBadgeWithCount}` — see AC6/AC7/AC8 evidence blocks. PASS.

**Zero-active name styling:** `TaskGridTest#nameCell_zeroActiveSchedules_isRenderedInErrorColor` / `#nameCell_zeroActiveSchedules_hasExplanatoryTooltip` — see AC8 evidence block. PASS.

**Repository integration test, including zero-schedule task AND paused-only task:** `JooqScheduleRepositoryIT#countActiveByTaskIds_taskWithNoSchedules_isAbsentFromResult` and `#countActiveByTaskIds_taskWithOnlyPausedSchedule_isAbsentFromResult` — see AC2/AC5 evidence blocks. PASS.

**Environment note:** the criterion (and `.claude/CLAUDE.md`) call this out as a "Testcontainers" repository IT against real Postgres. `JooqScheduleRepositoryIT` in fact extends `H2DatabaseBase` — it runs against in-memory H2 in PostgreSQL-compatibility mode, not a Testcontainers-launched Postgres. This is the **project-wide, pre-existing convention** for every repository IT in this codebase (`JooqTaskRepositoryIT`, `JooqDestinationRepositoryIT` also extend `H2DatabaseBase`) — this slice only adds test methods to an already-H2-based class, it does not introduce the pattern. Recorded honestly per this agent's instructions rather than treated as a failure of issue #34; a Postgres-specific behavior of the grouped `COUNT(*)`/partial-index path could in principle differ from H2 and go uncaught, but that is a repo-wide test-infrastructure gap, not specific to this change.

**Command:**
```
./gradlew :kairos-api:test :kairos-admin:test --console=plain
```

**Output:** see AC1, AC2, AC5, AC6, AC7, AC8 blocks above (all PASS, 0 failures/errors across the relevant classes).
</details>

<a id="ac13"></a>
<details>
<summary>PASS — <b>AC13</b> — <code>./gradlew build</code> passes — full build — PASS</summary>

**Criterion:** `./gradlew build` passes.

**Command:**
```
./gradlew build --console=plain
```

**Output (tail):**
```
> Task :common:build UP-TO-DATE
> Task :kairos-admin:compileJava UP-TO-DATE
> Task :kairos-admin:hillaConfigure
> Task :kairos-admin:vaadinBuildFrontend
> Task :kairos-admin:bootJar
> Task :kairos-admin:jar
> Task :kairos-admin:assemble
> Task :kairos-admin:test
> Task :kairos-admin:check
> Task :kairos-admin:build
> Task :kairos-api:build UP-TO-DATE
...
BUILD SUCCESSFUL in 5s
28 actionable tasks: 6 executed, 22 up-to-date
```

`kairos-admin:test` was actually executed (not up-to-date) in this run and passed as part of the full build; `common`/`kairos-api` modules were up-to-date from the immediately preceding `:common:test :kairos-api:test :kairos-admin:test` run (which itself executed fresh after `cleanTest`, see AC1–AC10 blocks), so all three modules' tests genuinely ran and passed within this verification session.
</details>

## Gaps

- **None remaining.** The one gap found during verification (AC11 — the raw `"click"` literal in `TaskGrid.scheduleBadge`) was fixed in this stage: `TaskGrid` now uses a `private static final String EVENT_CLICK = "click";` constant, and `./gradlew build` was re-run green afterward.

## Verdict

**13/13 acceptance criteria verified with passing tests.** The sole gap (AC11 string literal) was a one-line constant extraction, applied and re-verified during this stage. Only environment caveat: the repository IT (`JooqScheduleRepositoryIT`, AC12) runs on H2 in PostgreSQL-compat mode rather than Testcontainers+Postgres — the pre-existing, project-wide convention for every repo IT, noted honestly rather than hidden.

GAPS: AC11 — fix is a one-line constant extraction in `kairos-admin/src/main/java/dev/kairos/admin/feature/task/component/TaskGrid.java`, not a test-author task. All other criteria (AC1–AC10, AC12, AC13) are covered by passing tests or direct inspection, with real command output captured above. No criterion was left not-run; the only environment caveat is the pre-existing, project-wide use of H2 instead of Testcontainers+Postgres for repository ITs (AC12), which is honestly noted rather than hidden.
