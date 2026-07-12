package dev.kairos.admin.feature.destination.dto;

/**
 * Admin-side request body for {@code POST /api/v1/destinations}.
 *
 * @param destinationId   caller-supplied unique identifier (e.g. {@code booking-kafka})
 * @param destinationType transport type; must match a known API enum value
 *                        (e.g. {@code KAFKA}, {@code WEBHOOK})
 * @param config          transport-specific connectivity settings as a JSON object
 *
 * <p>Kept local rather than reusing the {@code common} equivalent because of the
 * Jackson 2 {@code JsonNode} vs. Jackson 3 mismatch on {@code config}; see
 * {@link DestinationDTO} for the full rationale.
 */
public record CreateDestinationRequest(
        String destinationId,
        String destinationType,
        Object config
) {
}
