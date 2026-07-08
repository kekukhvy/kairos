package dev.kairos.application.schedule.usecases;

import dev.kairos.common.pagination.Pagination;
import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleRepository;
import dev.kairos.domain.task.TaskId;

import java.util.List;
import java.util.Objects;

/**
 * Lists a task's schedules within the given pagination window.
 *
 * <p>Used by the {@code GET /api/v1/tasks/{taskId}/schedules} endpoint.
 */
public final class ListSchedulesByTaskUseCase {

    private final ScheduleRepository scheduleRepository;

    public ListSchedulesByTaskUseCase(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = Objects.requireNonNull(scheduleRepository);
    }

    public List<Schedule> execute(TaskId taskId, Pagination pagination) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(pagination, "pagination must not be null");
        return scheduleRepository.findByTaskId(taskId, pagination.limit(), pagination.offset());
    }
}
