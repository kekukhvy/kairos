package dev.kairos.domain.destination.exceptions;

import dev.kairos.common.exceptions.DomainException;
import dev.kairos.domain.destination.DestinationId;

public class DestinationAlreadyExistsException extends DomainException {

    public DestinationAlreadyExistsException(DestinationId destinationId) {
        super("Destination with id " + destinationId.value() + " already exists.");
    }
}
