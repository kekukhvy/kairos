package dev.kairos.application.schedule.usecases;

import dev.kairos.application.schedule.commands.CreateScheduleCommand;
import dev.kairos.common.exceptions.ValidationException;
import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.schedule.ScheduleRepository;
import dev.kairos.domain.schedule.ScheduleType;
import dev.kairos.domain.task.TaskId;
import dev.kairos.domain.task.TaskNotFoundException;
import dev.kairos.domain.task.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Creates a schedule for an existing task.
 *
 * <p>Used by the {@code POST /api/v1/tasks/{taskId}/schedules} endpoint. The
 * owning task must exist (else 404). The schedule type selects the factory
 * method, which enforces the type/field invariants; an invalid combination is
 * rejected with 400.
 *
 * @throws TaskNotFoundException if the owning task does not exist
 * @throws dev.kairos.common.exceptions.ValidationException on an invalid
 *         type/field combination
 */
public final class CreateScheduleUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreateScheduleUseCase.class);

    private final ScheduleRepository scheduleRepository;
    private final TaskRepository taskRepository;
    private final Clock clock;

    public CreateScheduleUseCase(ScheduleRepository scheduleRepository,
                                 TaskRepository taskRepository,
                                 Clock clock) {
        this.scheduleRepository = Objects.requireNonNull(scheduleRepository);
        this.taskRepository = Objects.requireNonNull(taskRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Schedule execute(CreateScheduleCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        TaskId taskId = TaskId.fromString(command.taskId());
        requireExistingTask(taskId);

        ScheduleType type = ScheduleType.parse(command.type());
        Schedule schedule = build(type, taskId, command);

        scheduleRepository.save(schedule);
        logger.info("Schedule created: id='{}', taskId='{}', type={}",
                schedule.id().value(), taskId.value(), type);
        return schedule;
    }

    private Schedule build(ScheduleType type, TaskId taskId, CreateScheduleCommand command) {
        ScheduleId id = ScheduleId.newId();
        Instant now = clock.instant();
        return switch (type) {
            case ONCE -> Schedule.once(id, taskId, command.label(), command.runAt(), now);
            case CRON -> Schedule.cron(id, taskId, command.label(),
                    command.cronExpression(), command.timezone(), now);
            case FIXED -> Schedule.fixed(id, taskId, command.label(),
                    requireIntervalPresent(command.intervalSeconds()), now);
        };
    }

    private int requireIntervalPresent(Integer intervalSeconds) {
        if (intervalSeconds == null) {
            throw new ValidationException("intervalSeconds is required for FIXED schedules");
        }
        return intervalSeconds;
    }

    private void requireExistingTask(TaskId taskId) {
        boolean missing = taskRepository.findById(taskId)
                .map(dev.kairos.domain.task.Task::isDeleted)
                .orElse(true);
        if (missing) {
            logger.warn("Schedule creation rejected — task not found: '{}'", taskId.value());
            throw new TaskNotFoundException(taskId);
        }
    }
}
