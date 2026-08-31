package dev.kairos.infrastructure.schedule;

import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.schedule.ScheduleRepository;
import dev.kairos.domain.task.TaskId;
import dev.kairos.infrastructure.generated.Tables;
import dev.kairos.infrastructure.generated.tables.records.SchedulesRecord;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.kairos.infrastructure.generated.tables.Schedules.SCHEDULES;
import static org.jooq.impl.DSL.count;

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

    /**
     * Counts active schedules per task in one grouped query — the whole page's
     * count is fetched in a single round trip, never one query per task.
     */
    @Override
    public Map<TaskId, Long> countActiveByTaskIds(Collection<TaskId> taskIds) {
        if (taskIds.isEmpty()) {
            return Map.of();
        }

        List<UUID> ids = taskIds.stream().map(TaskId::value).toList();
        Result<Record2<UUID, Integer>> rows = dslContext
                .select(SCHEDULES.TASK_ID, count())
                .from(SCHEDULES)
                .where(SCHEDULES.ACTIVE.isTrue())
                .and(SCHEDULES.TASK_ID.in(ids))
                .groupBy(SCHEDULES.TASK_ID)
                .fetch();

        return rows.stream()
                .collect(Collectors.toMap(
                        row -> TaskId.of(row.value1()),
                        row -> row.value2().longValue()));
    }
}
