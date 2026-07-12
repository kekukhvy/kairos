package dev.kairos.application.task.usecases;

import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import dev.kairos.domain.task.TaskNotFoundException;
import dev.kairos.domain.task.TaskRepository;

import java.time.Clock;
import java.util.Objects;

public final class GetTaskUseCase {

    private final TaskRepository taskRepository;

    public GetTaskUseCase(TaskRepository taskRepository) {
        this.taskRepository = Objects.requireNonNull(taskRepository);
    }

    public Task execute(TaskId taskId) {

        Objects.requireNonNull(taskId, "taskId must not be null");
        Task task = this.taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (task.isDeleted()) {
            throw new TaskNotFoundException(taskId);
        }

        return task;
    }
}
