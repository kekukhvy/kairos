package dev.kairos.domain.task;


import dev.kairos.domain.destination.DestinationId;

/**
 * The set of editable fields applied by {@link Task#update}. A named carrier
 * (instead of a long positional argument list) keeps the call site readable and
 * prevents accidentally swapping the two boolean flags.
 *
 * <p>Immutable, non-editable identity fields ({@code id}, {@code service},
 * {@code createdAt}) are intentionally absent.
 */
public record TaskEdit(
        String name,
        String description,
        boolean active,
        DestinationId destinationId,
        String eventName,
        String payload,
        int timeoutMs,
        boolean supportsRetry
) {
}
