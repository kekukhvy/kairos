package dev.kairos.api;

import dev.kairos.domain.destination.Destination;
import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.DestinationRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

    @Override
    public void save(Destination destination) {

    }

    @Override
    public Optional<Destination> findById(DestinationId id) {
        return Optional.empty();
    }

    @Override
    public List<Destination> findAll(int limit, int offset) {
        return List.of();
    }

    @Override
    public void deleteById(DestinationId id) {

    }
}
