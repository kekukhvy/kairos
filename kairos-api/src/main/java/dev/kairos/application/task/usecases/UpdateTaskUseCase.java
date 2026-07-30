package dev.kairos.application.task.usecases;

import dev.kairos.common.exceptions.ValidationException;
import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.DestinationRepository;
import dev.kairos.domain.task.*;
import dev.kairos.application.task.commands.UpdateTaskCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Objects;

/**
 * Updates the editable fields of an existing task (PUT = full replacement of the
 * editable set). A missing or soft-deleted task is reported as not found (404);
 * the referenced destination must exist.
 */
public class UpdateTaskUseCase {

    private static final Logger logger = LoggerFactory.getLogger(UpdateTaskUseCase.class);

    private final TaskRepository taskRepository;
    private final DestinationRepository destinationRepository;
    private final Clock clock;

    public UpdateTaskUseCase(TaskRepository taskRepository, DestinationRepository destinationRepository, Clock clock) {
        this.taskRepository = Objects.requireNonNull(taskRepository);
        this.destinationRepository = Objects.requireNonNull(destinationRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Task execute(TaskId taskId, UpdateTaskCommand command) {

        Objects.requireNonNull(taskId, "Task id cannot be null.");
        Objects.requireNonNull(command, "Update task command cannot be null.");

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        if (task.isDeleted()) {
            throw new TaskNotFoundException(taskId);
        }

        DestinationId destinationId = DestinationId.of(command.destinationId());
        requireExistingDestination(destinationId);
        requireUniqueServiceAndName(task, command.name());

        TaskEdit taskEdit = new TaskEdit(
                command.name(),
                command.description(),
                command.active() == null || command.active(),
                destinationId,
                command.eventName(),
                command.payload(),
                command.timeoutMs(),
                command.supportsRetry() != null && command.supportsRetry()
        );

        task.update(taskEdit, clock.instant());

        taskRepository.save(task);

        logger.info("Task updated: id='{}', service='{}', name='{}'",
                task.id().value(), task.service(), task.name());
        return task;
    }

    private void requireExistingDestination(DestinationId destinationId) {
        if (!destinationRepository.existsById(destinationId)) {
            throw new ValidationException("Destination with id " + destinationId + " does not exist!");
        }
    }

    /**
     * {@code service} is immutable, so only a rename can collide; a same-name
     * update against the task's own row is excluded via {@code task.id()}.
     */
    private void requireUniqueServiceAndName(Task task, String newName) {
        if (taskRepository.existsByServiceAndName(task.service(), newName, task.id())) {
            logger.warn("Task update rejected — rename collides with an existing task: id='{}', service='{}', name='{}'",
                    task.id().value(), task.service(), newName);
            throw new TaskNameAlreadyExistsException(task.service(), newName);
        }
    }
}
