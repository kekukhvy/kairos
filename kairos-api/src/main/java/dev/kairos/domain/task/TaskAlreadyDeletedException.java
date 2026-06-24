package dev.kairos.domain.task;

import dev.kairos.common.exceptions.DomainException;

/**
 * Raised when an operation targets a task that is already soft-deleted — the
 * core invariant of M1: once {@code deletedAt} is set, the task is frozen.
 *
 * <p>How this surfaces over HTTP is the application/API layer's call (e.g. a
 * repeated DELETE -> 409 Conflict).
 */
public class TaskAlreadyDeletedException extends DomainException {

    private final TaskId taskId;

    public TaskAlreadyDeletedException(TaskId taskId) {
        super("Task " + taskId.value() + " is already deleted");
        this.taskId = taskId;
    }

    public TaskId taskId() {
        return taskId;
    }
}