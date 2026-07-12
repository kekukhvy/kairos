package dev.kairos.application.task.usecases;

import dev.kairos.common.exceptions.ValidationException;
import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.DestinationRepository;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import dev.kairos.domain.task.TaskRepository;
import dev.kairos.application.task.commands.CreateTaskCommand;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class CreateTaskUseCase {

    private final DestinationRepository destinationRepository;
    private final TaskRepository taskRepository;
    private final Clock clock;

    public CreateTaskUseCase(DestinationRepository destinationRepository, TaskRepository taskRepository, Clock clock) {
        this.destinationRepository = Objects.requireNonNull(destinationRepository);
        this.taskRepository = Objects.requireNonNull(taskRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Task execute(CreateTaskCommand command) {

        Objects.requireNonNull(command, "command cannot be null!");

        DestinationId destinationId = DestinationId.of(command.destinationId());
        requireExistingDestination(destinationId);

        Instant now = clock.instant();
        Task.Builder builder = Task.builder()
                .id(TaskId.newId())
                .name(command.name())
                .service(command.service())
                .description(command.description())
                .destinationId(destinationId)
                .eventName(command.eventName())
                .payload(command.payload())
                .timeoutMs(command.timeoutMs())
                .createdAt(now)
                .updatedAt(now);

        if (command.active() != null) {
            builder.active(command.active());
        }

        if (command.supportsRetry() != null) {
            builder.supportsRetry(command.supportsRetry());
        }

        Task task = builder.build();
        this.taskRepository.save(task);

        return task;
    }

    private void requireExistingDestination(DestinationId destinationId) {
        if (!destinationRepository.existsById(destinationId)) {
            throw new ValidationException("Destination with id " + destinationId + " does not exist!");
        }
    }
}
