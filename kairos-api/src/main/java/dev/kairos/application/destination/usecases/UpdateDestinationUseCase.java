package dev.kairos.application.destination.usecases;

import dev.kairos.domain.destination.Destination;
import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.DestinationRepository;
import dev.kairos.domain.destination.exceptions.DestinationNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Updates the connectivity config of an existing destination.
 *
 * <p>Only {@code config} may be changed after creation — {@code type} and
 * {@code createdAt} are immutable. To change the delivery mechanism itself,
 * delete the destination and create a new one.
 *
 * <p>Used by the {@code PUT /api/v1/destinations/{id}} endpoint.
 *
 * @throws DestinationNotFoundException if no destination exists for the given id
 */
public final class UpdateDestinationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(UpdateDestinationUseCase.class);

    private final DestinationRepository destinationRepository;

    public UpdateDestinationUseCase(DestinationRepository destinationRepository) {
        this.destinationRepository = Objects.requireNonNull(destinationRepository);

    }

    /**
     * Loads the destination, replaces its config, and persists the change.
     *
     * @param destinationId id of the destination to update
     * @param config        new connectivity config (must be non-null and non-blank)
     * @throws DestinationNotFoundException if no destination exists for {@code destinationId}
     */
    public void execute(DestinationId destinationId, String config) {
        Objects.requireNonNull(destinationId, "destinationId cannot be null!");

        logger.debug("Loading destination for config update: id='{}'", destinationId.value());

        Destination destination = destinationRepository.findById(destinationId)
                .orElseThrow(() -> new DestinationNotFoundException(destinationId));

        destination.updateConfig(config);
        destinationRepository.save(destination);
        logger.info("Destination config updated: id='{}'", destinationId.value());
    }
}
