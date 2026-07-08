package dev.kairos.application.destination.usecases;

import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import dev.kairos.domain.task.TaskRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Stub {@link TaskRepository} for destination use-case tests. Only
 * {@link #existsByDestinationId} is meaningful here; all other methods are
 * no-ops or return empty values.
 */
final class StubTaskRepository implements TaskRepository {

    private final Set<String> destinationIdsInUse = new HashSet<>();

    /** Mark a destination id as referenced by at least one task. */
    void markInUse(String destinationId) {
        destinationIdsInUse.add(destinationId);
    }

    @Override
    public boolean existsByDestinationId(DestinationId id) {
        return destinationIdsInUse.contains(id.value());
    }

    @Override
    public void save(Task task) {}

    @Override
    public Optional<Task> findById(TaskId id) {
        return Optional.empty();
    }

    @Override
    public List<Task> findAll(int limit, int offset) {
        return List.of();
    }

    @Override
    public void softDelete(TaskId id, Instant deletedAt) {}
}
