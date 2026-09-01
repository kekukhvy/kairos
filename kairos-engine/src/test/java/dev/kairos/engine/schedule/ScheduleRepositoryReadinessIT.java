package dev.kairos.engine.schedule;

import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.schedule.ScheduleRepository;
import dev.kairos.domain.task.TaskId;
import dev.kairos.infrastructure.H2DatabaseBase;
import dev.kairos.infrastructure.schedule.JooqScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the actual point of #63: {@code kairos-engine} can read schedules
 * through the {@link ScheduleRepository} <em>port</em>, backed by the real
 * {@link JooqScheduleRepository} <em>implementation</em> — now that the
 * implementation has moved out of {@code kairos-api} into
 * {@code kairos-persistence}, a module the engine already depends on.
 *
 * <p>The engine's own claim loop (M6) will read due schedules exactly this
 * way: through the port, never by hand-rolling SQL. This test seeds a row via
 * a plain jOOQ insert (bypassing the repository, since {@code save()}'s
 * upsert needs Postgres — see {@code JooqScheduleRepositoryIT}) and asserts
 * that {@link ScheduleRepository#findByTaskId} — exercised only via the
 * interface type, never the concrete class — returns it. Without the module
 * move, this test could not even compile from {@code kairos-engine}.
 */
class ScheduleRepositoryReadinessIT extends H2DatabaseBase {

    private static final String DESTINATION_ID = "engine-repo-it-dest";
    private static final String DESTINATION_TYPE = "KAFKA";
    private static final String DESTINATION_CONFIG = "{\"topic\":\"engine-repo-it\"}";

    private static final String TASK_SERVICE = "engine-repo-it-service";
    private static final String TASK_NAME = "engine-repo-it-task";
    private static final String TASK_EVENT_NAME = "engine.repo.it.task.v1";
    private static final int TASK_TIMEOUT_MS = 5_000;

    private static final String SCHEDULE_LABEL = "engine-repo-it-fixed";
    private static final int INTERVAL_SECONDS = 120;

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static final int LIMIT_TEN = 10;
    private static final int OFFSET_ZERO = 0;

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

    private TaskId seedTaskWithDestination() {
        dslContext.execute("""
                INSERT INTO destinations (id, type, config)
                VALUES (?, ?, ?)
                """, DESTINATION_ID, DESTINATION_TYPE, DESTINATION_CONFIG);

        TaskId taskId = TaskId.newId();
        dslContext.execute("""
                INSERT INTO tasks (id, service, name, destination_id, event_name, timeout_ms)
                VALUES (?, ?, ?, ?, ?, ?)
                """, taskId.value(), TASK_SERVICE, TASK_NAME, DESTINATION_ID, TASK_EVENT_NAME, TASK_TIMEOUT_MS);

        return taskId;
    }

    private void seedFixedSchedule(ScheduleId scheduleId, TaskId taskId) {
        Schedule fixedSchedule = Schedule.fixed(scheduleId, taskId, SCHEDULE_LABEL, INTERVAL_SECONDS, NOW);

        dslContext.execute("""
                INSERT INTO schedules (id, task_id, label, type, interval_seconds, timezone, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                fixedSchedule.id().value(), fixedSchedule.taskId().value(), fixedSchedule.label(),
                fixedSchedule.type().name(), fixedSchedule.intervalSeconds(), fixedSchedule.timezone(),
                toOdt(fixedSchedule.createdAt()), toOdt(fixedSchedule.updatedAt()));
    }

    private static OffsetDateTime toOdt(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
