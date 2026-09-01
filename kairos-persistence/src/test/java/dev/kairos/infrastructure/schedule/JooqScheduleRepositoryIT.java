package dev.kairos.infrastructure.schedule;

import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.schedule.ScheduleType;
import dev.kairos.domain.task.TaskId;
import dev.kairos.infrastructure.H2DatabaseBase;
import dev.kairos.infrastructure.generated.Tables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H2-backed integration tests for {@link JooqScheduleRepository}.
 *
 * <p><b>save() coverage limitation:</b> {@link JooqScheduleRepository#save} renders a
 * Postgres {@code INSERT ... ON CONFLICT (id) DO UPDATE} upsert. H2 — even in
 * {@code MODE=PostgreSQL} — supports {@code ON CONFLICT DO NOTHING} but NOT
 * {@code DO UPDATE}, so the upsert tests are {@link Disabled}. The read paths
 * (findById / findByTaskId / deleteById) are exercised by seeding rows with a
 * plain jOOQ INSERT instead of {@code save()}.
 *
 * <p>Prerequisites: each test seeds a destination + task first, because
 * {@code schedules.task_id} has a FK to {@code tasks(id) ON DELETE CASCADE}.
 */
class JooqScheduleRepositoryIT extends H2DatabaseBase {

    private static final String UPSERT_NEEDS_POSTGRES =
            "JooqScheduleRepository.save() uses INSERT ... ON CONFLICT DO UPDATE, "
                    + "unsupported by H2; verify against real Postgres.";

    // ── fixed test-data constants ────────────────────────────────────────────

    private static final String DESTINATION_ID = "dest-kafka-it";
    private static final String DESTINATION_TYPE = "KAFKA";
    private static final String DESTINATION_CONFIG = "{\"topic\":\"test\"}";

    private static final String TASK_SERVICE = "it-service";
    private static final String TASK_NAME = "it-task";
    private static final String TASK_EVENT_NAME = "it.task.v1";
    private static final int TASK_TIMEOUT_MS = 3_000;

    private static final String DEFAULT_LABEL = "it-label";
    private static final String DEFAULT_CRON = "0 8 * * *";
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final int DEFAULT_INTERVAL_SECONDS = 3_600;

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant FUTURE_RUN_AT = Instant.parse("2027-06-01T08:00:00Z");

    private static final int LIMIT_TEN = 10;
    private static final int LIMIT_ONE = 1;
    private static final int OFFSET_ZERO = 0;
    private static final int OFFSET_ONE = 1;

    // ── repository under test ────────────────────────────────────────────────

    private JooqScheduleRepository repository;
    private TaskId seededTaskId;

    @BeforeEach
    void setUp() {
        repository = new JooqScheduleRepository(dslContext);
        cleanTables();
        seededTaskId = seedTaskWithDestination();
    }

    // ── save / upsert — Postgres-only ────────────────────────────────────────

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_newOnceSchedule_canBeFoundById() {
        Schedule s = onceSchedule(randomScheduleId(), seededTaskId);

        repository.save(s);

        assertTrue(repository.findById(s.id()).isPresent());
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_newOnceSchedule_roundTripsAllFields() {
        ScheduleId id = randomScheduleId();
        Schedule s = onceSchedule(id, seededTaskId);

        repository.save(s);

        Schedule loaded = repository.findById(id).orElseThrow();
        assertEquals(id, loaded.id());
        assertEquals(seededTaskId, loaded.taskId());
        assertEquals(ScheduleType.ONCE, loaded.type());
        assertEquals(DEFAULT_LABEL, loaded.label());
        assertEquals(FUTURE_RUN_AT, loaded.runAt());
        assertNull(loaded.cronExpression());
        assertNull(loaded.intervalSeconds());
        assertEquals(DEFAULT_TIMEZONE, loaded.timezone());
        assertTrue(loaded.active());
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_newCronSchedule_roundTripsAllFields() {
        ScheduleId id = randomScheduleId();
        Schedule s = cronSchedule(id, seededTaskId);

        repository.save(s);

        Schedule loaded = repository.findById(id).orElseThrow();
        assertEquals(ScheduleType.CRON, loaded.type());
        assertEquals(DEFAULT_CRON, loaded.cronExpression());
        assertNull(loaded.runAt());
        assertNull(loaded.intervalSeconds());
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_newFixedSchedule_roundTripsAllFields() {
        ScheduleId id = randomScheduleId();
        Schedule s = fixedSchedule(id, seededTaskId);

        repository.save(s);

        Schedule loaded = repository.findById(id).orElseThrow();
        assertEquals(ScheduleType.FIXED, loaded.type());
        assertEquals(DEFAULT_INTERVAL_SECONDS, loaded.intervalSeconds());
        assertNull(loaded.runAt());
        assertNull(loaded.cronExpression());
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_sameIdTwice_doesNotDuplicateRow() {
        ScheduleId id = randomScheduleId();
        repository.save(onceSchedule(id, seededTaskId));
        repository.save(onceSchedule(id, seededTaskId));

        int count = dslContext.fetchCount(Tables.SCHEDULES, Tables.SCHEDULES.ID.eq(id.value()));
        assertEquals(1, count);
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_updatesLabel_onConflict() {
        ScheduleId id = randomScheduleId();
        repository.save(onceSchedule(id, seededTaskId));

        Schedule updated = Schedule.builder()
                .id(id)
                .taskId(seededTaskId)
                .type(ScheduleType.ONCE)
                .label("updated-label")
                .runAt(FUTURE_RUN_AT)
                .timezone(DEFAULT_TIMEZONE)
                .active(true)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
        repository.save(updated);

        Schedule loaded = repository.findById(id).orElseThrow();
        assertEquals("updated-label", loaded.label());
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_missingId_returnsEmpty() {
        Optional<Schedule> result = repository.findById(randomScheduleId());

        assertFalse(result.isPresent());
    }

    @Test
    void findById_seededOnceSchedule_returnsPresent() {
        ScheduleId id = randomScheduleId();
        insertOnce(id, seededTaskId);

        assertTrue(repository.findById(id).isPresent());
    }

    @Test
    void findById_seededOnceSchedule_roundTripsId() {
        ScheduleId id = randomScheduleId();
        insertOnce(id, seededTaskId);

        Schedule loaded = repository.findById(id).orElseThrow();

        assertEquals(id, loaded.id());
    }

    @Test
    void findById_seededOnceSchedule_roundTripsTaskId() {
        ScheduleId id = randomScheduleId();
        insertOnce(id, seededTaskId);

        Schedule loaded = repository.findById(id).orElseThrow();

        assertEquals(seededTaskId, loaded.taskId());
    }

    @Test
    void findById_seededOnceSchedule_roundTripsType() {
        ScheduleId id = randomScheduleId();
        insertOnce(id, seededTaskId);

        Schedule loaded = repository.findById(id).orElseThrow();

        assertEquals(ScheduleType.ONCE, loaded.type());
    }

    @Test
    void findById_seededOnceSchedule_roundTripsLabel() {
        ScheduleId id = randomScheduleId();
        insertOnce(id, seededTaskId);

        Schedule loaded = repository.findById(id).orElseThrow();

        assertEquals(DEFAULT_LABEL, loaded.label());
    }

    @Test
    void findById_seededOnceSchedule_roundTripsRunAt() {
        ScheduleId id = randomScheduleId();
        insertOnce(id, seededTaskId);

        Schedule loaded = repository.findById(id).orElseThrow();

        assertEquals(FUTURE_RUN_AT, loaded.runAt());
    }

    @Test
    void findById_seededOnceSchedule_cronExpressionIsNull() {
        ScheduleId id = randomScheduleId();
        insertOnce(id, seededTaskId);

        Schedule loaded = repository.findById(id).orElseThrow();

        assertNull(loaded.cronExpression());
    }

    @Test
    void findById_seededOnceSchedule_intervalSecondsIsNull() {
        ScheduleId id = randomScheduleId();
        insertOnce(id, seededTaskId);

        Schedule loaded = repository.findById(id).orElseThrow();

        assertNull(loaded.intervalSeconds());
    }

    @Test
    void findById_seededOnceSchedule_roundTripsTimestamps() {
        ScheduleId id = randomScheduleId();
        insertOnce(id, seededTaskId);

        Schedule loaded = repository.findById(id).orElseThrow();

        assertEquals(CREATED_AT, loaded.createdAt());
        assertEquals(UPDATED_AT, loaded.updatedAt());
    }

    @Test
    void findById_seededCronSchedule_roundTripsCronExpression() {
        ScheduleId id = randomScheduleId();
        insertCron(id, seededTaskId);

        Schedule loaded = repository.findById(id).orElseThrow();

        assertEquals(ScheduleType.CRON, loaded.type());
        assertEquals(DEFAULT_CRON, loaded.cronExpression());
        assertNull(loaded.runAt());
        assertNull(loaded.intervalSeconds());
    }

    @Test
    void findById_seededFixedSchedule_roundTripsIntervalSeconds() {
        ScheduleId id = randomScheduleId();
        insertFixed(id, seededTaskId);

        Schedule loaded = repository.findById(id).orElseThrow();

        assertEquals(ScheduleType.FIXED, loaded.type());
        assertEquals(DEFAULT_INTERVAL_SECONDS, loaded.intervalSeconds());
        assertNull(loaded.runAt());
        assertNull(loaded.cronExpression());
    }

    @Test
    void findById_seededSchedule_activeIsTrue() {
        ScheduleId id = randomScheduleId();
        insertOnce(id, seededTaskId);

        Schedule loaded = repository.findById(id).orElseThrow();

        assertTrue(loaded.active());
    }

    @Test
    void findById_nullLabelIsPreserved() {
        ScheduleId id = randomScheduleId();
        insertOnceWithNullLabel(id, seededTaskId);

        Schedule loaded = repository.findById(id).orElseThrow();

        assertNull(loaded.label());
    }

    // ── findByTaskId ─────────────────────────────────────────────────────────

    @Test
    void findByTaskId_withNoSchedules_returnsEmptyList() {
        List<Schedule> result = repository.findByTaskId(seededTaskId, LIMIT_TEN, OFFSET_ZERO);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByTaskId_withOneSchedule_returnsOneItem() {
        insertOnce(randomScheduleId(), seededTaskId);

        List<Schedule> result = repository.findByTaskId(seededTaskId, LIMIT_TEN, OFFSET_ZERO);

        assertEquals(1, result.size());
    }

    @Test
    void findByTaskId_filtersSchedulesByTaskId() {
        TaskId otherTaskId = seedTaskWithDestination("dest-kafka-other", "other-task");
        insertOnce(randomScheduleId(), seededTaskId);
        insertOnce(randomScheduleId(), otherTaskId);

        List<Schedule> result = repository.findByTaskId(seededTaskId, LIMIT_TEN, OFFSET_ZERO);

        assertEquals(1, result.size());
        assertEquals(seededTaskId, result.get(0).taskId());
    }

    @Test
    void findByTaskId_respectsLimit() {
        insertOnce(randomScheduleId(), seededTaskId);
        insertOnce(randomScheduleId(), seededTaskId);

        List<Schedule> result = repository.findByTaskId(seededTaskId, LIMIT_ONE, OFFSET_ZERO);

        assertEquals(1, result.size());
    }

    @Test
    void findByTaskId_respectsOffset() {
        insertOnce(randomScheduleId(), seededTaskId);
        insertOnce(randomScheduleId(), seededTaskId);

        List<Schedule> result = repository.findByTaskId(seededTaskId, LIMIT_TEN, OFFSET_ONE);

        assertEquals(1, result.size());
    }

    // ── deleteById ───────────────────────────────────────────────────────────

    @Test
    void deleteById_existingSchedule_removesRow() {
        ScheduleId id = randomScheduleId();
        insertOnce(id, seededTaskId);

        repository.deleteById(id);

        assertFalse(repository.findById(id).isPresent());
    }

    @Test
    void deleteById_doesNotRemoveOtherSchedules() {
        ScheduleId id1 = randomScheduleId();
        ScheduleId id2 = randomScheduleId();
        insertOnce(id1, seededTaskId);
        insertOnce(id2, seededTaskId);

        repository.deleteById(id1);

        assertTrue(repository.findById(id2).isPresent());
    }

    @Test
    void deleteById_absentSchedule_doesNotThrow() {
        assertDoesNotThrow(() -> repository.deleteById(randomScheduleId()));
    }

    // ── countActiveByTaskIds ─────────────────────────────────────────────────

    @Test
    void countActiveByTaskIds_withEmptyInput_returnsEmptyMap() {
        Map<TaskId, Long> result = repository.countActiveByTaskIds(Set.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void countActiveByTaskIds_taskWithNoSchedules_isAbsentFromResult() {
        Map<TaskId, Long> result = repository.countActiveByTaskIds(Set.of(seededTaskId));

        assertFalse(result.containsKey(seededTaskId));
    }

    @Test
    void countActiveByTaskIds_taskWithOnlyPausedSchedule_isAbsentFromResult() {
        insertPausedOnce(randomScheduleId(), seededTaskId);

        Map<TaskId, Long> result = repository.countActiveByTaskIds(Set.of(seededTaskId));

        assertFalse(result.containsKey(seededTaskId),
                "a task whose only schedule is paused must count as 0 (absent from the map)");
    }

    @Test
    void countActiveByTaskIds_taskWithOneActiveSchedule_countsOne() {
        insertOnce(randomScheduleId(), seededTaskId);

        Map<TaskId, Long> result = repository.countActiveByTaskIds(Set.of(seededTaskId));

        assertEquals(1L, result.get(seededTaskId));
    }

    @Test
    void countActiveByTaskIds_taskWithMultipleActiveSchedules_countsAll() {
        insertOnce(randomScheduleId(), seededTaskId);
        insertCron(randomScheduleId(), seededTaskId);
        insertFixed(randomScheduleId(), seededTaskId);

        Map<TaskId, Long> result = repository.countActiveByTaskIds(Set.of(seededTaskId));

        assertEquals(3L, result.get(seededTaskId));
    }

    @Test
    void countActiveByTaskIds_ignoresPausedSchedulesAmongActiveOnes() {
        insertOnce(randomScheduleId(), seededTaskId);
        insertPausedOnce(randomScheduleId(), seededTaskId);

        Map<TaskId, Long> result = repository.countActiveByTaskIds(Set.of(seededTaskId));

        assertEquals(1L, result.get(seededTaskId));
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

    @Test
    void countActiveByTaskIds_onlyCountsRequestedTaskIds() {
        TaskId otherTaskId = seedTaskWithDestination("dest-kafka-count-excluded", "excluded-count-task");
        insertOnce(randomScheduleId(), seededTaskId);
        insertOnce(randomScheduleId(), otherTaskId);

        Map<TaskId, Long> result = repository.countActiveByTaskIds(Set.of(seededTaskId));

        assertFalse(result.containsKey(otherTaskId));
    }

    // ── cascade on task delete ────────────────────────────────────────────────

    @Test
    void schedules_areCascadeDeletedWhenTaskIsDeleted() {
        ScheduleId scheduleId = randomScheduleId();
        insertOnce(scheduleId, seededTaskId);

        dslContext.deleteFrom(Tables.TASKS)
                .where(Tables.TASKS.ID.eq(seededTaskId.value()))
                .execute();

        assertFalse(repository.findById(scheduleId).isPresent());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static ScheduleId randomScheduleId() {
        return ScheduleId.of(UUID.randomUUID());
    }

    private static OffsetDateTime toOdt(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private void cleanTables() {
        dslContext.deleteFrom(Tables.SCHEDULES).execute();
        dslContext.deleteFrom(Tables.TASKS).execute();
        dslContext.deleteFrom(Tables.DESTINATIONS).execute();
    }

    /** Seeds one destination + one task; returns the new TaskId. */
    private TaskId seedTaskWithDestination() {
        return seedTaskWithDestination(DESTINATION_ID, TASK_NAME);
    }

    private TaskId seedTaskWithDestination(String destinationId, String taskName) {
        // Plain INSERT — cleanTables() guarantees no duplicate destinations per test.
        dslContext.insertInto(Tables.DESTINATIONS)
                .set(Tables.DESTINATIONS.ID, destinationId)
                .set(Tables.DESTINATIONS.TYPE, DESTINATION_TYPE)
                // Use inline DSL field cast to TEXT so H2 doesn't re-quote via JSONB binding.
                .set(org.jooq.impl.DSL.field(
                        org.jooq.impl.DSL.name("config"), org.jooq.impl.SQLDataType.VARCHAR),
                        DESTINATION_CONFIG)
                .execute();

        UUID taskUuid = UUID.randomUUID();
        dslContext.insertInto(Tables.TASKS)
                .set(Tables.TASKS.ID, taskUuid)
                .set(Tables.TASKS.SERVICE, TASK_SERVICE)
                .set(Tables.TASKS.NAME, taskName)
                .set(Tables.TASKS.ACTIVE, true)
                .set(Tables.TASKS.DESTINATION_ID, destinationId)
                .set(Tables.TASKS.EVENT_NAME, TASK_EVENT_NAME)
                .set(Tables.TASKS.TIMEOUT_MS, TASK_TIMEOUT_MS)
                .set(Tables.TASKS.SUPPORTS_RETRY, false)
                .set(Tables.TASKS.CREATED_AT, toOdt(CREATED_AT))
                .set(Tables.TASKS.UPDATED_AT, toOdt(UPDATED_AT))
                .execute();
        return TaskId.of(taskUuid);
    }

    private void insertOnce(ScheduleId id, TaskId taskId) {
        dslContext.insertInto(Tables.SCHEDULES)
                .set(Tables.SCHEDULES.ID, id.value())
                .set(Tables.SCHEDULES.TASK_ID, taskId.value())
                .set(Tables.SCHEDULES.LABEL, DEFAULT_LABEL)
                .set(Tables.SCHEDULES.TYPE, "ONCE")
                .set(Tables.SCHEDULES.RUN_AT, toOdt(FUTURE_RUN_AT))
                .set(Tables.SCHEDULES.TIMEZONE, DEFAULT_TIMEZONE)
                .set(Tables.SCHEDULES.ACTIVE, true)
                .set(Tables.SCHEDULES.CREATED_AT, toOdt(CREATED_AT))
                .set(Tables.SCHEDULES.UPDATED_AT, toOdt(UPDATED_AT))
                .execute();
    }

    private void insertPausedOnce(ScheduleId id, TaskId taskId) {
        dslContext.insertInto(Tables.SCHEDULES)
                .set(Tables.SCHEDULES.ID, id.value())
                .set(Tables.SCHEDULES.TASK_ID, taskId.value())
                .set(Tables.SCHEDULES.LABEL, DEFAULT_LABEL)
                .set(Tables.SCHEDULES.TYPE, "ONCE")
                .set(Tables.SCHEDULES.RUN_AT, toOdt(FUTURE_RUN_AT))
                .set(Tables.SCHEDULES.TIMEZONE, DEFAULT_TIMEZONE)
                .set(Tables.SCHEDULES.ACTIVE, false)
                .set(Tables.SCHEDULES.CREATED_AT, toOdt(CREATED_AT))
                .set(Tables.SCHEDULES.UPDATED_AT, toOdt(UPDATED_AT))
                .execute();
    }

    private void insertOnceWithNullLabel(ScheduleId id, TaskId taskId) {
        dslContext.insertInto(Tables.SCHEDULES)
                .set(Tables.SCHEDULES.ID, id.value())
                .set(Tables.SCHEDULES.TASK_ID, taskId.value())
                .set(Tables.SCHEDULES.TYPE, "ONCE")
                .set(Tables.SCHEDULES.RUN_AT, toOdt(FUTURE_RUN_AT))
                .set(Tables.SCHEDULES.TIMEZONE, DEFAULT_TIMEZONE)
                .set(Tables.SCHEDULES.ACTIVE, true)
                .set(Tables.SCHEDULES.CREATED_AT, toOdt(CREATED_AT))
                .set(Tables.SCHEDULES.UPDATED_AT, toOdt(UPDATED_AT))
                .execute();
    }

    private void insertCron(ScheduleId id, TaskId taskId) {
        dslContext.insertInto(Tables.SCHEDULES)
                .set(Tables.SCHEDULES.ID, id.value())
                .set(Tables.SCHEDULES.TASK_ID, taskId.value())
                .set(Tables.SCHEDULES.LABEL, DEFAULT_LABEL)
                .set(Tables.SCHEDULES.TYPE, "CRON")
                .set(Tables.SCHEDULES.CRON_EXPRESSION, DEFAULT_CRON)
                .set(Tables.SCHEDULES.TIMEZONE, DEFAULT_TIMEZONE)
                .set(Tables.SCHEDULES.ACTIVE, true)
                .set(Tables.SCHEDULES.CREATED_AT, toOdt(CREATED_AT))
                .set(Tables.SCHEDULES.UPDATED_AT, toOdt(UPDATED_AT))
                .execute();
    }

    private void insertFixed(ScheduleId id, TaskId taskId) {
        dslContext.insertInto(Tables.SCHEDULES)
                .set(Tables.SCHEDULES.ID, id.value())
                .set(Tables.SCHEDULES.TASK_ID, taskId.value())
                .set(Tables.SCHEDULES.LABEL, DEFAULT_LABEL)
                .set(Tables.SCHEDULES.TYPE, "FIXED")
                .set(Tables.SCHEDULES.INTERVAL_SECONDS, DEFAULT_INTERVAL_SECONDS)
                .set(Tables.SCHEDULES.TIMEZONE, DEFAULT_TIMEZONE)
                .set(Tables.SCHEDULES.ACTIVE, true)
                .set(Tables.SCHEDULES.CREATED_AT, toOdt(CREATED_AT))
                .set(Tables.SCHEDULES.UPDATED_AT, toOdt(UPDATED_AT))
                .execute();
    }

    // Domain builders for @Disabled save() tests (not used in H2 read-path tests).

    private static Schedule onceSchedule(ScheduleId id, TaskId taskId) {
        return Schedule.builder()
                .id(id).taskId(taskId).type(ScheduleType.ONCE)
                .label(DEFAULT_LABEL).runAt(FUTURE_RUN_AT)
                .timezone(DEFAULT_TIMEZONE).active(true)
                .createdAt(CREATED_AT).updatedAt(UPDATED_AT)
                .build();
    }

    private static Schedule cronSchedule(ScheduleId id, TaskId taskId) {
        return Schedule.builder()
                .id(id).taskId(taskId).type(ScheduleType.CRON)
                .label(DEFAULT_LABEL).cronExpression(DEFAULT_CRON)
                .timezone(DEFAULT_TIMEZONE).active(true)
                .createdAt(CREATED_AT).updatedAt(UPDATED_AT)
                .build();
    }

    private static Schedule fixedSchedule(ScheduleId id, TaskId taskId) {
        return Schedule.builder()
                .id(id).taskId(taskId).type(ScheduleType.FIXED)
                .label(DEFAULT_LABEL).intervalSeconds(DEFAULT_INTERVAL_SECONDS)
                .timezone(DEFAULT_TIMEZONE).active(true)
                .createdAt(CREATED_AT).updatedAt(UPDATED_AT)
                .build();
    }
}
