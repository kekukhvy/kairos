package dev.kairos.admin.feature.task.dto;

import java.time.Instant;
import java.util.UUID;

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
        Instant updatedAt
) {
}