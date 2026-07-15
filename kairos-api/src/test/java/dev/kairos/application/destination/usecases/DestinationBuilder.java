package dev.kairos.application.destination.usecases;

import dev.kairos.domain.destination.Destination;
import dev.kairos.domain.destination.DestinationId;
import dev.kairos.common.destination.DestinationType;

import java.time.Instant;

/**
 * Test-only factory that produces valid {@link Destination} instances with
 * sensible defaults. Individual tests override only the fields they care about.
 */
final class DestinationBuilder {

    static final String DEFAULT_ID = "dest-kafka-1";
    static final DestinationType DEFAULT_TYPE = DestinationType.KAFKA;
    static final String DEFAULT_CONFIG = "{\"topic\":\"payments\"}";
    static final Instant DEFAULT_CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private DestinationBuilder() {}

    static Destination.Builder defaults() {
        return Destination.builder()
                .destinationId(DestinationId.of(DEFAULT_ID))
                .destinationType(DEFAULT_TYPE)
                .destinationConfig(DEFAULT_CONFIG)
                .createdAt(DEFAULT_CREATED_AT);
    }

    static Destination buildDefault() {
        return defaults().build();
    }

    static Destination withId(String id) {
        return defaults()
                .destinationId(DestinationId.of(id))
                .build();
    }
}
