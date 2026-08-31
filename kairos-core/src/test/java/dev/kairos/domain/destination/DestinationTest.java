package dev.kairos.domain.destination;

import dev.kairos.common.destination.DestinationType;
import dev.kairos.common.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DestinationTest {

    private static final DestinationId ID_A = DestinationId.of("dest-kafka-a");
    private static final DestinationId ID_B = DestinationId.of("dest-kafka-b");
    private static final DestinationType TYPE = DestinationType.KAFKA;
    private static final String CONFIG = "{\"topic\":\"payments\"}";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final String UPDATED_CONFIG = "{\"topic\":\"refunds\"}";

    // --- builder happy path ---

    @Test
    void build_withAllValidFields_succeeds() {
        assertDoesNotThrow(() -> defaults(ID_A).build());
    }

    // --- builder null guards ---

    @Test
    void build_withNullId_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> defaults(ID_A).destinationId(null).build());
    }

    @Test
    void build_withNullType_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> defaults(ID_A).destinationType(null).build());
    }

    @Test
    void build_withNullCreatedAt_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> defaults(ID_A).createdAt(null).build());
    }

    // --- builder config validation ---

    @Test
    void build_withNullConfig_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> defaults(ID_A).destinationConfig(null).build());
    }

    @Test
    void build_withBlankConfig_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> defaults(ID_A).destinationConfig("   ").build());
    }

    @Test
    void build_withEmptyConfig_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> defaults(ID_A).destinationConfig("").build());
    }

    // --- updateConfig happy path ---

    @Test
    void updateConfig_withValidConfig_replacesConfig() {
        Destination destination = defaults(ID_A).build();

        destination.updateConfig(UPDATED_CONFIG);

        assertEquals(UPDATED_CONFIG, destination.config());
    }

    @Test
    void updateConfig_withValidConfig_doesNotChangeType() {
        Destination destination = defaults(ID_A).build();

        destination.updateConfig(UPDATED_CONFIG);

        assertEquals(TYPE, destination.destinationType());
    }

    @Test
    void updateConfig_withValidConfig_doesNotChangeCreatedAt() {
        Destination destination = defaults(ID_A).build();

        destination.updateConfig(UPDATED_CONFIG);

        assertEquals(CREATED_AT, destination.createdAt());
    }

    // --- updateConfig rejections ---

    @Test
    void updateConfig_withNullConfig_throwsValidationException() {
        Destination destination = defaults(ID_A).build();

        assertThrows(ValidationException.class, () -> destination.updateConfig(null));
    }

    @Test
    void updateConfig_withBlankConfig_throwsValidationException() {
        Destination destination = defaults(ID_A).build();

        assertThrows(ValidationException.class, () -> destination.updateConfig("   "));
    }

    @Test
    void updateConfig_withEmptyConfig_throwsValidationException() {
        Destination destination = defaults(ID_A).build();

        assertThrows(ValidationException.class, () -> destination.updateConfig(""));
    }

    // --- equality and hashCode by id only ---

    @Test
    void equals_sameId_differentConfig_areEqual() {
        Destination first = defaults(ID_A).destinationConfig(CONFIG).build();
        Destination second = defaults(ID_A).destinationConfig(UPDATED_CONFIG).build();

        assertEquals(first, second);
    }

    @Test
    void equals_differentId_areNotEqual() {
        Destination first = defaults(ID_A).build();
        Destination second = defaults(ID_B).build();

        assertNotEquals(first, second);
    }

    @Test
    void hashCode_sameId_differentConfig_areEqual() {
        Destination first = defaults(ID_A).destinationConfig(CONFIG).build();
        Destination second = defaults(ID_A).destinationConfig(UPDATED_CONFIG).build();

        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void hashCode_differentId_areNotEqual() {
        Destination first = defaults(ID_A).build();
        Destination second = defaults(ID_B).build();

        assertNotEquals(first.hashCode(), second.hashCode());
    }

    // --- helpers ---

    private static Destination.Builder defaults(DestinationId id) {
        return Destination.builder()
                .destinationId(id)
                .destinationType(TYPE)
                .destinationConfig(CONFIG)
                .createdAt(CREATED_AT);
    }
}
