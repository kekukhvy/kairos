package dev.kairos.admin.feature.task.dto;

/**
 * Admin-side request body for updating a task. Kept local rather than reusing
 * {@code dev.kairos.common.dto.task.UpdateTaskRequest} because of the same
 * Jackson 2 {@code JsonNode} vs. Jackson 3 mismatch on {@code payload}; see
 * {@link TaskDto} for the full rationale.
 */
public record UpdateTaskRequest(
        String name,
        String description,
        Boolean active,
        String destinationId,
        String eventName,
        Object payload,
        int timeoutMs,
        Boolean supportsRetry
) {
}