package dev.kairos.infrastructure.schedule;

import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.schedule.ScheduleRepository;
import dev.kairos.domain.task.TaskId;
import dev.kairos.infrastructure.generated.Tables;
import dev.kairos.infrastructure.generated.tables.records.SchedulesRecord;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static dev.kairos.infrastructure.generated.tables.Schedules.SCHEDULES;

/**
 * jOOQ-backed implementation of {@link ScheduleRepository}.
 *
 * <p>All methods run in the calling thread's transaction context (or
 * auto-commit when called outside one). Transaction management belongs to the
 * application / API layer; this class has no opinion on it.
 */
public final class JooqScheduleRepository implements ScheduleRepository {

    private final DSLContext dslContext;

    public JooqScheduleRepository(DSLContext dslContext) {
        this.dslContext = Objects.requireNonNull(dslContext, "dslContext cannot be null");
    }

    /**
     * Upserts the schedule: INSERT on first save, UPDATE on subsequent saves.
     * {@code type} and {@code createdAt} are never updated (both immutable).
     */
    @Override
    public void save(Schedule schedule) {
        SchedulesRecord record = ScheduleMapper.toRecord(schedule, dslContext.newRecord(Tables.SCHEDULES));
        dslContext.insertInto(SCHEDULES)
                .set(record)
                .onConflict(SCHEDULES.ID)
                .doUpdate()
                .set(SCHEDULES.LABEL, record.getLabel())
                .set(SCHEDULES.RUN_AT, record.getRunAt())
                .set(SCHEDULES.CRON_EXPRESSION, record.getCronExpression())
                .set(SCHEDULES.INTERVAL_SECONDS, record.getIntervalSeconds())
                .set(SCHEDULES.TIMEZONE, record.getTimezone())
                .set(SCHEDULES.ACTIVE, record.getActive())
                .set(SCHEDULES.UPDATED_AT, record.getUpdatedAt())
                .execute();
    }

    @Override
    public Optional<Schedule> findById(ScheduleId id) {
        return dslContext.selectFrom(SCHEDULES)
                .where(SCHEDULES.ID.equal(id.value()))
                .fetchOptional()
                .map(ScheduleMapper::toDomain);
    }

    @Override
    public List<Schedule> findByTaskId(TaskId taskId, int limit, int offset) {
        return dslContext.selectFrom(SCHEDULES)
                .where(SCHEDULES.TASK_ID.equal(taskId.value()))
                .orderBy(SCHEDULES.CREATED_AT.desc())
                .limit(limit)
                .offset(offset)
                .fetch()
                .map(ScheduleMapper::toDomain);
    }

    @Override
    public void deleteById(ScheduleId id) {
        dslContext.deleteFrom(SCHEDULES)
                .where(SCHEDULES.ID.equal(id.value()))
                .execute();
    }
}
