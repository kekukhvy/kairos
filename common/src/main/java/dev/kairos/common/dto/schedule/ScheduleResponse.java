package dev.kairos.common.dto.schedule;

import java.time.Instant;
import java.util.UUID;

/**
 * Response body for all schedule endpoints. Type-specific "when" fields
 * ({@code runAt}, {@code cronExpression}, {@code intervalSeconds}) are null for
 * the types they do not apply to.
 */
public record ScheduleResponse(
        UUID id,
        UUID taskId,
        String type,
        String label,
        Instant runAt,
        String cronExpression,
        Integer intervalSeconds,
        String timezone,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
