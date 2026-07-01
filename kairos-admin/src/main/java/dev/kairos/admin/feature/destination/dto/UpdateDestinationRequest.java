package dev.kairos.admin.feature.destination.dto;

/**
 * Admin-side request body for {@code PUT /api/v1/destinations/{id}}.
 *
 * <p>Only {@code config} may be changed; the destination id and type are
 * immutable after creation.
 *
 * @param config transport-specific connectivity settings as a JSON object
 */
public record UpdateDestinationRequest(
        Object config
) {
}
