package dev.kairos.application.destination.usecases;

import dev.kairos.application.destination.commands.CreateDestinationCommand;
import dev.kairos.common.destination.DestinationType;
import dev.kairos.domain.destination.*;
import dev.kairos.domain.destination.exceptions.DestinationAlreadyExistsException;
import dev.kairos.domain.destination.exceptions.InvalidDestinationTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import static dev.kairos.application.destination.usecases.DestinationConfigValidator.validateConfigSchema;

/**
 * Creates a new destination.
 *
 * <p>Used by the {@code POST /api/v1/destinations} endpoint. {@code id} is
 * caller-supplied (human-readable, e.g. {@code booking-kafka}) rather than
 * generated, so a duplicate-id check runs before the row is written.
 * {@code createdAt} is stamped from the injected {@link Clock}. {@code config}
 * is validated against the per-type schema (see
 * {@code dev.kairos.common.destination.DestinationConfigSchema}) before the
 * destination is built, so a config missing a required key is rejected
 * without ever reaching the repository.
 *
 * @throws DestinationAlreadyExistsException if a destination with the same id
 *                                            already exists
 * @throws InvalidDestinationTypeException   if {@code destinationType} is not
 *                                            one of the known
 *                                            {@link DestinationType} values
 * @throws dev.kairos.common.exceptions.ValidationException if {@code config}
 *                                            omits a key required by the schema
 *                                            for {@code destinationType}
 */
public final class CreateDestinationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(CreateDestinationUseCase.class);

    private final DestinationRepository repository;
    private final Clock clock;
    private final ConfigKeyReader configKeyReader;

    public CreateDestinationUseCase(DestinationRepository repository,
                                    Clock clock,
                                    ConfigKeyReader configKeyReader) {
        this.repository = Objects.requireNonNull(repository, "'repository' must not be null.");
        this.clock = Objects.requireNonNull(clock, "'clock' must not be null.");
        this.configKeyReader = Objects.requireNonNull(configKeyReader, "'configKeyReader' must not be null.");
    }

    public Destination execute(CreateDestinationCommand command) {
        Objects.requireNonNull(command, "'command' must not be null.");
        DestinationId destinationId = DestinationId.of(command.destinationId());

        validateExistingDestination(destinationId);
        DestinationType type = parseDestinationType(command.destinationType());
        validateConfigSchema(type, command.config(), configKeyReader);

        Instant now = clock.instant();
        Destination.Builder builder = Destination.builder()
                .destinationId(destinationId)
                .destinationType(type)
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