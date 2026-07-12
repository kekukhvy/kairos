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
 *
 * <p>Kept local rather than reusing
 * {@code dev.kairos.common.dto.destination.DestinationResponse}: the common
 * record types {@code config} as a Jackson 2 {@code JsonNode}, which the admin's
 * Jackson 3 ({@code tools.jackson}) mapper cannot deserialize. Here {@code config}
 * is a plain {@link Object}, handled by both. Remove once {@code common} switches
 * that field to {@code Object}.
 */
public record DestinationDTO(
        String destinationId,
        String destinationType,
        Object config,
        Instant createdAt
        ) {
}