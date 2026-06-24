package dev.kairos.api.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.common.dto.task.TaskResponse;
import dev.kairos.domain.task.Task;

/**
 * Maps a {@link dev.kairos.domain.task.Task} domain entity to a {@link dev.kairos.common.dto.task.TaskResponse} DTO. Handles the
 * {@code payload} String ↔ {@link tools.jackson.databind.JsonNode} conversion so the response embeds
 * payload as a proper JSON object rather than an escaped string.
 */
final class TaskDtoMapper {

    private TaskDtoMapper() {
    }

    static TaskResponse toResponse(Task task, ObjectMapper objectMapper) {
        return new TaskResponse(
                task.id().value(),
                task.service(),
                task.name(),
                task.description(),
                task.active(),
                task.destinationId().value(),
                task.messageType(),
                parsePayload(task.payload(), objectMapper),
                task.timeoutMs(),
                task.supportsRetry(),
                task.createdAt(),
                task.updatedAt()
        );
    }

    /**
     * Converts the stored JSON string back to a {@link JsonNode} for embedding in
     * the response. Falls back to a text node if the string is somehow not valid
     * JSON (guard against corrupt stored data).
     */
    private static JsonNode parsePayload(String payload, ObjectMapper objectMapper) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            return objectMapper.getNodeFactory().textNode(payload);
        }
    }
}
