package dev.kairos.application.schedule.usecases;

import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.schedule.ScheduleType;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;

import java.time.Instant;
import java.util.UUID;

/**
 * Shared test-data factory for schedule use-case tests. All constants are
 * package-private so each test class imports only what it needs.
 */
final class UseCaseScheduleBuilder {

    static final TaskId TASK_ID =
            new TaskId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    static final ScheduleId SCHEDULE_ID =
            ScheduleId.of(UUID.fromString("11111111-0000-0000-0000-000000000001"));
    static final ScheduleId UNKNOWN_SCHEDULE_ID =
            ScheduleId.of(UUID.fromString("ffffffff-0000-0000-0000-000000000099"));

    static final Instant FIXED_NOW = Instant.parse("2026-06-01T12:00:00Z");
    static final Instant FUTURE_RUN_AT = Instant.parse("2027-01-01T08:00:00Z");
    static final Instant PAST_RUN_AT = Instant.parse("2025-01-01T08:00:00Z");

    static final String DEFAULT_LABEL = "test-label";
    static final String DEFAULT_CRON = "0 8 * * *";
    static final String DEFAULT_TIMEZONE = "UTC";
    static final int DEFAULT_INTERVAL_SECONDS = 3_600;

    private UseCaseScheduleBuilder() {
    }

    /** A live (non-deleted) task that can own schedules. */
    static Task liveTask() {
        return Task.builder()
                .id(TASK_ID)
                .service("test-service")
                .name("test-task")
                .destinationId(DestinationId.of("dest-kafka-1"))
                .eventName("test.event.v1")
                .timeoutMs(5_000)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    /** A soft-deleted task. */
    static Task deletedTask() {
        Instant deletedAt = Instant.parse("2026-02-01T00:00:00Z");
        return Task.builder()
                .id(TASK_ID)
                .service("test-service")
                .name("test-task")
                .destinationId(DestinationId.of("dest-kafka-1"))
                .eventName("test.event.v1")
                .timeoutMs(5_000)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(deletedAt)
                .deletedAt(deletedAt)
                .build();
    }

    /** A fully valid ONCE schedule with the default SCHEDULE_ID and TASK_ID. */
    static Schedule onceSchedule() {
        return Schedule.once(SCHEDULE_ID, TASK_ID, DEFAULT_LABEL, FUTURE_RUN_AT, FIXED_NOW);
    }

    /** A fully valid CRON schedule. */
    static Schedule cronSchedule() {
        return Schedule.cron(SCHEDULE_ID, TASK_ID, DEFAULT_LABEL, DEFAULT_CRON, DEFAULT_TIMEZONE, FIXED_NOW);
    }

    /** A fully valid FIXED schedule. */
    static Schedule fixedSchedule() {
        return Schedule.fixed(SCHEDULE_ID, TASK_ID, DEFAULT_LABEL, DEFAULT_INTERVAL_SECONDS, FIXED_NOW);
    }

    /** A ONCE schedule with a caller-supplied id. */
    static Schedule onceScheduleWithId(ScheduleId id) {
        return Schedule.once(id, TASK_ID, DEFAULT_LABEL, FUTURE_RUN_AT, FIXED_NOW);
    }

    /** A ONCE schedule belonging to the given task. */
    static Schedule onceScheduleForTask(TaskId taskId) {
        return Schedule.once(ScheduleId.newId(), taskId, DEFAULT_LABEL, FUTURE_RUN_AT, FIXED_NOW);
    }
}
