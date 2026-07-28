package dev.kairos.domain.schedule;

import dev.kairos.domain.task.TaskId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    /**
     * Counts each task's <em>active</em> ({@code active = true}) schedules in a
     * single query, for a batch of task ids — used to populate the task
     * listing's schedule-count read model without an N+1 fan-out. A task with
     * no active schedules (none at all, or only paused ones) is absent from the
     * returned map; callers default a missing key to {@code 0}.
     *
     * @param taskIds the tasks to count for; an empty collection short-circuits
     *                to an empty map without querying
     */
    Map<TaskId, Long> countActiveByTaskIds(Collection<TaskId> taskIds);
}
