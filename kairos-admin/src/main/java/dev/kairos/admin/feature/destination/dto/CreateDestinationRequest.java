package dev.kairos.admin.feature.destination.dto;

/**
 * Admin-side request body for {@code POST /api/v1/destinations}.
 *
 * @param destinationId   caller-supplied unique identifier (e.g. {@code booking-kafka})
 * @param destinationType transport type; must match a known API enum value
 *                        (e.g. {@code KAFKA}, {@code WEBHOOK})
 * @param config          transport-specific connectivity settings as a JSON object
 */
public record CreateDestinationRequest(
        String destinationId,
        String destinationType,
        Object config
) {
}
