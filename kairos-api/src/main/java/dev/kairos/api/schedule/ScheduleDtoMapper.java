package dev.kairos.api.schedule;

import dev.kairos.common.dto.schedule.ScheduleResponse;
import dev.kairos.domain.schedule.Schedule;

/**
 * Maps a {@link Schedule} domain aggregate to a {@link ScheduleResponse} DTO.
 */
final class ScheduleDtoMapper {

    private ScheduleDtoMapper() {
    }

    static ScheduleResponse toResponse(Schedule schedule) {
        return new ScheduleResponse(
                schedule.id().value(),
                schedule.taskId().value(),
                schedule.type().name(),
                schedule.label(),
                schedule.runAt(),
                schedule.cronExpression(),
                schedule.intervalSeconds(),
                schedule.timezone(),
                schedule.active(),
                schedule.createdAt(),
                schedule.updatedAt()
        );
    }
}
