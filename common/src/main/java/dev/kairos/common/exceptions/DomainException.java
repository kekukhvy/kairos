package dev.kairos.common.exceptions;


/**
 * Base type for all domain-level failures (broken invariants, invalid state).
 * Lets the API layer catch one type and translate domain errors into HTTP
 * responses, instead of leaking raw {@link RuntimeException}s.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}