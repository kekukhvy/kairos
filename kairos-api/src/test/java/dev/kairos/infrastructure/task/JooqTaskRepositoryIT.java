package dev.kairos.infrastructure.task;

import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import dev.kairos.infrastructure.H2DatabaseBase;
import dev.kairos.infrastructure.generated.Tables;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * H2-backed integration tests for {@link JooqTaskRepository} (no Docker).
 *
 * <p><b>save() coverage limitation:</b> {@link JooqTaskRepository#save} renders a
 * Postgres {@code INSERT ... ON CONFLICT (id) DO UPDATE} upsert. H2 — even in
 * {@code MODE=PostgreSQL} — supports {@code ON CONFLICT DO NOTHING} but NOT
 * {@code DO UPDATE}, so the upsert cannot run here. The save()/upsert tests are
 * therefore {@link Disabled} with a pointer to run them against real Postgres;
 * the read paths (findById / findAll / softDelete) are exercised on H2 by
 * seeding rows with a plain jOOQ INSERT instead of {@code save()}.
 */
class JooqTaskRepositoryIT extends H2DatabaseBase {

    private static final String UPSERT_NEEDS_POSTGRES =
            "JooqTaskRepository.save() uses INSERT ... ON CONFLICT DO UPDATE, "
                    + "unsupported by H2; verify against real Postgres.";

    // ── fixed test-data constants ────────────────────────────────────────────

    private static final String DESTINATION_ID_VALUE = "dest-kafka-it";
    private static final String DESTINATION_TYPE = "KAFKA";
    private static final String DESTINATION_CONFIG_JSON = "{\"topic\":\"test\"}";

    private static final String SERVICE = "it-service";
    private static final String NAME = "it-task";
    private static final String DESCRIPTION = "integration test task";
    private static final String MESSAGE_TYPE = "it.task.v1";
    private static final String PAYLOAD_JSON = "{\"key\":\"value\"}";
    private static final int TIMEOUT_MS = 3_000;

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant DELETED_AT = Instant.parse("2026-03-01T00:00:00Z");

    private static final String LAST_STATUS_SUCCESS = "SUCCESS";
    private static final Instant LAST_RUN_AT = Instant.parse("2026-01-05T10:00:00Z");
    private static final Instant NEXT_RUN_AT = Instant.parse("2026-01-06T10:00:00Z");

    private static final int LIMIT_ONE = 1;
    private static final int LIMIT_TEN = 10;
    private static final int OFFSET_ZERO = 0;
    private static final int OFFSET_ONE = 1;

    // String views of the json columns (TEXT on H2) so seeds write the raw value
    // rather than going through the JSONB binding, which H2 would re-quote.
    private static final Field<String> PAYLOAD_AS_TEXT =
            DSL.field(DSL.name("payload"), SQLDataType.VARCHAR);
    private static final Field<String> CONFIG_AS_TEXT =
            DSL.field(DSL.name("config"), SQLDataType.VARCHAR);

    // ── repository under test ────────────────────────────────────────────────

    private JooqTaskRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JooqTaskRepository(dslContext);
        cleanTables();
        seedDestination(DESTINATION_ID_VALUE);
    }

    // ── save / upsert (Postgres-only: ON CONFLICT DO UPDATE) ──────────────────

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_newTask_canBeFoundById() {
        Task task = fullTask(randomTaskId());

        repository.save(task);

        assertTrue(repository.findById(task.id()).isPresent());
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_newTask_roundTripsAllScalarFields() {
        TaskId id = randomTaskId();
        Task task = fullTask(id);

        repository.save(task);

        Task loaded = repository.findById(id).orElseThrow();
        assertEquals(id, loaded.id());
        assertEquals(SERVICE, loaded.service());
        assertEquals(NAME, loaded.name());
        assertEquals(DESCRIPTION, loaded.description());
        assertTrue(loaded.active());
        assertEquals(DESTINATION_ID_VALUE, loaded.destinationId().value());
        assertEquals(MESSAGE_TYPE, loaded.messageType());
        assertEquals(TIMEOUT_MS, loaded.timeoutMs());
        assertFalse(loaded.supportsRetry());
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_newTask_payloadJsonbRoundTrips() {
        TaskId id = randomTaskId();
        repository.save(fullTask(id));

        Task loaded = repository.findById(id).orElseThrow();
        assertEquals(PAYLOAD_JSON, loaded.payload());
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_newTask_timestampsRoundTripAsUtcInstants() {
        TaskId id = randomTaskId();
        repository.save(fullTask(id));

        Task loaded = repository.findById(id).orElseThrow();
        assertEquals(CREATED_AT, loaded.createdAt());
        assertEquals(UPDATED_AT, loaded.updatedAt());
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_newTask_nullableDescriptionIsPreserved() {
        TaskId id = randomTaskId();
        repository.save(minimalTask(id));

        Task loaded = repository.findById(id).orElseThrow();
        assertNull(loaded.description());
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_newTask_nullablePayloadIsPreserved() {
        TaskId id = randomTaskId();
        repository.save(minimalTask(id));

        Task loaded = repository.findById(id).orElseThrow();
        assertNull(loaded.payload());
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_newTask_deletedAtIsNullForLiveTask() {
        TaskId id = randomTaskId();
        repository.save(fullTask(id));

        Task loaded = repository.findById(id).orElseThrow();
        assertNull(loaded.deletedAt());
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_sameIdTwice_doesNotDuplicateRow() {
        TaskId id = randomTaskId();
        repository.save(fullTask(id));
        repository.save(renamedTask(id, "updated-name"));

        int count = dslContext.fetchCount(Tables.TASKS, Tables.TASKS.ID.eq(id.value()));
        assertEquals(1, count);
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_sameIdTwice_updatesName() {
        TaskId id = randomTaskId();
        repository.save(fullTask(id));
        repository.save(renamedTask(id, "updated-name"));

        Task loaded = repository.findById(id).orElseThrow();
        assertEquals("updated-name", loaded.name());
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_doesNotOverwriteEngineColumns() {
        TaskId id = randomTaskId();
        repository.save(fullTask(id));
        seedEngineColumns(id, LAST_STATUS_SUCCESS, LAST_RUN_AT, NEXT_RUN_AT);

        repository.save(renamedTask(id, "engine-columns-test"));

        assertEngineColumnsUnchanged(id, LAST_STATUS_SUCCESS, LAST_RUN_AT, NEXT_RUN_AT);
    }

    // ── findById edge cases ──────────────────────────────────────────────────

    @Test
    void findById_missingId_returnsEmpty() {
        Optional<Task> result = repository.findById(randomTaskId());

        assertFalse(result.isPresent());
    }

    @Test
    void findById_existingTask_roundTripsAllScalarFields() {
        TaskId id = randomTaskId();
        insertTask(fullTask(id));

        Task loaded = repository.findById(id).orElseThrow();
        assertEquals(id, loaded.id());
        assertEquals(SERVICE, loaded.service());
        assertEquals(NAME, loaded.name());
        assertEquals(DESCRIPTION, loaded.description());
        assertTrue(loaded.active());
        assertEquals(DESTINATION_ID_VALUE, loaded.destinationId().value());
        assertEquals(MESSAGE_TYPE, loaded.messageType());
        assertEquals(TIMEOUT_MS, loaded.timeoutMs());
        assertFalse(loaded.supportsRetry());
    }

    @Test
    void findById_existingTask_payloadJsonbRoundTrips() {
        TaskId id = randomTaskId();
        insertTask(fullTask(id));

        Task loaded = repository.findById(id).orElseThrow();
        assertEquals(PAYLOAD_JSON, loaded.payload());
    }

    @Test
    void findById_existingTask_timestampsRoundTripAsUtcInstants() {
        TaskId id = randomTaskId();
        insertTask(fullTask(id));

        Task loaded = repository.findById(id).orElseThrow();
        assertEquals(CREATED_AT, loaded.createdAt());
        assertEquals(UPDATED_AT, loaded.updatedAt());
    }

    @Test
    void findById_taskWithoutDescriptionOrPayload_mapsNulls() {
        TaskId id = randomTaskId();
        insertTask(minimalTask(id));

        Task loaded = repository.findById(id).orElseThrow();
        assertNull(loaded.description());
        assertNull(loaded.payload());
    }

    @Test
    void findById_softDeletedTask_returnsTask() {
        TaskId id = randomTaskId();
        insertTask(fullTask(id));
        repository.softDelete(id, DELETED_AT);

        Optional<Task> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertNotNull(result.get().deletedAt());
    }

    // ── findAll pagination and soft-delete filtering ─────────────────────────

    @Test
    void findAll_excludesSoftDeletedRows() {
        TaskId liveId = randomTaskId();
        TaskId deadId = randomTaskId();
        insertTask(fullTask(liveId));
        insertTask(fullTask(deadId));
        repository.softDelete(deadId, DELETED_AT);

        List<Task> result = repository.findAll(LIMIT_TEN, OFFSET_ZERO);

        assertEquals(1, result.size());
        assertEquals(liveId, result.get(0).id());
    }

    @Test
    void findAll_respectsLimit() {
        insertTask(fullTask(randomTaskId()));
        insertTask(fullTask(randomTaskId()));

        List<Task> result = repository.findAll(LIMIT_ONE, OFFSET_ZERO);

        assertEquals(1, result.size());
    }

    @Test
    void findAll_respectsOffset() {
        insertTask(taskWithCreatedAt(randomTaskId(), Instant.parse("2026-05-01T00:00:00Z")));
        insertTask(taskWithCreatedAt(randomTaskId(), Instant.parse("2026-05-02T00:00:00Z")));

        List<Task> result = repository.findAll(LIMIT_TEN, OFFSET_ONE);

        assertEquals(1, result.size());
    }

    @Test
    void findAll_withNoLiveTasks_returnsEmptyList() {
        List<Task> result = repository.findAll(LIMIT_TEN, OFFSET_ZERO);

        assertTrue(result.isEmpty());
    }

    // ── softDelete ───────────────────────────────────────────────────────────

    @Test
    void softDelete_stampsDeletedAt() {
        TaskId id = randomTaskId();
        insertTask(fullTask(id));

        repository.softDelete(id, DELETED_AT);

        Task loaded = repository.findById(id).orElseThrow();
        assertEquals(DELETED_AT, loaded.deletedAt());
    }

    @Test
    void softDelete_taskExcludedFromFindAll() {
        TaskId id = randomTaskId();
        insertTask(fullTask(id));

        repository.softDelete(id, DELETED_AT);

        List<Task> result = repository.findAll(LIMIT_TEN, OFFSET_ZERO);
        assertTrue(result.isEmpty());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static TaskId randomTaskId() {
        return TaskId.of(UUID.randomUUID());
    }

    private static Task fullTask(TaskId id) {
        return Task.builder()
                .id(id)
                .service(SERVICE)
                .name(NAME)
                .description(DESCRIPTION)
                .active(true)
                .destinationId(DestinationId.of(DESTINATION_ID_VALUE))
                .messageType(MESSAGE_TYPE)
                .payload(PAYLOAD_JSON)
                .timeoutMs(TIMEOUT_MS)
                .supportsRetry(false)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }

    private static Task minimalTask(TaskId id) {
        return Task.builder()
                .id(id)
                .service(SERVICE)
                .name(NAME)
                .active(true)
                .destinationId(DestinationId.of(DESTINATION_ID_VALUE))
                .messageType(MESSAGE_TYPE)
                .timeoutMs(TIMEOUT_MS)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }

    private static Task renamedTask(TaskId id, String newName) {
        return Task.builder()
                .id(id)
                .service(SERVICE)
                .name(newName)
                .active(true)
                .destinationId(DestinationId.of(DESTINATION_ID_VALUE))
                .messageType(MESSAGE_TYPE)
                .timeoutMs(TIMEOUT_MS)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }

    private static Task taskWithCreatedAt(TaskId id, Instant createdAt) {
        return Task.builder()
                .id(id)
                .service(SERVICE)
                .name(NAME)
                .active(true)
                .destinationId(DestinationId.of(DESTINATION_ID_VALUE))
                .messageType(MESSAGE_TYPE)
                .timeoutMs(TIMEOUT_MS)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    /**
     * Seeds a task row with a plain INSERT (no ON CONFLICT) so the read-path
     * tests work on H2. Mirrors what {@link JooqTaskRepository#save} would write
     * for a brand-new task.
     */
    private void insertTask(Task task) {
        dslContext.insertInto(Tables.TASKS)
                .set(Tables.TASKS.ID, task.id().value())
                .set(Tables.TASKS.SERVICE, task.service())
                .set(Tables.TASKS.NAME, task.name())
                .set(Tables.TASKS.DESCRIPTION, task.description())
                .set(Tables.TASKS.ACTIVE, task.active())
                .set(Tables.TASKS.DESTINATION_ID, task.destinationId().value())
                .set(Tables.TASKS.MESSAGE_TYPE, task.messageType())
                // Write the raw String into the TEXT json column (the H2 JSONB type
                // would re-quote it); read-back via TaskMapper must match exactly.
                .set(PAYLOAD_AS_TEXT, task.payload())
                .set(Tables.TASKS.TIMEOUT_MS, task.timeoutMs())
                .set(Tables.TASKS.SUPPORTS_RETRY, task.supportsRetry())
                .set(Tables.TASKS.CREATED_AT, toOffsetDateTime(task.createdAt()))
                .set(Tables.TASKS.UPDATED_AT, toOffsetDateTime(task.updatedAt()))
                .set(Tables.TASKS.DELETED_AT, task.deletedAt() != null ? toOffsetDateTime(task.deletedAt()) : null)
                .execute();
    }

    private void cleanTables() {
        dslContext.deleteFrom(Tables.TASKS).execute();
        dslContext.deleteFrom(Tables.DESTINATIONS).execute();
    }

    private void seedDestination(String destinationId) {
        dslContext.insertInto(Tables.DESTINATIONS)
                .set(Tables.DESTINATIONS.ID, destinationId)
                .set(Tables.DESTINATIONS.TYPE, DESTINATION_TYPE)
                .set(CONFIG_AS_TEXT, DESTINATION_CONFIG_JSON)
                .execute();
    }

    private void seedEngineColumns(TaskId id, String lastStatus,
                                   Instant lastRunAt, Instant nextRunAt) {
        dslContext.update(Tables.TASKS)
                .set(Tables.TASKS.LAST_STATUS, lastStatus)
                .set(Tables.TASKS.LAST_RUN_AT, lastRunAt.atOffset(ZoneOffset.UTC))
                .set(Tables.TASKS.NEXT_RUN_AT, nextRunAt.atOffset(ZoneOffset.UTC))
                .where(Tables.TASKS.ID.eq(id.value()))
                .execute();
    }

    private void assertEngineColumnsUnchanged(TaskId id, String expectedStatus,
                                              Instant expectedLastRunAt,
                                              Instant expectedNextRunAt) {
        var record = dslContext.selectFrom(Tables.TASKS)
                .where(Tables.TASKS.ID.eq(id.value()))
                .fetchOne();
        assertNotNull(record);
        assertEquals(expectedStatus, record.getLastStatus());
        assertEquals(toOffsetDateTime(expectedLastRunAt), record.getLastRunAt());
        assertEquals(toOffsetDateTime(expectedNextRunAt), record.getNextRunAt());
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
