package dev.kairos.common.dto.task;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Request body for {@code POST /api/v1/tasks}.
 *
 * <p>{@code active} and {@code supportsRetry} are nullable — omitting them
 * applies domain defaults (active = true, supportsRetry = false).
 * {@code payload} accepts any valid JSON object/array/value; stored as JSONB.
 */
public record CreateTaskRequest(
        String service,
        String name,
        String description,
        Boolean active,
        String destinationId,
        String eventName,
        JsonNode payload,
        int timeoutMs,
        Boolean supportsRetry
) {
}