package dev.kairos.application.schedule.usecases;

import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.schedule.ScheduleNotFoundException;
import dev.kairos.domain.schedule.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Pauses or resumes a single schedule by flipping its {@code active} flag,
 * independent of the owning task's {@code active} state.
 *
 * <p>Used by the {@code PATCH /api/v1/schedules/{id}/pause} and
 * {@code .../resume} endpoints.
 *
 * @throws ScheduleNotFoundException if no schedule exists for the id
 */
public final class SetScheduleActiveUseCase {

    private static final Logger logger = LoggerFactory.getLogger(SetScheduleActiveUseCase.class);

    private final ScheduleRepository scheduleRepository;
    private final Clock clock;

    public SetScheduleActiveUseCase(ScheduleRepository scheduleRepository, Clock clock) {
        this.scheduleRepository = Objects.requireNonNull(scheduleRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public Schedule execute(ScheduleId scheduleId, boolean active) {
        Objects.requireNonNull(scheduleId, "scheduleId must not be null");

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));

        Instant now = clock.instant();
        if (active) {
            schedule.resume(now);
        } else {
            schedule.pause(now);
        }

        scheduleRepository.save(schedule);
        logger.info("Schedule {}: id='{}'", active ? "resumed" : "paused", scheduleId.value());
        return schedule;
    }
}
