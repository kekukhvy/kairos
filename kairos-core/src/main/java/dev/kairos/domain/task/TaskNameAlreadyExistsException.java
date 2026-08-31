package dev.kairos.domain.task;

import dev.kairos.common.exceptions.DomainException;

/**
 * Raised when a task's {@code (service, name)} pair is not unique among live
 * tasks — the human-readable identity a task exposes to its owning service.
 * {@code service} is immutable, so this can only be triggered by creating a
 * task or renaming one into a name already taken within the same service.
 *
 * <p>How this surfaces over HTTP is the application/API layer's call (409
 * Conflict, mirroring {@link TaskAlreadyDeletedException}).
 */
public class TaskNameAlreadyExistsException extends DomainException {

    public TaskNameAlreadyExistsException(String service, String name) {
        super("Task '" + name + "' already exists in service '" + service + "'");
    }
}
