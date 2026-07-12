package dev.kairos.infrastructure.schedule;

import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.schedule.ScheduleType;
import dev.kairos.domain.task.TaskId;
import dev.kairos.infrastructure.generated.tables.records.SchedulesRecord;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Converts between the jOOQ-generated {@link SchedulesRecord} and the
 * {@link Schedule} domain aggregate. Lives in the infrastructure package so the
 * domain stays free of any jOOQ types.
 */
final class ScheduleMapper {

    private ScheduleMapper() {
    }

    static Schedule toDomain(SchedulesRecord r) {
        return Schedule.builder()
                .id(ScheduleId.of(r.getId()))
                .taskId(TaskId.of(r.getTaskId()))
                .type(ScheduleType.valueOf(r.getType()))
                .label(r.getLabel())
                .runAt(toInstant(r.getRunAt()))
                .cronExpression(r.getCronExpression())
                .intervalSeconds(r.getIntervalSeconds())
                .timezone(r.getTimezone())
                .active(r.getActive())
                .createdAt(toInstant(r.getCreatedAt()))
                .updatedAt(toInstant(r.getUpdatedAt()))
                .build();
    }

    static SchedulesRecord toRecord(Schedule schedule, SchedulesRecord r) {
        r.setId(schedule.id().value());
        r.setTaskId(schedule.taskId().value());
        r.setType(schedule.type().name());
        r.setLabel(schedule.label());
        r.setRunAt(toOffsetDateTime(schedule.runAt()));
        r.setCronExpression(schedule.cronExpression());
        r.setIntervalSeconds(schedule.intervalSeconds());
        r.setTimezone(schedule.timezone());
        r.setActive(schedule.active());
        r.setCreatedAt(toOffsetDateTime(schedule.createdAt()));
        r.setUpdatedAt(toOffsetDateTime(schedule.updatedAt()));
        return r;
    }

    private static Instant toInstant(OffsetDateTime odt) {
        return odt != null ? odt.toInstant() : null;
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }
}
