package dev.kairos.engine.schedule;

import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.schedule.ScheduleType;
import dev.kairos.domain.task.TaskId;
import dev.kairos.infrastructure.H2DatabaseBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves two structural facts about the {@code kairos-core} extraction, from
 * inside {@code kairos-engine}:
 *
 * <ol>
 *   <li>{@code kairos-engine} can build and read {@link Schedule} /
 *       {@link ScheduleType}, moved out of {@code kairos-api} into the shared
 *       {@code kairos-core} library.</li>
 *   <li>{@link H2DatabaseBase}, published as a {@code kairos-persistence}
 *       test fixture, is consumable from another module's test source set —
 *       not just from {@code kairos-api}, where it used to live.</li>
 * </ol>
 *
 * <p>A {@code FIXED} schedule is built through the domain factory, persisted
 * with a plain jOOQ insert against the shared in-process H2 schema, and read
 * back to confirm the stored {@code type} column round-trips through
 * {@link ScheduleType#parse(String)} to the same domain value.
 */
class ScheduleTypeReadinessIT extends H2DatabaseBase {

    private static final String DESTINATION_ID = "engine-it-dest";
    private static final String DESTINATION_TYPE = "KAFKA";
    private static final String DESTINATION_CONFIG = "{\"topic\":\"engine-it\"}";

    private static final String TASK_SERVICE = "engine-it-service";
    private static final String TASK_NAME = "engine-it-task";
    private static final String TASK_EVENT_NAME = "engine.it.task.v1";
    private static final int TASK_TIMEOUT_MS = 5_000;

    private static final String SCHEDULE_LABEL = "engine-it-fixed";
    private static final int INTERVAL_SECONDS = 60;

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void cleanTables() {
        dslContext.execute("DELETE FROM schedules");
        dslContext.execute("DELETE FROM tasks");
        dslContext.execute("DELETE FROM destinations");
    }

    @Test
    void fixedScheduleBuiltFromKairosCoreRoundTripsThroughTheSharedH2Schema() {
        TaskId taskId = seedTaskWithDestination();
        Schedule fixedSchedule = Schedule.fixed(ScheduleId.newId(), taskId, SCHEDULE_LABEL, INTERVAL_SECONDS, NOW);

        insertSchedule(fixedSchedule);

        String storedType = readScheduleType(fixedSchedule.id());
        assertEquals(ScheduleType.FIXED, ScheduleType.parse(storedType));
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

    private void insertSchedule(Schedule schedule) {
        dslContext.execute("""
                INSERT INTO schedules (id, task_id, label, type, interval_seconds, timezone, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                schedule.id().value(), schedule.taskId().value(), schedule.label(), schedule.type().name(),
                schedule.intervalSeconds(), schedule.timezone(),
                OffsetDateTime.ofInstant(schedule.createdAt(), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(schedule.updatedAt(), ZoneOffset.UTC));
    }

    private String readScheduleType(ScheduleId scheduleId) {
        return dslContext.fetchOne("SELECT type FROM schedules WHERE id = ?", scheduleId.value())
                .get(0, String.class);
    }
}
