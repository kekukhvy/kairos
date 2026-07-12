package dev.kairos.api;

import dev.kairos.domain.destination.Destination;
import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.DestinationType;

import java.time.Instant;

/**
 * Test-data factory for destination API-layer tests. Produces {@link Destination}
 * domain entities with stable, well-known field values so each test overrides
 * only what it cares about.
 */
final class ApiDestinationBuilder {

    static final String DESTINATION_ID = "dest-kafka-payments";
    static final String DESTINATION_ID_2 = "dest-sqs-orders";
    static final DestinationType DESTINATION_TYPE = DestinationType.KAFKA;
    static final String CONFIG_JSON = "{\"topic\":\"payments\"}";
    static final String UPDATED_CONFIG_JSON = "{\"topic\":\"payments-v2\"}";
    static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    static final String UNKNOWN_DESTINATION_ID = "does-not-exist";

    private ApiDestinationBuilder() {
    }

    /** A fully populated destination with a predictable id. */
    static Destination destination() {
        return Destination.builder()
                .destinationId(DestinationId.of(DESTINATION_ID))
                .destinationType(DESTINATION_TYPE)
                .destinationConfig(CONFIG_JSON)
                .createdAt(CREATED_AT)
                .build();
    }

    /** A destination with a custom id. */
    static Destination destinationWithId(String id) {
        return Destination.builder()
                .destinationId(DestinationId.of(id))
                .destinationType(DESTINATION_TYPE)
                .destinationConfig(CONFIG_JSON)
                .createdAt(CREATED_AT)
                .build();
    }
}
