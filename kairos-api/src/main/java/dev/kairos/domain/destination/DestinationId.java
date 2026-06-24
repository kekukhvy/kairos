package dev.kairos.domain.destination;

import static dev.kairos.common.util.helpers.Validation.requireText;

public record DestinationId(String value) {

    public static final int MAX_DESTINATION_ID_LENGTH = 128;

    public DestinationId {

        requireText(value, "value", MAX_DESTINATION_ID_LENGTH);
    }

    public static DestinationId of(String destinationId) {
        return new DestinationId(destinationId);
    }

    @Override
    public String toString() {
        return "DestinationId{" +
                "value='" + value + '\'' +
                '}';
    }
}
