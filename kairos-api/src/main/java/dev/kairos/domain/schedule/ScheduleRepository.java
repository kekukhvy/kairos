package dev.kairos.domain.schedule;

import dev.kairos.domain.task.TaskId;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for {@link Schedule}. Implemented in the infrastructure
 * layer (JooqScheduleRepository). No SQL/JDBC leaks across this boundary.
 */
public interface ScheduleRepository {

    /**
     * Persists the current state of the schedule: inserts if the
     * {@link ScheduleId} is new, otherwise updates the existing row. The entity
     * is the source of truth for every column.
     */
    void save(Schedule schedule);

    /**
     * Finds a schedule by id. Empty if no row exists for the id.
     */
    Optional<Schedule> findById(ScheduleId id);

    /**
     * Lists a task's schedules, newest first, with pagination.
     *
     * @param taskId owning task
     * @param limit  max number of rows to return
     * @param offset number of rows to skip
     */
    List<Schedule> findByTaskId(TaskId taskId, int limit, int offset);

    /**
     * Deletes the schedule with the given id. Idempotent: deleting an absent
     * schedule is not an error.
     */
    void deleteById(ScheduleId id);
}
