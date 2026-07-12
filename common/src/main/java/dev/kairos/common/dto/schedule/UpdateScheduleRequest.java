package dev.kairos.common.dto.schedule;

import java.time.Instant;

/**
 * Request body for {@code PUT /api/v1/schedules/{id}}. {@code type} is immutable
 * and therefore absent — changing type means delete + recreate. Only the "when"
 * field matching the schedule's current type is applied, plus {@code label} and
 * {@code timezone}.
 */
public record UpdateScheduleRequest(
        String label,
        Instant runAt,
        String cronExpression,
        Integer intervalSeconds,
        String timezone
) {
}
