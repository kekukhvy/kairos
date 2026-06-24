package dev.kairos.infrastructure.task;

import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import dev.kairos.domain.task.TaskRepository;
import dev.kairos.infrastructure.generated.Tables;

import static dev.kairos.infrastructure.generated.tables.Tasks.*;

import dev.kairos.infrastructure.generated.tables.records.TasksRecord;
import org.jooq.DSLContext;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * jOOQ-backed implementation of {@link TaskRepository}.
 *
 * <p>All methods run in the calling thread's transaction context (or
 * auto-commit when called outside one). Transaction management belongs to the
 * application / API layer; this class has no opinion on it.
 */
public final class JooqTaskRepository implements TaskRepository {


    private final DSLContext dslContext;

    public JooqTaskRepository(DSLContext dslContext) {
        this.dslContext = Objects.requireNonNull(dslContext, "dslContext cannot be null");
    }

    /**
     * Upserts the task: INSERT on first save, UPDATE on subsequent saves.
     * The entity is the source of truth for every column except the denormalized
     * engine columns ({@code last_status}, {@code last_run_at}, {@code next_run_at}),
     * which this repository never touches.
     */
    @Override
    public void save(Task task) {
        TasksRecord record = TaskMapper.toRecord(task, dslContext.newRecord(Tables.TASKS));
        dslContext.insertInto(TASKS)
                .set(record)
                .onConflict(TASKS.ID)
                .doUpdate()
                .set(TASKS.NAME, record.getName())
                .set(TASKS.DESCRIPTION, record.getDescription())
                .set(TASKS.ACTIVE, record.getActive())
                .set(TASKS.DESTINATION_ID, record.getDestinationId())
                .set(TASKS.MESSAGE_TYPE, record.getMessageType())
                .set(TASKS.PAYLOAD, record.getPayload())
                .set(TASKS.TIMEOUT_MS, record.getTimeoutMs())
                .set(TASKS.SUPPORTS_RETRY, record.getSupportsRetry())
                .set(TASKS.UPDATED_AT, record.getUpdatedAt())
                .set(TASKS.DELETED_AT, record.getDeletedAt())
                .execute();


    }

    @Override
    public Optional<Task> findById(TaskId id) {
        return dslContext.selectFrom(TASKS)
                .where(TASKS.ID.equal(id.value()))
                .fetchOptional()
                .map(TaskMapper::toDomain);
    }

    @Override
    public List<Task> findAll(int limit, int offset) {
        return dslContext.selectFrom(TASKS)
                .where(TASKS.DELETED_AT.isNull())
                .limit(limit)
                .offset(offset)
                .fetch()
                .map(TaskMapper::toDomain);
    }

    @Override
    public void softDelete(TaskId id, Instant deletedAt) {
        dslContext.update(TASKS)
                .set(TASKS.DELETED_AT, deletedAt.atOffset(ZoneOffset.UTC))
                .where(TASKS.ID.equal(id.value()))
                .execute();
    }
}
