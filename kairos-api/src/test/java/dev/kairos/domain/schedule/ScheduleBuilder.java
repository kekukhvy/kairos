package dev.kairos.domain.schedule;

import dev.kairos.domain.task.TaskId;

import java.time.Instant;
import java.util.UUID;

/**
 * Test-only builder helper that produces fully valid {@link Schedule} instances
 * via the factory methods, using sensible fixed defaults. Tests override only
 * the fields they care about.
 */
final class ScheduleBuilder {

    static final ScheduleId DEFAULT_ID =
            ScheduleId.of(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"));
    static final TaskId DEFAULT_TASK_ID =
            new TaskId(UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001"));
    static final String DEFAULT_LABEL = "daily-morning";
    static final Instant FIXED_NOW = Instant.parse("2026-01-01T12:00:00Z");
    static final Instant FUTURE_RUN_AT = Instant.parse("2026-06-01T08:00:00Z");
    static final Instant PAST_RUN_AT = Instant.parse("2025-01-01T08:00:00Z");
    static final String DEFAULT_CRON = "0 8 * * MON-FRI";
    static final String DEFAULT_TIMEZONE = "Europe/Warsaw";
    static final int DEFAULT_INTERVAL_SECONDS = 3_600; // one hour

    private ScheduleBuilder() {
    }

    /** A valid ONCE schedule firing at FUTURE_RUN_AT. */
    static Schedule defaultOnce() {
        return Schedule.once(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL, FUTURE_RUN_AT, FIXED_NOW);
    }

    /** A valid CRON schedule. */
    static Schedule defaultCron() {
        return Schedule.cron(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL,
                DEFAULT_CRON, DEFAULT_TIMEZONE, FIXED_NOW);
    }

    /** A valid FIXED schedule. */
    static Schedule defaultFixed() {
        return Schedule.fixed(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL,
                DEFAULT_INTERVAL_SECONDS, FIXED_NOW);
    }
}
