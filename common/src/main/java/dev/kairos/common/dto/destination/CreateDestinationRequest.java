package dev.kairos.common.dto.destination;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Request body for {@code POST /api/v1/destinations}.
 *
 * <p>{@code destinationId} is caller-supplied (human-readable, e.g.
 * {@code booking-kafka}) and must be unique across all destinations.
 * {@code destinationType} must match one of the known
 * {@link dev.kairos.domain.destination.DestinationType} enum values
 * (case-sensitive). {@code config} accepts any valid JSON object holding the
 * connectivity parameters for the chosen delivery adapter; stored as JSONB.
 */
public record CreateDestinationRequest(
        String destinationId,
        String destinationType,
        JsonNode config
) {
}