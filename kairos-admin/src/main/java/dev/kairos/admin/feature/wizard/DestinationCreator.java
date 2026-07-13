package dev.kairos.admin.feature.wizard;

import dev.kairos.admin.feature.destination.dto.CreateDestinationRequest;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;

/**
 * Narrow collaborator {@link WizardCommit} depends on instead of the full
 * {@code DestinationService}, so unit tests can stub it with a lambda instead
 * of standing up a Spring bean.
 */
@FunctionalInterface
public interface DestinationCreator {

    DestinationDTO create(CreateDestinationRequest request);
}
