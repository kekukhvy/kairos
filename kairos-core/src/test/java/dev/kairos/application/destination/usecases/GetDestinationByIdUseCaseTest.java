package dev.kairos.application.destination.usecases;

import dev.kairos.domain.destination.Destination;
import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.exceptions.DestinationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dev.kairos.application.destination.usecases.DestinationBuilder.DEFAULT_ID;
import static dev.kairos.application.destination.usecases.DestinationBuilder.buildDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetDestinationByIdUseCaseTest {

    private static final DestinationId KNOWN_ID = DestinationId.of(DEFAULT_ID);
    private static final DestinationId UNKNOWN_ID = DestinationId.of("dest-does-not-exist");

    private InMemoryDestinationRepository destinationRepository;
    private GetDestinationByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        destinationRepository = new InMemoryDestinationRepository();
        useCase = new GetDestinationByIdUseCase(destinationRepository);
    }

    // --- happy path ---

    @Test
    void execute_withKnownId_returnsDestination() {
        destinationRepository.seed(buildDefault());

        Destination result = useCase.execute(KNOWN_ID);

        assertEquals(KNOWN_ID, result.destinationId());
    }

    // --- missing destination ---

    @Test
    void execute_withUnknownId_throwsDestinationNotFoundException() {
        assertThrows(DestinationNotFoundException.class,
                () -> useCase.execute(UNKNOWN_ID));
    }

    @Test
    void execute_withUnknownId_exceptionCarriesDestinationId() {
        DestinationNotFoundException thrown = assertThrows(DestinationNotFoundException.class,
                () -> useCase.execute(UNKNOWN_ID));

        assertEquals(UNKNOWN_ID, thrown.destinationId());
    }

    // --- null guard ---

    @Test
    void execute_withNullId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }
}
