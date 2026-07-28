package dev.kairos.common.dto.task;


import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Response body for all task endpoints. {@code deletedAt} is intentionally
 * absent — deleted tasks are never returned (callers get 404 instead).
 *
 * <p>{@code activeScheduleCount} is a read-side projection: the number of the
 * task's schedules with {@code active = true}. It is not persisted on the
 * {@code Task} aggregate — it is assembled by the API from
 * {@code ScheduleRepository} at response time (see
 * {@code doc/specs/task-schedule-count-badge.md}).
 */
public record TaskResponse(
        UUID id,
        String service,
        String name,
        String description,
        boolean active,
        String destinationId,
        String eventName,
        JsonNode payload,
        int timeoutMs,
        boolean supportsRetry,
        Instant createdAt,
        Instant updatedAt,
        long activeScheduleCount
) {
}
