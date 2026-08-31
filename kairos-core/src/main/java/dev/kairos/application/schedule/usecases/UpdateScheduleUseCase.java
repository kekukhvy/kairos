package dev.kairos.application.schedule.usecases;

import dev.kairos.application.schedule.commands.UpdateScheduleCommand;
import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleEdit;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.schedule.ScheduleNotFoundException;
import dev.kairos.domain.schedule.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Objects;

/**
 * Updates the "when" field of an existing schedule, plus {@code label} and
 * {@code timezone}. {@code type} is immutable — to change type, delete and
 * recreate. Only the field belonging to the current type is applied.
 *
 * <p>Used by the {@code PUT /api/v1/schedules/{id}} endpoint.
 *
 * @throws ScheduleNotFoundException if no schedule exists for the id
 */
public final class UpdateScheduleUseCase {

    private static final Logger logger = LoggerFactory.getLogger(UpdateScheduleUseCase.class);

    private final ScheduleRepository scheduleRepository;
    private final Clock clock;

    public UpdateScheduleUseCase(ScheduleRepository scheduleRepository, Clock clock) {
        this.scheduleRepository = Objects.requireNonNull(scheduleRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Schedule execute(ScheduleId scheduleId, UpdateScheduleCommand command) {
        Objects.requireNonNull(scheduleId, "scheduleId must not be null");
        Objects.requireNonNull(command, "command must not be null");

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));

        ScheduleEdit edit = new ScheduleEdit(
                command.label(),
                command.runAt(),
                command.cronExpression(),
                command.intervalSeconds(),
                command.timezone());

        schedule.update(edit, clock.instant());
        scheduleRepository.save(schedule);
        logger.info("Schedule updated: id='{}'", scheduleId.value());
        return schedule;
    }
}
