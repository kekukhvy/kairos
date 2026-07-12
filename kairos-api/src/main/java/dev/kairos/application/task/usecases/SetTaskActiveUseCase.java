package dev.kairos.application.task.usecases;

import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import dev.kairos.domain.task.TaskNotFoundException;
import dev.kairos.domain.task.TaskRepository;

import java.time.Clock;
import java.util.Objects;

public class SetTaskActiveUseCase {

    private final TaskRepository taskRepository;
    private final Clock clock;

    public SetTaskActiveUseCase(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = Objects.requireNonNull(taskRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Task execute(TaskId taskId, boolean active) {
        Objects.requireNonNull(taskId, "Task id cannot be null.");

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        task.setActive(active, clock.instant());

        taskRepository.save(task);

        return task;
    }
}
