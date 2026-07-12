package dev.kairos.common.dto.destination;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Request body for {@code PUT /api/v1/destinations/{id}}.
 *
 * <p>Only {@code config} may be updated after a destination is created —
 * {@code destinationType} and {@code createdAt} are immutable. To change the
 * delivery mechanism itself, delete the destination and create a new one.
 * {@code config} accepts any valid JSON object; stored as JSONB.
 */
public record UpdateDestinationRequest(
        JsonNode config
) {
}
