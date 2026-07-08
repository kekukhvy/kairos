package dev.kairos.application.destination.usecases;

import dev.kairos.domain.destination.Destination;
import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.DestinationRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory fake for {@link DestinationRepository}. Stores destinations by id
 * in insertion order. {@code save()} is tracked so tests can assert call count.
 * {@code deleteById()} removes the entry (permanent hard-delete semantics,
 * matching the real repository).
 */
final class InMemoryDestinationRepository implements DestinationRepository {

    private final Map<String, Destination> store = new LinkedHashMap<>();
    private int saveCallCount = 0;

    @Override
    public boolean existsById(DestinationId id) {
        return store.containsKey(id.value());
    }

    @Override
    public void save(Destination destination) {
        saveCallCount++;
        store.put(destination.destinationId().value(), destination);
    }

    @Override
    public Optional<Destination> findById(DestinationId id) {
        return Optional.ofNullable(store.get(id.value()));
    }

    @Override
    public List<Destination> findAll(int limit, int offset) {
        lastFindAllLimit = limit;
        lastFindAllOffset = offset;
        List<Destination> all = new ArrayList<>(store.values());
        int from = Math.min(offset, all.size());
        int to = Math.min(from + limit, all.size());
        return all.subList(from, to);
    }

    @Override
    public void deleteById(DestinationId id) {
        store.remove(id.value());
    }

    // --- test helpers -------------------------------------------------------

    int saveCallCount() {
        return saveCallCount;
    }

    int lastFindAllLimit;
    int lastFindAllOffset;

    /** Seed a destination without counting it as a save(). */
    void seed(Destination destination) {
        store.put(destination.destinationId().value(), destination);
    }
}
