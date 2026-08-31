package dev.kairos.application.schedule.commands;

import java.time.Instant;

/**
 * Input for updating a schedule. {@code type} is immutable, so it is absent.
 * Only the "when" field matching the schedule's current type is applied, plus
 * {@code label} and {@code timezone}.
 */
public record UpdateScheduleCommand(
        String label,
        Instant runAt,
        String cronExpression,
        Integer intervalSeconds,
        String timezone
) {
}
