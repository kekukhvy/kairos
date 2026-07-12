package dev.kairos.application.destination.usecases;

import dev.kairos.domain.destination.Destination;
import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.exceptions.DestinationNotFoundException;
import dev.kairos.domain.destination.DestinationRepository;

import java.util.Objects;

/**
 * Fetches a single destination by id.
 *
 * <p>Used both by the {@code GET /api/v1/destinations/{id}} endpoint and,
 * internally, by other use cases (e.g. update/delete) that need to load the
 * current state of a destination before acting on it.
 */
public final class GetDestinationByIdUseCase {

    private final DestinationRepository destinationRepository;

    public GetDestinationByIdUseCase(DestinationRepository destinationRepository) {
        this.destinationRepository = Objects.requireNonNull(destinationRepository);
    }

    /**
     * @throws DestinationNotFoundException if no destination exists for the id
     */
    public Destination execute(DestinationId destinationId) {
        Objects.requireNonNull(destinationId, "destinationId must not be null");

        return destinationRepository.findById(destinationId)
                .orElseThrow(() -> new DestinationNotFoundException(destinationId));
    }
}