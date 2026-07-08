package dev.kairos.api;

import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.task.TaskId;

import java.time.Instant;
import java.util.UUID;

/**
 * Test-data factory for schedule API-layer tests. Produces {@link Schedule}
 * domain entities with stable, well-known field values so each test overrides
 * only what it cares about.
 */
final class ApiScheduleBuilder {

    // TASK_UUID deliberately absent — use ApiTaskBuilder.TASK_UUID to avoid ambiguity.
    static final TaskId TASK_ID = ApiTaskBuilder.TASK_ID;

    static final UUID SCHEDULE_UUID = UUID.fromString("11111111-0000-0000-0000-000000000001");
    static final ScheduleId SCHEDULE_ID = ScheduleId.of(SCHEDULE_UUID);

    static final String UNKNOWN_TASK_UUID = "ffffffff-0000-0000-0000-000000000099";
    static final String UNKNOWN_SCHEDULE_UUID = "eeeeeeee-0000-0000-0000-000000000099";
    static final String MALFORMED_UUID = "not-a-uuid";

    static final Instant FIXED_NOW = Instant.parse("2026-01-01T12:00:00Z");
    static final Instant FUTURE_RUN_AT = Instant.parse("2027-06-01T08:00:00Z");
    static final Instant PAST_RUN_AT = Instant.parse("2025-01-01T08:00:00Z");

    static final String DEFAULT_LABEL = "api-test-label";
    static final String DEFAULT_CRON = "0 8 * * *";
    static final String DEFAULT_TIMEZONE = "UTC";
    static final int DEFAULT_INTERVAL_SECONDS = 3_600;

    private ApiScheduleBuilder() {
    }

    /** A valid active ONCE schedule with the default ids. */
    static Schedule onceSchedule() {
        return Schedule.once(SCHEDULE_ID, TASK_ID, DEFAULT_LABEL, FUTURE_RUN_AT, FIXED_NOW);
    }

    /** A valid active CRON schedule with the default ids. */
    static Schedule cronSchedule() {
        return Schedule.cron(SCHEDULE_ID, TASK_ID, DEFAULT_LABEL, DEFAULT_CRON, DEFAULT_TIMEZONE, FIXED_NOW);
    }

    /** A valid active FIXED schedule with the default ids. */
    static Schedule fixedSchedule() {
        return Schedule.fixed(SCHEDULE_ID, TASK_ID, DEFAULT_LABEL, DEFAULT_INTERVAL_SECONDS, FIXED_NOW);
    }

    /** A ONCE schedule with a random id (for list pagination tests). */
    static Schedule onceScheduleWithRandomId() {
        return Schedule.once(ScheduleId.newId(), TASK_ID, DEFAULT_LABEL, FUTURE_RUN_AT, FIXED_NOW);
    }
}
