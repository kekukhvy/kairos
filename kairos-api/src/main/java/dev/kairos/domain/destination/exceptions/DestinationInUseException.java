package dev.kairos.domain.destination.exceptions;

import dev.kairos.common.exceptions.DomainException;
import dev.kairos.domain.destination.DestinationId;

/**
 * Raised when a caller attempts to delete a destination that is still
 * referenced by at least one task. The destination cannot be removed until
 * all referencing tasks are deleted or reassigned.
 */
public class DestinationInUseException extends DomainException {
    public DestinationInUseException(DestinationId destinationId) {
        super(String.format("Destination with id '%s' is already in use", destinationId.value()));
    }
}
