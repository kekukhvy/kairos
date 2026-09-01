package dev.kairos.application.destination.usecases;

import dev.kairos.application.destination.commands.CreateDestinationCommand;
import dev.kairos.common.exceptions.ValidationException;
import dev.kairos.domain.destination.Destination;
import dev.kairos.common.destination.DestinationType;
import dev.kairos.domain.destination.exceptions.DestinationAlreadyExistsException;
import dev.kairos.domain.destination.exceptions.InvalidDestinationTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static dev.kairos.application.destination.usecases.DestinationBuilder.DEFAULT_CONFIG;
import static dev.kairos.application.destination.usecases.DestinationBuilder.DEFAULT_ID;
import static dev.kairos.application.destination.usecases.DestinationBuilder.DEFAULT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateDestinationUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private static final String UNKNOWN_TYPE = "GRPC";

    private static final Map<DestinationType, String> VALID_CONFIG_BY_TYPE = Map.of(
            DestinationType.KAFKA, "{\"topic\":\"payments\"}",
            DestinationType.SQS, "{\"queueUrl\":\"https://sqs.example.com/q\"}",
            DestinationType.WEBHOOK, "{\"url\":\"https://example.com/hook\"}",
            DestinationType.RABBITMQ, "{\"exchange\":\"orders\",\"routingKey\":\"orders.created\"}"
    );

    private InMemoryDestinationRepository destinationRepository;
    private CreateDestinationUseCase useCase;

    @BeforeEach
    void setUp() {
        destinationRepository = new InMemoryDestinationRepository();
        useCase = new CreateDestinationUseCase(destinationRepository, FIXED_CLOCK, new JacksonConfigKeyReaderForTests());
    }

    // --- happy path ---

    @Test
    void execute_withValidCommand_returnsDestination() {
        Destination result = useCase.execute(validCommand());

        assertNotNull(result);
    }

    @Test
    void execute_withValidCommand_savesDestination() {
        useCase.execute(validCommand());

        assertEquals(1, destinationRepository.saveCallCount());
    }

    @Test
    void execute_withValidCommand_createdAtEqualsClockInstant() {
        Destination result = useCase.execute(validCommand());

        assertEquals(FIXED_NOW, result.createdAt());
    }

    @Test
    void execute_withValidCommand_idMatchesCommand() {
        Destination result = useCase.execute(validCommand());

        assertEquals(DEFAULT_ID, result.destinationId().value());
    }

    @Test
    void execute_withValidCommand_typeMatchesCommand() {
        Destination result = useCase.execute(validCommand());

        assertEquals(DEFAULT_TYPE, result.destinationType());
    }

    @Test
    void execute_withValidCommand_configMatchesCommand() {
        Destination result = useCase.execute(validCommand());

        assertEquals(DEFAULT_CONFIG, result.config());
    }

    @Test
    void execute_withAllKnownTypes_parsesEachType() {
        for (DestinationType type : DestinationType.values()) {
            CreateDestinationCommand cmd = new CreateDestinationCommand(
                    "dest-" + type.name().toLowerCase(),
                    type.name(),
                    VALID_CONFIG_BY_TYPE.get(type)
            );

            Destination result = useCase.execute(cmd);

            assertEquals(type, result.destinationType());
        }
    }

    // --- config schema validation ---

    @Test
    void execute_withConfigMissingRequiredKey_throwsValidationException() {
        CreateDestinationCommand cmd = new CreateDestinationCommand(
                DEFAULT_ID, DestinationType.SQS.name(), "{}");

        assertThrows(ValidationException.class, () -> useCase.execute(cmd));
    }

    @Test
    void execute_withConfigMissingRequiredKey_messageNamesMissingKey() {
        CreateDestinationCommand cmd = new CreateDestinationCommand(
                DEFAULT_ID, DestinationType.SQS.name(), "{}");

        ValidationException ex = assertThrows(ValidationException.class, () -> useCase.execute(cmd));

        assertTrue(ex.getMessage().contains("queueUrl"));
    }

    @Test
    void execute_withConfigMissingRequiredKey_doesNotSave() {
        CreateDestinationCommand cmd = new CreateDestinationCommand(
                DEFAULT_ID, DestinationType.SQS.name(), "{}");

        try {
            useCase.execute(cmd);
        } catch (ValidationException ignored) {
        }

        assertEquals(0, destinationRepository.saveCallCount());
    }

    @Test
    void execute_withConfigContainingAllRequiredKeys_succeeds() {
        CreateDestinationCommand cmd = new CreateDestinationCommand(
                DEFAULT_ID, DestinationType.RABBITMQ.name(),
                "{\"exchange\":\"orders\",\"routingKey\":\"orders.created\"}");

        Destination result = useCase.execute(cmd);

        assertEquals(DestinationType.RABBITMQ, result.destinationType());
    }

    @Test
    void execute_withConfigNotAJsonObject_throwsValidationException() {
        CreateDestinationCommand cmd = new CreateDestinationCommand(
                DEFAULT_ID, DestinationType.KAFKA.name(), "[\"topic\"]");

        assertThrows(ValidationException.class, () -> useCase.execute(cmd));
    }

    // --- duplicate id ---

    @Test
    void execute_whenIdAlreadyExists_throwsDestinationAlreadyExistsException() {
        destinationRepository.seed(DestinationBuilder.buildDefault());

        assertThrows(DestinationAlreadyExistsException.class,
                () -> useCase.execute(validCommand()));
    }

    @Test
    void execute_whenIdAlreadyExists_doesNotSave() {
        destinationRepository.seed(DestinationBuilder.buildDefault());

        try {
            useCase.execute(validCommand());
        } catch (DestinationAlreadyExistsException ignored) {}

        assertEquals(0, destinationRepository.saveCallCount());
    }

    // --- invalid type ---

    @Test
    void execute_withUnknownType_throwsInvalidDestinationTypeException() {
        CreateDestinationCommand cmd = new CreateDestinationCommand(
                DEFAULT_ID, UNKNOWN_TYPE, DEFAULT_CONFIG);

        assertThrows(InvalidDestinationTypeException.class,
                () -> useCase.execute(cmd));
    }

    @Test
    void execute_withUnknownType_doesNotSave() {
        CreateDestinationCommand cmd = new CreateDestinationCommand(
                DEFAULT_ID, UNKNOWN_TYPE, DEFAULT_CONFIG);

        try {
            useCase.execute(cmd);
        } catch (InvalidDestinationTypeException ignored) {}

        assertEquals(0, destinationRepository.saveCallCount());
    }

    // --- null guard ---

    @Test
    void execute_withNullCommand_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    // --- helpers ---

    private static CreateDestinationCommand validCommand() {
        return new CreateDestinationCommand(DEFAULT_ID, DEFAULT_TYPE.name(), DEFAULT_CONFIG);
    }
}
