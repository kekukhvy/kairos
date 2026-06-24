package dev.kairos.common.dto.task;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Request body for {@code PUT /api/v1/tasks/{id}}.
 *
 * <p>PUT semantics: full replacement of the editable field set. {@code service}
 * is intentionally absent — a task's owning service is immutable.
 */
public record UpdateTaskRequest(
        String name,
        String description,
        Boolean active,
        String destinationId,
        String messageType,
        JsonNode payload,
        int timeoutMs,
        Boolean supportsRetry
) {
}