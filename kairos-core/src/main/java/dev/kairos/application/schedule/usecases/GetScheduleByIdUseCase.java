package dev.kairos.application.schedule.usecases;

import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.schedule.ScheduleNotFoundException;
import dev.kairos.domain.schedule.ScheduleRepository;

import java.util.Objects;

/**
 * Fetches a single schedule by id.
 *
 * <p>Used by the {@code GET /api/v1/schedules/{id}} endpoint and, internally,
 * by the update/pause/resume use cases to load current state before acting.
 */
public final class GetScheduleByIdUseCase {

    private final ScheduleRepository scheduleRepository;

    public GetScheduleByIdUseCase(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = Objects.requireNonNull(scheduleRepository);
    }

    /**
     * @throws ScheduleNotFoundException if no schedule exists for the id
     */
    public Schedule execute(ScheduleId scheduleId) {
        Objects.requireNonNull(scheduleId, "scheduleId must not be null");
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
    }
}
