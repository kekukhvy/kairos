package dev.kairos.application.task.commands;

import dev.kairos.application.task.usecases.CreateTaskUseCase;

/**
 * Input for {@link CreateTaskUseCase}. Carries raw values (no domain value
 * objects) so the API edge maps request DTO -> command trivially; the use case
 * turns these into domain types and lets the domain validate.
 *
 * <p>{@code active} and {@code supportsRetry} are nullable: when null the domain
 * defaults apply (active = true, supportsRetry = false).
 */
public record CreateTaskCommand(
        String service,
        String name,
        String description,
        Boolean active,
        String destinationId,
        String messageType,
        String payload,
        int timeoutMs,
        Boolean supportsRetry
) {
}
