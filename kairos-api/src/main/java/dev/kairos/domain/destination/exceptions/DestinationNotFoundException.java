package dev.kairos.domain.destination.exceptions;

import dev.kairos.common.exceptions.DomainException;
import dev.kairos.domain.destination.DestinationId;

/**
 * Raised when no destination exists for a given {@link DestinationId}.
 * Destinations have no soft-delete state, so "not found" always means no row
 * exists for the id. The API maps this to HTTP 404.
 */
public class DestinationNotFoundException extends DomainException {

    private final DestinationId destinationId;

    public DestinationNotFoundException(DestinationId destinationId) {
        super("Destination " + destinationId.value() + " not found");
        this.destinationId = destinationId;
    }

    public DestinationId destinationId() {
        return destinationId;
    }
}