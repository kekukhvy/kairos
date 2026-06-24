package dev.kairos.infrastructure.destination;

import dev.kairos.domain.destination.DestinationId;
import dev.kairos.infrastructure.H2DatabaseBase;
import dev.kairos.infrastructure.generated.Tables;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JooqDestinationRepositoryIT extends H2DatabaseBase {

    private static final String SEEDED_DESTINATION_ID = "dest-kafka-exists";
    private static final String ABSENT_DESTINATION_ID = "dest-does-not-exist";
    private static final String DESTINATION_TYPE = "KAFKA";
    private static final String DESTINATION_CONFIG_JSON = "{\"topic\":\"test\"}";

    // String view of the config json column (TEXT on H2) — see H2DatabaseBase.
    private static final Field<String> CONFIG_AS_TEXT =
            DSL.field(DSL.name("config"), SQLDataType.VARCHAR);

    private JooqDestinationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JooqDestinationRepository(dslContext);
        cleanDestinations();
        seedDestination(SEEDED_DESTINATION_ID);
    }

    // --- happy path ---

    @Test
    void existsById_seededDestination_returnsTrue() {
        boolean result = repository.existsById(DestinationId.of(SEEDED_DESTINATION_ID));

        assertTrue(result);
    }

    // --- missing destination ---

    @Test
    void existsById_absentDestination_returnsFalse() {
        boolean result = repository.existsById(DestinationId.of(ABSENT_DESTINATION_ID));

        assertFalse(result);
    }

    // --- helpers ---

    private void cleanDestinations() {
        dslContext.deleteFrom(Tables.DESTINATIONS).execute();
    }

    private void seedDestination(String destinationId) {
        dslContext.insertInto(Tables.DESTINATIONS)
                .set(Tables.DESTINATIONS.ID, destinationId)
                .set(Tables.DESTINATIONS.TYPE, DESTINATION_TYPE)
                .set(CONFIG_AS_TEXT, DESTINATION_CONFIG_JSON)
                .execute();
    }
}
