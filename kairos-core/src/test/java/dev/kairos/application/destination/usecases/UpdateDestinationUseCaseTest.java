package dev.kairos.application.destination.usecases;

import dev.kairos.common.exceptions.ValidationException;
import dev.kairos.domain.destination.Destination;
import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.exceptions.DestinationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dev.kairos.application.destination.usecases.DestinationBuilder.DEFAULT_CONFIG;
import static dev.kairos.application.destination.usecases.DestinationBuilder.DEFAULT_ID;
import static dev.kairos.application.destination.usecases.DestinationBuilder.DEFAULT_TYPE;
import static dev.kairos.application.destination.usecases.DestinationBuilder.buildDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateDestinationUseCaseTest {

    private static final DestinationId KNOWN_ID = DestinationId.of(DEFAULT_ID);
    private static final DestinationId UNKNOWN_ID = DestinationId.of("dest-does-not-exist");
    private static final String NEW_CONFIG = "{\"topic\":\"refunds\"}";

    private InMemoryDestinationRepository destinationRepository;
    private UpdateDestinationUseCase useCase;

    @BeforeEach
    void setUp() {
        destinationRepository = new InMemoryDestinationRepository();
        useCase = new UpdateDestinationUseCase(destinationRepository, new JacksonConfigKeyReaderForTests());
    }

    // --- happy path ---

    @Test
    void execute_withKnownId_updatesConfigInRepository() {
        destinationRepository.seed(buildDefault());

        useCase.execute(KNOWN_ID, NEW_CONFIG);

        Destination stored = destinationRepository.findById(KNOWN_ID).orElseThrow();
        assertEquals(NEW_CONFIG, stored.config());
    }

    @Test
    void execute_withKnownId_savesAfterUpdate() {
        destinationRepository.seed(buildDefault());

        useCase.execute(KNOWN_ID, NEW_CONFIG);

        assertEquals(1, destinationRepository.saveCallCount());
    }

    @Test
    void execute_withKnownId_doesNotChangeType() {
        destinationRepository.seed(buildDefault());

        useCase.execute(KNOWN_ID, NEW_CONFIG);

        Destination stored = destinationRepository.findById(KNOWN_ID).orElseThrow();
        assertEquals(DEFAULT_TYPE, stored.destinationType());
    }

    @Test
    void execute_withSameConfig_savesSuccessfully() {
        destinationRepository.seed(buildDefault());

        useCase.execute(KNOWN_ID, DEFAULT_CONFIG);

        assertEquals(1, destinationRepository.saveCallCount());
    }

    // --- missing destination ---

    @Test
    void execute_withUnknownId_throwsDestinationNotFoundException() {
        assertThrows(DestinationNotFoundException.class,
                () -> useCase.execute(UNKNOWN_ID, NEW_CONFIG));
    }

    @Test
    void execute_withUnknownId_doesNotSave() {
        try {
            useCase.execute(UNKNOWN_ID, NEW_CONFIG);
        } catch (DestinationNotFoundException ignored) {}

        assertEquals(0, destinationRepository.saveCallCount());
    }

    // --- invalid config rejected by domain ---

    @Test
    void execute_withNullConfig_throwsValidationException() {
        destinationRepository.seed(buildDefault());

        assertThrows(ValidationException.class,
                () -> useCase.execute(KNOWN_ID, null));
    }

    @Test
    void execute_withBlankConfig_throwsValidationException() {
        destinationRepository.seed(buildDefault());

        assertThrows(ValidationException.class,
                () -> useCase.execute(KNOWN_ID, "   "));
    }

    // --- null guard ---

    @Test
    void execute_withNullId_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> useCase.execute(null, NEW_CONFIG));
    }

    // --- config schema validation against stored type ---

    @Test
    void execute_withConfigMissingRequiredKeyForStoredType_throwsValidationException() {
        destinationRepository.seed(buildDefault());

        assertThrows(ValidationException.class,
                () -> useCase.execute(KNOWN_ID, "{}"));
    }

    @Test
    void execute_withConfigMissingRequiredKeyForStoredType_messageNamesMissingKey() {
        destinationRepository.seed(buildDefault());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> useCase.execute(KNOWN_ID, "{}"));

        assertEquals("config is missing required key(s): topic", ex.getMessage());
    }

    @Test
    void execute_withConfigMissingRequiredKeyForStoredType_doesNotSave() {
        destinationRepository.seed(buildDefault());

        try {
            useCase.execute(KNOWN_ID, "{}");
        } catch (ValidationException ignored) {
        }

        assertEquals(0, destinationRepository.saveCallCount());
    }

    @Test
    void execute_withConfigContainingRequiredKeyForStoredType_succeeds() {
        destinationRepository.seed(buildDefault());

        useCase.execute(KNOWN_ID, "{\"topic\":\"\"}");

        Destination stored = destinationRepository.findById(KNOWN_ID).orElseThrow();
        assertEquals("{\"topic\":\"\"}", stored.config());
    }
}
