package dev.kairos.domain.task;


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
}


