package dev.kairos.admin.feature.destination.dto;

/**
 * Admin-side request body for {@code PUT /api/v1/destinations/{id}}.
 *
 * <p>Only {@code config} may be changed; the destination id and type are
 * immutable after creation.
 *
 * @param config transport-specific connectivity settings as a JSON object
 *
 * <p>Kept local rather than reusing the {@code common} equivalent because of the
 * Jackson 2 {@code JsonNode} vs. Jackson 3 mismatch on {@code config}; see
 * {@link DestinationDTO} for the full rationale.
 */
public record UpdateDestinationRequest(
        Object config
) {
}
