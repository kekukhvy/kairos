package dev.kairos.application.schedule.commands;

import java.time.Instant;

/**
 * Input for creating a schedule. Carries raw values (no domain value objects)
 * so the API edge maps request DTO → command trivially; the use case turns
 * these into domain types via the {@code Schedule} factory methods, which
 * validate the type/field combination.
 *
 * <p>Only the "when" field matching {@code type} is required; the others are
 * left null ({@code runAt} for {@code ONCE}, {@code cronExpression} for
 * {@code CRON}, {@code intervalSeconds} for {@code FIXED}).
 */
public record CreateScheduleCommand(
        String taskId,
        String type,
        String label,
        Instant runAt,
        String cronExpression,
        Integer intervalSeconds,
        String timezone
) {
}
