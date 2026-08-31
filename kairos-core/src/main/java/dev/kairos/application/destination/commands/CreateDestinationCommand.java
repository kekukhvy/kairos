package dev.kairos.application.destination.commands;

import dev.kairos.application.destination.usecases.CreateDestinationUseCase;

/**
 * Input for {@link CreateDestinationUseCase}. Carries raw values (no domain
 * value objects) so the API edge maps request DTO -> command trivially; the
 * use case turns these into domain types ({@link dev.kairos.domain.destination.DestinationId},
 * {@link dev.kairos.common.destination.DestinationType}) and lets the domain
 * validate.
 */
public record CreateDestinationCommand(
        String destinationId,
        String destinationType,
        String config
) {
}