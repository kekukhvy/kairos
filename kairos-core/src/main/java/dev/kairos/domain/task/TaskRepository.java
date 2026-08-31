package dev.kairos.domain.task;


import dev.kairos.domain.destination.DestinationId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistence port for {@link Task}. Implemented in the infrastructure layer
 * (JooqTaskRepository). No SQL/JDBC leaks across this boundary.
 */
public interface TaskRepository {

    /**
     * Persists the current state of the task: inserts if the {@link TaskId} is
     * new, otherwise updates the existing row. The entity is the source of truth
     * for every column, including {@code createdAt}/{@code updatedAt}.
     */
    void save(Task task);

    /**
     * Finds a task by id <b>regardless of soft-delete state</b>. The caller
     * inspects {@link Task#isDeleted()} and decides how to react (GET -> 404 on a
     * deleted task, repeated DELETE -> 409). Empty if no row exists for the id.
     */
    Optional<Task> findById(TaskId id);

    /**
     * Lists live (not soft-deleted) tasks, newest first, with pagination.
     *
     * @param limit  max number of rows to return
     * @param offset number of rows to skip
     */
    List<Task> findAll(int limit, int offset);

    /**
     * Stamps {@code deleted_at} for the given task (single-row UPDATE). The 404 /
     * 409 decision is made by the caller before invoking this; here it simply
     * performs the soft delete.
     */
    void softDelete(TaskId id, Instant deletedAt);

    /**
     * Returns {@code true} if at least one non-deleted task still references
     * the given destination. Used by {@code DeleteDestinationUseCase} to block
     * hard-deleting a destination while tasks depend on it.
     */
    boolean existsByDestinationId(DestinationId id);

    /**
     * Returns {@code true} if a <b>live</b> (not soft-deleted) task already has
     * the given {@code (service, name)} pair, other than {@code excludeId}
     * itself. {@code (service, name)} is a task's human-readable identity, so
     * this backs the uniqueness check on create and rename.
     *
     * @param excludeId the task being renamed (excluded from the match so a
     *                  no-op rename doesn't collide with itself), or
     *                  {@code null} on create, when there is no task yet to
     *                  exclude
     */
    boolean existsByServiceAndName(String service, String name, TaskId excludeId);
}


