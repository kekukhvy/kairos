package dev.kairos.domain.task;

import dev.kairos.common.exceptions.DomainException;

/**
 * Raised when a task cannot be found — either no row exists, or it is
 * soft-deleted and therefore invisible to reads and updates. The API maps this
 * to HTTP 404.
 */
public class TaskNotFoundException extends DomainException {

    private final TaskId taskId;

    public TaskNotFoundException(TaskId taskId) {
        super("Task " + taskId.value() + " not found");
        this.taskId = taskId;
    }

    public TaskId taskId() {
        return taskId;
    }
}