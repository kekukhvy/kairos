package dev.kairos.domain.destination.exceptions;

import dev.kairos.common.exceptions.DomainException;

public class InvalidDestinationTypeException extends DomainException {
    public InvalidDestinationTypeException(String raw) {
        super(raw);
    }
}
