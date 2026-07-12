package dev.kairos.application.schedule.usecases;

import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.schedule.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Deletes a schedule.
 *
 * <p>Used by the {@code DELETE /api/v1/schedules/{id}} endpoint. Deletion is
 * idempotent: deleting an already-absent schedule is not an error, so no
 * existence check is performed — this mirrors standard DELETE semantics and
 * keeps the operation a single round trip.
 */
public final class DeleteScheduleUseCase {

    private static final Logger logger = LoggerFactory.getLogger(DeleteScheduleUseCase.class);

    private final ScheduleRepository scheduleRepository;

    public DeleteScheduleUseCase(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = Objects.requireNonNull(scheduleRepository);
    }

    public void execute(ScheduleId scheduleId) {
        Objects.requireNonNull(scheduleId, "scheduleId must not be null");
        scheduleRepository.deleteById(scheduleId);
        logger.info("Schedule deleted: id='{}'", scheduleId.value());
    }
}
