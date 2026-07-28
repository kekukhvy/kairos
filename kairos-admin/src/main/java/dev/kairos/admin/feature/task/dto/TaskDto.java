package dev.kairos.admin.feature.task.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin-side response DTO for a task. Deliberately NOT reused from
 * {@code dev.kairos.common.dto.task.TaskResponse}: the common record types
 * {@code payload} as a Jackson 2 {@code com.fasterxml.jackson.databind.JsonNode},
 * which the admin's Jackson 3 ({@code tools.jackson}) mapper cannot deserialize.
 * Here {@code payload} is a plain {@link Object} (a {@code Map}/{@code List}/
 * scalar), which both Jackson versions handle. Once {@code common} drops
 * {@code JsonNode} in favour of {@code Object}, this local copy can be removed.
 *
 * <p>{@code activeScheduleCount} mirrors {@code TaskResponse.activeScheduleCount}
 * — the number of this task's schedules with {@code active = true}, computed
 * by the API. It is a read-side projection only; it does not imply the task
 * aggregate itself carries a schedules field.
 */
public record TaskDto(
        UUID id,
        String service,
        String name,
        String description,
        boolean active,
        String destinationId,
        String eventName,
        Object payload,
        int timeoutMs,
        boolean supportsRetry,
        Instant createdAt,
        Instant updatedAt,
        long activeScheduleCount
) {

    /** Human-readable {@code service / name} label used in pickers and grids. */
    public String label() {
        return service + " / " + name;
    }
}