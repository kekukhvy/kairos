package dev.kairos.admin.feature.destination.dto;

import java.time.Instant;

/**
 * Admin-side response DTO for a single destination returned by the kairos-api.
 *
 * @param destinationId   unique identifier of the destination
 * @param destinationType transport type (e.g. {@code KAFKA}, {@code WEBHOOK})
 * @param config          opaque JSON object containing transport-specific connectivity
 *                        settings; structure varies by {@code destinationType}
 * @param createdAt       timestamp at which the destination was registered
 */
public record DestinationDTO(
        String destinationId,
        String destinationType,
        Object config,
        Instant createdAt
        ) {
}