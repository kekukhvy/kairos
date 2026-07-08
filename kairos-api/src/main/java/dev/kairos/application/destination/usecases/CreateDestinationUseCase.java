package dev.kairos.application.destination.usecases;

import dev.kairos.application.destination.commands.CreateDestinationCommand;
import dev.kairos.domain.destination.*;
import dev.kairos.domain.destination.exceptions.DestinationAlreadyExistsException;
import dev.kairos.domain.destination.exceptions.InvalidDestinationTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Creates a new destination.
 *
 * <p>Used by the {@code POST /api/v1/destinations} endpoint. {@code id} is
 * caller-supplied (human-readable, e.g. {@code booking-kafka}) rather than
 * generated, so a duplicate-id check runs before the row is written.
 * {@code createdAt} is stamped from the injected {@link Clock}.
 *
 * @throws DestinationAlreadyExistsException if a destination with the same id
 *                                            already exists
 * @throws InvalidDestinationTypeException   if {@code destinationType} is not
 *                                            one of the known
 *                                            {@link DestinationType} values
 */
public final class CreateDestinationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreateDestinationUseCase.class);

    private final DestinationRepository repository;
    private final Clock clock;

    public CreateDestinationUseCase(DestinationRepository repository,
                                    Clock clock) {
        this.repository = Objects.requireNonNull(repository, "'repository' must not be null.");
        this.clock = Objects.requireNonNull(clock, "'clock' must not be null.");
    }

    public Destination execute(CreateDestinationCommand command) {
        Objects.requireNonNull(command, "'command' must not be null.");
        DestinationId destinationId = DestinationId.of(command.destinationId());

        validateExistingDestination(destinationId);

        Instant now = clock.instant();
        Destination.Builder builder = Destination.builder()
                .destinationId(destinationId)
                .destinationType(parseDestinationType(command.destinationType()))
                .destinationConfig(command.config())
                .createdAt(now);

        Destination destination = builder.build();
        repository.save(destination);
        logger.info("Destination created: id='{}', type={}", destinationId.value(), destination.destinationType());
        return destination;
    }

    private void validateExistingDestination(DestinationId destinationId) {
        if (repository.existsById(destinationId)) {
            logger.warn("Destination creation rejected — id already exists: '{}'", destinationId.value());
            throw new DestinationAlreadyExistsException(destinationId);
        }
    }

    private DestinationType parseDestinationType(String raw) {
        try {
            return DestinationType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            logger.warn("Destination creation rejected — unrecognised type: '{}'", raw);
            throw new InvalidDestinationTypeException("Invalid destination type: " + raw);
        }
    }

}