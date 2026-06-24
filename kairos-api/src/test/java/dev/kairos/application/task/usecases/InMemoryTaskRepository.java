package dev.kairos.application.task.usecases;

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
 * In-memory fake for {@link TaskRepository}. Stores tasks by id; findAll
 * returns insertion order (tests that need ordering control the list directly).
 * save() is tracked so tests can assert it was (or was not) called.
 */
final class InMemoryTaskRepository implements TaskRepository {

    private final Map<TaskId, Task> store = new LinkedHashMap<>();
    private int saveCallCount = 0;

    @Override
    public void save(Task task) {
        saveCallCount++;
        store.put(task.id(), task);
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Task> findAll(int limit, int offset) {
        lastFindAllLimit = limit;
        lastFindAllOffset = offset;
        List<Task> all = new ArrayList<>(store.values());
        int from = Math.min(offset, all.size());
        int to = Math.min(from + limit, all.size());
        return all.subList(from, to);
    }

    @Override
    public void softDelete(TaskId id, Instant deletedAt) {
        // not used by any use case under test — intentionally left as no-op
    }

    // --- test helpers -------------------------------------------------------

    int saveCallCount() {
        return saveCallCount;
    }

    int lastFindAllLimit;
    int lastFindAllOffset;

    /** Seed a task without counting it as a save(). */
    void seed(Task task) {
        store.put(task.id(), task);
    }
}
