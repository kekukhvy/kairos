package dev.kairos.api.destination;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.common.dto.destination.DestinationResponse;
import dev.kairos.domain.destination.Destination;

import static dev.kairos.common.util.helpers.JsonConverter.parseJson;


/**
 * Maps a {@link dev.kairos.domain.destination.Destination} domain entity to a
 * {@link dev.kairos.common.dto.destination.DestinationResponse} DTO. Handles the
 * {@code config} String ↔ {@link com.fasterxml.jackson.databind.JsonNode} conversion
 * so the response embeds config as a proper JSON object rather than an escaped string.
 */
final class DestinationDtoMapper {

    private DestinationDtoMapper() {
    }

    static DestinationResponse toResponse(Destination destination, ObjectMapper objectMapper) {
        return new DestinationResponse(
                destination.destinationId().value(),
                destination.destinationType().name(),
                parseJson(destination.config(), objectMapper),
                destination.createdAt()
        );
    }
}
