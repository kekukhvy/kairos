package dev.kairos.admin.feature.task.dto;

public record UpdateTaskRequest(
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