package dev.kairos.admin.feature.task.dto;

/**
 * Admin-side request body for creating a task. Kept local rather than reusing
 * {@code dev.kairos.common.dto.task.CreateTaskRequest} because the common record
 * types {@code payload} as a Jackson 2 {@code JsonNode}, which the admin's
 * Jackson 3 mapper cannot serialize; a plain {@link Object} works for both.
 * Remove once {@code common} switches that field to {@code Object}. See
 * {@link TaskDto} for the full rationale.
 */
public record CreateTaskRequest(
        String service,
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