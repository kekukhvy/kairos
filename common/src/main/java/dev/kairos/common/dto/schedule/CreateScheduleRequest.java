package dev.kairos.common.dto.schedule;

import java.time.Instant;

/**
 * Request body for {@code POST /api/v1/tasks/{taskId}/schedules}.
 *
 * <p>Only the "when" field matching {@code type} is required; the others must be
 * omitted (null):
 * <ul>
 *   <li>{@code ONCE} → {@code runAt} (must be in the future)</li>
 *   <li>{@code CRON} → {@code cronExpression} (+ optional {@code timezone})</li>
 *   <li>{@code FIXED} → {@code intervalSeconds} (0 &lt; n ≤ 86400)</li>
 * </ul>
 * {@code label} and {@code timezone} are optional; {@code timezone} defaults to
 * {@code UTC}.
 */
public record CreateScheduleRequest(
        String type,
        String label,
        Instant runAt,
        String cronExpression,
        Integer intervalSeconds,
        String timezone
) {
}
