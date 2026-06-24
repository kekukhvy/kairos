package dev.kairos.api;

import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.DestinationRepository;

import java.util.HashSet;
import java.util.Set;

/**
 * Stub {@link DestinationRepository} for API-layer tests. Controls which
 * destination ids are considered to exist, mirroring the approach from the
 * use-case test package without crossing package-private boundaries.
 */
final class StubDestinationRepositoryForApi implements DestinationRepository {

    private final Set<String> existingIds = new HashSet<>();

    /** Marks a destination id as present. */
    void register(String destinationId) {
        existingIds.add(destinationId);
    }

    @Override
    public boolean existsById(DestinationId id) {
        return existingIds.contains(id.value());
    }
}
