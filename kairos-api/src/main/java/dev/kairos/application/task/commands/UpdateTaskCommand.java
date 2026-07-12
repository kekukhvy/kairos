package dev.kairos.application.task.commands;

import dev.kairos.application.task.usecases.UpdateTaskUseCase;

/**
 * Input for {@link UpdateTaskUseCase}. PUT semantics: a full replacement of the
 * editable fields. {@code service} is intentionally absent — a task's owning
 * service is immutable.
 *
 * <p>{@code active} and {@code supportsRetry} are nullable: when null they fall
 * back to active = true / supportsRetry = false.
 */
public record UpdateTaskCommand(
        String name,
        String description,
        Boolean active,
        String destinationId,
        String eventName,
        String payload,
        int timeoutMs,
        Boolean supportsRetry
) {
}
