package dev.kairos.application.schedule.usecases;

import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import dev.kairos.domain.task.TaskRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stub {@link TaskRepository} for schedule use-case tests. Only
 * {@link #findById} is meaningful; all other methods are no-ops or return
 * empty values. Seed tasks with {@link #seed} before each test.
 */
final class StubTaskRepositoryForSchedule implements TaskRepository {

    private final Map<TaskId, Task> store = new LinkedHashMap<>();

    void seed(Task task) {
        store.put(task.id(), task);
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void save(Task task) {}

    @Override
    public List<Task> findAll(int limit, int offset) {
        return List.of();
    }

    @Override
    public void softDelete(TaskId id, Instant deletedAt) {}

    @Override
    public boolean existsByDestinationId(DestinationId id) {
        return false;
    }
}
