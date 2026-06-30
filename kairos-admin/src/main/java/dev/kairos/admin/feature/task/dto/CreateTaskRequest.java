package dev.kairos.admin.feature.task.dto;

public record CreateTaskRequest(
        String service,
        String name,
        String description,
        Boolean active,
        String destinationId,
        String messageType,
        Object payload,
        int timeoutMs,
        Boolean supportsRetry
) {
}