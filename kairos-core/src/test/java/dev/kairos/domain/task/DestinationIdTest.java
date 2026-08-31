package dev.kairos.domain.task;

import dev.kairos.common.exceptions.ValidationException;
import dev.kairos.domain.destination.DestinationId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DestinationIdTest {

    private static final String VALID_ID = "dest-kafka-payments";
    private static final String MAX_LENGTH_ID = "a".repeat(DestinationId.MAX_DESTINATION_ID_LENGTH);
    private static final String OVER_MAX_LENGTH_ID = "a".repeat(DestinationId.MAX_DESTINATION_ID_LENGTH + 1);

    @Test
    void constructor_withValidId_createsDestinationId() {
        DestinationId destinationId = new DestinationId(VALID_ID);

        assertNotNull(destinationId);
        assertEquals(VALID_ID, destinationId.value());
    }

    @Test
    void of_withValidId_createsDestinationId() {
        DestinationId destinationId = DestinationId.of(VALID_ID);

        assertNotNull(destinationId);
        assertEquals(VALID_ID, destinationId.value());
    }

    @Test
    void constructor_withNullId_throwsValidationException() {
        assertThrows(ValidationException.class, () -> new DestinationId(null));
    }

    @Test
    void constructor_withBlankId_throwsValidationException() {
        assertThrows(ValidationException.class, () -> new DestinationId("   "));
    }

    @Test
    void constructor_withEmptyId_throwsValidationException() {
        assertThrows(ValidationException.class, () -> new DestinationId(""));
    }

    @Test
    void constructor_withExactMaxLengthId_succeeds() {
        DestinationId destinationId = new DestinationId(MAX_LENGTH_ID);

        assertEquals(MAX_LENGTH_ID, destinationId.value());
    }

    @Test
    void constructor_withIdExceedingMaxLength_throwsValidationException() {
        assertThrows(ValidationException.class, () -> new DestinationId(OVER_MAX_LENGTH_ID));
    }

    @Test
    void of_withNullId_throwsValidationException() {
        assertThrows(ValidationException.class, () -> DestinationId.of(null));
    }

    @Test
    void equality_twoDestinationIdsWithSameValue_areEqual() {
        DestinationId first = DestinationId.of(VALID_ID);
        DestinationId second = DestinationId.of(VALID_ID);

        assertEquals(first, second);
    }
}
