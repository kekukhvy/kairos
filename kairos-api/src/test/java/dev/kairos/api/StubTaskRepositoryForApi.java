package dev.kairos.api;

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
 * Stub {@link TaskRepository} for destination API-layer tests. Only
 * {@link #existsByDestinationId} is meaningful; all other methods are no-ops or
 * return empty values. Call {@link #markDestinationInUse} to simulate a task
 * referencing a given destination.
 */
final class StubTaskRepositoryForApi implements TaskRepository {

    private final Set<String> destinationIdsInUse = new HashSet<>();

    /** Marks a destination id as referenced by at least one task. */
    void markDestinationInUse(String destinationId) {
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

    @Override
    public boolean existsByServiceAndName(String service, String name, TaskId excludeId) {
        return false;
    }
}
