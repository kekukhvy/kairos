package dev.kairos.api.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.common.dto.task.TaskResponse;
import dev.kairos.domain.task.Task;

import static dev.kairos.common.util.helpers.JsonConverter.parseJson;

/**
 * Maps a {@link dev.kairos.domain.task.Task} domain entity to a {@link dev.kairos.common.dto.task.TaskResponse} DTO. Handles the
 * {@code payload} String ↔ {@link tools.jackson.databind.JsonNode} conversion so the response embeds
 * payload as a proper JSON object rather than an escaped string.
 *
 * <p>{@code activeScheduleCount} is not read from {@code Task} — the domain
 * aggregate carries no schedules field (aggregate boundary, see
 * {@code doc/specs/task-schedule-count-badge.md}). Callers pass it in
 * explicitly, sourced from {@code ScheduleRepository.countActiveByTaskIds}.
 */
final class TaskDtoMapper {

    private TaskDtoMapper() {
    }

    static TaskResponse toResponse(Task task, ObjectMapper objectMapper, long activeScheduleCount) {
        return new TaskResponse(
                task.id().value(),
                task.service(),
                task.name(),
                task.description(),
                task.active(),
                task.destinationId().value(),
                task.eventName(),
                parseJson(task.payload(), objectMapper),
                task.timeoutMs(),
                task.supportsRetry(),
                task.createdAt(),
                task.updatedAt(),
                activeScheduleCount
        );
    }
}
