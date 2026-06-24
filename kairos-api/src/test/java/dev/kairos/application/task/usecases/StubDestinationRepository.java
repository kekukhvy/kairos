package dev.kairos.application.task.usecases;

import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.DestinationRepository;

import java.util.HashSet;
import java.util.Set;

/**
 * Stub {@link DestinationRepository} that lets tests control which destination
 * ids are considered to exist.
 */
final class StubDestinationRepository implements DestinationRepository {

    private final Set<String> existingIds = new HashSet<>();

    /** Register a destination id as present. */
    void register(String destinationId) {
        existingIds.add(destinationId);
    }

    @Override
    public boolean existsById(DestinationId id) {
        return existingIds.contains(id.value());
    }
}
