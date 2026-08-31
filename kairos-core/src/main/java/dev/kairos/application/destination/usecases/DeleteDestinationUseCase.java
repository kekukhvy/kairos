package dev.kairos.application.destination.usecases;

import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.DestinationRepository;
import dev.kairos.domain.destination.exceptions.DestinationInUseException;
import dev.kairos.domain.task.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Deletes a destination.
 *
 * <p>Used by the {@code DELETE /api/v1/destinations/{id}} endpoint. Deletion
 * is idempotent: deleting an already-absent destination is not treated as an
 * error, so no existence check is performed before the delete — this mirrors
 * standard DELETE semantics and keeps the operation a single round trip.
 *
 * <p>Deletion is blocked while any task still references this destination,
 * since tasks hold a foreign key to it.
 *
 * @throws DestinationInUseException if at least one task still references
 * this destination
 */
public final class DeleteDestinationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(DeleteDestinationUseCase.class);

    private final DestinationRepository destinationRepository;
    private final TaskRepository taskRepository;

    public DeleteDestinationUseCase(DestinationRepository destinationRepository, TaskRepository taskRepository) {
        this.destinationRepository = Objects.requireNonNull(destinationRepository);
        this.taskRepository = Objects.requireNonNull(taskRepository);
    }

    public void execute(DestinationId destinationId) {
        Objects.requireNonNull(destinationId, "destinationId cannot be null!");
        validateDestinationIsNotUsed(destinationId);
        this.destinationRepository.deleteById(destinationId);
        logger.info("Destination deleted: id='{}'", destinationId.value());
    }

    private void validateDestinationIsNotUsed(DestinationId destinationId) {
        if (this.taskRepository.existsByDestinationId(destinationId)) {
            logger.warn("Destination deletion rejected — still referenced by at least one task: id='{}'", destinationId.value());
            throw new DestinationInUseException(destinationId);
        }
    }
}