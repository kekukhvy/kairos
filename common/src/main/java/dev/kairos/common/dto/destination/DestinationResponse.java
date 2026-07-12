package dev.kairos.common.dto.destination;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Response body returned by all destination endpoints.
 *
 * <p>{@code destinationType} is the string name of the
 * {@link dev.kairos.domain.destination.DestinationType} enum value.
 * {@code config} is the raw connectivity config embedded as a JSON object
 * (never an escaped string). {@code createdAt} is the UTC instant at which
 * the destination was first persisted.
 */
public record DestinationResponse(
        String destinationId,
        String destinationType,
        JsonNode config,
        Instant createdAt
) {
}
