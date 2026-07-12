package dev.kairos.api;

import dev.kairos.domain.destination.Destination;
import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.DestinationRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory fake {@link DestinationRepository} for API-layer tests. Supports
 * the full repository interface so the destination handler and all its use cases
 * can run without a database.
 *
 * <p>{@code deleteById} performs a permanent hard-delete (no soft-delete),
 * mirroring the production repository contract.
 */
final class InMemoryDestinationRepositoryForApi implements DestinationRepository {

    private final Map<String, Destination> store = new LinkedHashMap<>();

    @Override
    public boolean existsById(DestinationId id) {
        return store.containsKey(id.value());
    }

    @Override
    public void save(Destination destination) {
        store.put(destination.destinationId().value(), destination);
    }

    @Override
    public Optional<Destination> findById(DestinationId id) {
        return Optional.ofNullable(store.get(id.value()));
    }

    @Override
    public List<Destination> findAll(int limit, int offset) {
        List<Destination> all = new ArrayList<>(store.values());
        int from = Math.min(offset, all.size());
        int to = Math.min(from + limit, all.size());
        return all.subList(from, to);
    }

    @Override
    public void deleteById(DestinationId id) {
        store.remove(id.value());
    }

    /** Seeds a destination directly, bypassing save(). */
    void seed(Destination destination) {
        store.put(destination.destinationId().value(), destination);
    }

    /** Returns the number of destinations currently held. */
    int size() {
        return store.size();
    }
}
