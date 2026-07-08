package dev.kairos.infrastructure.destination;

import dev.kairos.domain.destination.Destination;
import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.DestinationType;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * H2-backed integration tests for {@link JooqDestinationRepository}.
 *
 * <p><b>save() / upsert coverage limitation:</b> {@link JooqDestinationRepository#save}
 * renders a Postgres {@code INSERT ... ON CONFLICT (id) DO UPDATE} upsert. H2 in
 * {@code MODE=PostgreSQL} supports {@code ON CONFLICT DO NOTHING} but NOT
 * {@code DO UPDATE}, so the upsert tests are {@link Disabled} with a pointer to
 * verify against real Postgres. Read paths (findById / findAll / deleteById /
 * existsById) are exercised on H2 by seeding rows with a plain jOOQ INSERT.
 */
class JooqDestinationRepositoryIT extends H2DatabaseBase {

    private static final String UPSERT_NEEDS_POSTGRES =
            "JooqDestinationRepository.save() uses INSERT ... ON CONFLICT DO UPDATE, "
                    + "unsupported by H2; verify against real Postgres.";

    // ── fixed test-data constants ────────────────────────────────────────────

    private static final String SEEDED_ID = "dest-kafka-exists";
    private static final String ABSENT_ID = "dest-does-not-exist";
    private static final String SECOND_ID = "dest-sqs-second";
    private static final String THIRD_ID = "dest-webhook-third";

    private static final String DESTINATION_TYPE_KAFKA = "KAFKA";
    private static final String DESTINATION_TYPE_SQS = "SQS";
    private static final DestinationType DOMAIN_TYPE_KAFKA = DestinationType.KAFKA;

    private static final String CONFIG_JSON = "{\"topic\":\"payments\"}";
    private static final String UPDATED_CONFIG_JSON = "{\"topic\":\"refunds\"}";

    private static final Instant OLDER_CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NEWER_CREATED_AT = Instant.parse("2026-06-01T00:00:00Z");

    private static final int LIMIT_ONE = 1;
    private static final int LIMIT_TEN = 10;
    private static final int OFFSET_ZERO = 0;
    private static final int OFFSET_ONE = 1;

    // H2 stores config as TEXT (not JSONB); write raw string via this typed field.
    private static final Field<String> CONFIG_AS_TEXT =
            DSL.field(DSL.name("config"), SQLDataType.VARCHAR);

    // ── repository under test ────────────────────────────────────────────────

    private JooqDestinationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JooqDestinationRepository(dslContext);
        cleanDestinations();
    }

    // ── existsById ───────────────────────────────────────────────────────────

    @Test
    void existsById_seededDestination_returnsTrue() {
        seedDestination(SEEDED_ID, DESTINATION_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT);

        assertTrue(repository.existsById(DestinationId.of(SEEDED_ID)));
    }

    @Test
    void existsById_absentDestination_returnsFalse() {
        assertFalse(repository.existsById(DestinationId.of(ABSENT_ID)));
    }

    // ── save (insert) — Postgres-only ────────────────────────────────────────

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_newDestination_canBeFoundById() {
        Destination dest = buildDestination(SEEDED_ID, DOMAIN_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT);

        repository.save(dest);

        assertTrue(repository.findById(DestinationId.of(SEEDED_ID)).isPresent());
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_newDestination_roundTripsAllFields() {
        Destination dest = buildDestination(SEEDED_ID, DOMAIN_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT);

        repository.save(dest);

        Destination loaded = repository.findById(DestinationId.of(SEEDED_ID)).orElseThrow();
        assertEquals(SEEDED_ID, loaded.destinationId().value());
        assertEquals(DOMAIN_TYPE_KAFKA, loaded.destinationType());
        assertEquals(CONFIG_JSON, loaded.config());
        assertEquals(OLDER_CREATED_AT, loaded.createdAt());
    }

    // ── save (upsert: update config but NOT createdAt or type) — Postgres-only ─

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_existingId_updatesConfig() {
        repository.save(buildDestination(SEEDED_ID, DOMAIN_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT));

        Destination updated = buildDestination(SEEDED_ID, DOMAIN_TYPE_KAFKA, UPDATED_CONFIG_JSON, OLDER_CREATED_AT);
        repository.save(updated);

        Destination loaded = repository.findById(DestinationId.of(SEEDED_ID)).orElseThrow();
        assertEquals(UPDATED_CONFIG_JSON, loaded.config());
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_existingId_doesNotDuplicateRow() {
        repository.save(buildDestination(SEEDED_ID, DOMAIN_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT));
        repository.save(buildDestination(SEEDED_ID, DOMAIN_TYPE_KAFKA, UPDATED_CONFIG_JSON, OLDER_CREATED_AT));

        int count = dslContext.fetchCount(Tables.DESTINATIONS,
                Tables.DESTINATIONS.ID.eq(SEEDED_ID));
        assertEquals(1, count);
    }

    @Test
    @Disabled(UPSERT_NEEDS_POSTGRES)
    void save_existingId_doesNotOverwriteCreatedAt() {
        repository.save(buildDestination(SEEDED_ID, DOMAIN_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT));

        // Attempt to overwrite with a different createdAt — the ON CONFLICT clause
        // must NOT update created_at (it's excluded from the DO UPDATE set).
        Destination withNewerCreatedAt = buildDestination(
                SEEDED_ID, DOMAIN_TYPE_KAFKA, UPDATED_CONFIG_JSON, NEWER_CREATED_AT);
        repository.save(withNewerCreatedAt);

        Destination loaded = repository.findById(DestinationId.of(SEEDED_ID)).orElseThrow();
        assertEquals(OLDER_CREATED_AT, loaded.createdAt());
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_seededDestination_returnsPresent() {
        seedDestination(SEEDED_ID, DESTINATION_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT);

        Optional<Destination> result = repository.findById(DestinationId.of(SEEDED_ID));

        assertTrue(result.isPresent());
    }

    @Test
    void findById_seededDestination_roundTripsId() {
        seedDestination(SEEDED_ID, DESTINATION_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT);

        Destination loaded = repository.findById(DestinationId.of(SEEDED_ID)).orElseThrow();

        assertEquals(SEEDED_ID, loaded.destinationId().value());
    }

    @Test
    void findById_seededDestination_roundTripsType() {
        seedDestination(SEEDED_ID, DESTINATION_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT);

        Destination loaded = repository.findById(DestinationId.of(SEEDED_ID)).orElseThrow();

        assertEquals(DOMAIN_TYPE_KAFKA, loaded.destinationType());
    }

    @Test
    void findById_seededDestination_roundTripsConfig() {
        seedDestination(SEEDED_ID, DESTINATION_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT);

        Destination loaded = repository.findById(DestinationId.of(SEEDED_ID)).orElseThrow();

        assertEquals(CONFIG_JSON, loaded.config());
    }

    @Test
    void findById_seededDestination_roundTripsCreatedAt() {
        seedDestination(SEEDED_ID, DESTINATION_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT);

        Destination loaded = repository.findById(DestinationId.of(SEEDED_ID)).orElseThrow();

        assertEquals(OLDER_CREATED_AT, loaded.createdAt());
    }

    @Test
    void findById_absentDestination_returnsEmpty() {
        Optional<Destination> result = repository.findById(DestinationId.of(ABSENT_ID));

        assertFalse(result.isPresent());
    }

    // ── findAll — newest first + pagination ──────────────────────────────────

    @Test
    void findAll_withNoDestinations_returnsEmptyList() {
        List<Destination> result = repository.findAll(LIMIT_TEN, OFFSET_ZERO);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAll_withOneDestination_returnsIt() {
        seedDestination(SEEDED_ID, DESTINATION_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT);

        List<Destination> result = repository.findAll(LIMIT_TEN, OFFSET_ZERO);

        assertEquals(1, result.size());
    }

    @Test
    void findAll_orderedNewestFirst_newerDestinationComesFirst() {
        seedDestination(SEEDED_ID, DESTINATION_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT);
        seedDestination(SECOND_ID, DESTINATION_TYPE_SQS, CONFIG_JSON, NEWER_CREATED_AT);

        List<Destination> result = repository.findAll(LIMIT_TEN, OFFSET_ZERO);

        assertEquals(SECOND_ID, result.get(0).destinationId().value());
        assertEquals(SEEDED_ID, result.get(1).destinationId().value());
    }

    @Test
    void findAll_respectsLimit() {
        seedDestination(SEEDED_ID, DESTINATION_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT);
        seedDestination(SECOND_ID, DESTINATION_TYPE_SQS, CONFIG_JSON, NEWER_CREATED_AT);

        List<Destination> result = repository.findAll(LIMIT_ONE, OFFSET_ZERO);

        assertEquals(1, result.size());
    }

    @Test
    void findAll_respectsOffset() {
        seedDestination(SEEDED_ID, DESTINATION_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT);
        seedDestination(SECOND_ID, DESTINATION_TYPE_SQS, CONFIG_JSON, NEWER_CREATED_AT);
        seedDestination(THIRD_ID, DESTINATION_TYPE_KAFKA, CONFIG_JSON,
                Instant.parse("2026-03-01T00:00:00Z"));

        List<Destination> result = repository.findAll(LIMIT_TEN, OFFSET_ONE);

        assertEquals(2, result.size());
    }

    // ── deleteById ───────────────────────────────────────────────────────────

    @Test
    void deleteById_seededDestination_removesRow() {
        seedDestination(SEEDED_ID, DESTINATION_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT);

        repository.deleteById(DestinationId.of(SEEDED_ID));

        assertFalse(repository.existsById(DestinationId.of(SEEDED_ID)));
    }

    @Test
    void deleteById_seededDestination_doesNotRemoveOthers() {
        seedDestination(SEEDED_ID, DESTINATION_TYPE_KAFKA, CONFIG_JSON, OLDER_CREATED_AT);
        seedDestination(SECOND_ID, DESTINATION_TYPE_SQS, CONFIG_JSON, NEWER_CREATED_AT);

        repository.deleteById(DestinationId.of(SEEDED_ID));

        assertTrue(repository.existsById(DestinationId.of(SECOND_ID)));
    }

    @Test
    void deleteById_absentDestination_doesNotThrow() {
        // Hard-delete of an absent row is a no-op — idempotent.
        repository.deleteById(DestinationId.of(ABSENT_ID));

        assertFalse(repository.existsById(DestinationId.of(ABSENT_ID)));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void cleanDestinations() {
        dslContext.deleteFrom(Tables.DESTINATIONS).execute();
    }

    private void seedDestination(String id, String type, String config, Instant createdAt) {
        dslContext.insertInto(Tables.DESTINATIONS)
                .set(Tables.DESTINATIONS.ID, id)
                .set(Tables.DESTINATIONS.TYPE, type)
                .set(CONFIG_AS_TEXT, config)
                .set(Tables.DESTINATIONS.CREATED_AT,
                        OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC))
                .execute();
    }

    private static Destination buildDestination(String id, DestinationType type,
                                                String config, Instant createdAt) {
        return Destination.builder()
                .destinationId(DestinationId.of(id))
                .destinationType(type)
                .destinationConfig(config)
                .createdAt(createdAt)
                .build();
    }
}
