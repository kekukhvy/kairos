package dev.kairos.api;

import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import dev.kairos.domain.task.TaskRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory fake {@link TaskRepository} for API-layer tests. Mirrors the
 * approach used in the use-case test package but lives here so API tests stay
 * independent of that package's package-private types.
 *
 * <p>{@link #softDelete} stamps the row in-place so subsequent {@link #findById}
 * calls return the deleted record (matching production SQL behavior: the repo
 * returns deleted tasks, the use case decides to 404 them).
 */
final class InMemoryTaskRepositoryForApi implements TaskRepository {

    private final Map<TaskId, Task> store = new LinkedHashMap<>();

    @Override
    public void save(Task task) {
        store.put(task.id(), task);
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Task> findAll(int limit, int offset) {
        // Mirrors the production SQL contract: deleted_at IS NULL excludes soft-deleted rows.
        List<Task> live = store.values().stream()
                .filter(t -> !t.isDeleted())
                .collect(java.util.stream.Collectors.toList());
        int from = Math.min(offset, live.size());
        int to = Math.min(from + limit, live.size());
        return live.subList(from, to);
    }

    @Override
    public void softDelete(TaskId id, Instant deletedAt) {
        // The production repository stamps deleted_at via SQL; the in-memory
        // store holds the already-mutated Task (softDelete was called on it by
        // the use case before reaching here), so no extra work is needed.
    }

    @Override
    public boolean existsByDestinationId(DestinationId id) {
        return store.values().stream()
                .anyMatch(t -> t.destinationId().equals(id));
    }

    @Override
    public boolean existsByServiceAndName(String service, String name, TaskId excludeId) {
        return store.values().stream()
                .filter(t -> !t.isDeleted())
                .filter(t -> !t.id().equals(excludeId))
                .anyMatch(t -> t.service().equals(service) && t.name().equals(name));
    }

    /** Seeds a task directly without going through save(), preserving deletedAt state. */
    void seed(Task task) {
        store.put(task.id(), task);
    }
}
