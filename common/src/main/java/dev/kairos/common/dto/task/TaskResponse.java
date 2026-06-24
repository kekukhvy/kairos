package dev.kairos.common.dto.task;


import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Response body for all task endpoints. {@code deletedAt} is intentionally
 * absent — deleted tasks are never returned (callers get 404 instead).
 */
public record TaskResponse(
        UUID id,
        String service,
        String name,
        String description,
        boolean active,
        String destinationId,
        String messageType,
        JsonNode payload,
        int timeoutMs,
        boolean supportsRetry,
        Instant createdAt,
        Instant updatedAt
) {
}
