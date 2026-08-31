package dev.kairos.application.destination.usecases;

import dev.kairos.common.pagination.Pagination;
import dev.kairos.domain.destination.Destination;
import dev.kairos.domain.destination.DestinationRepository;

import java.util.List;
import java.util.Objects;

/**
 * Lists destinations with pagination, newest first.
 *
 * <p>Used by the {@code GET /api/v1/destinations} endpoint.
 */
public final class ListDestinationsUseCase {

    private final DestinationRepository destinationRepository;

    public ListDestinationsUseCase(DestinationRepository destinationRepository) {
        this.destinationRepository = Objects.requireNonNull(destinationRepository);
    }

    public List<Destination> execute(Pagination pagination) {
        Objects.requireNonNull(pagination, "pagination must not be null");

        return destinationRepository.findAll(pagination.limit(), pagination.offset());
    }
}