package dev.kairos.application.destination.usecases;

import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.destination.exceptions.DestinationInUseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dev.kairos.application.destination.usecases.DestinationBuilder.DEFAULT_ID;
import static dev.kairos.application.destination.usecases.DestinationBuilder.buildDefault;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteDestinationUseCaseTest {

    private static final DestinationId KNOWN_ID = DestinationId.of(DEFAULT_ID);
    private static final DestinationId ABSENT_ID = DestinationId.of("dest-does-not-exist");

    private InMemoryDestinationRepository destinationRepository;
    private StubTaskRepository taskRepository;
    private DeleteDestinationUseCase useCase;

    @BeforeEach
    void setUp() {
        destinationRepository = new InMemoryDestinationRepository();
        taskRepository = new StubTaskRepository();
        useCase = new DeleteDestinationUseCase(destinationRepository, taskRepository);
    }

    // --- happy path ---

    @Test
    void execute_whenNoTaskReferences_deletesDestination() {
        destinationRepository.seed(buildDefault());

        useCase.execute(KNOWN_ID);

        assertFalse(destinationRepository.existsById(KNOWN_ID));
    }

    @Test
    void execute_whenNoTaskReferences_doesNotThrow() {
        destinationRepository.seed(buildDefault());

        assertDoesNotThrow(() -> useCase.execute(KNOWN_ID));
    }

    // --- in-use guard ---

    @Test
    void execute_whenTaskReferencesDestination_throwsDestinationInUseException() {
        destinationRepository.seed(buildDefault());
        taskRepository.markInUse(DEFAULT_ID);

        assertThrows(DestinationInUseException.class, () -> useCase.execute(KNOWN_ID));
    }

    @Test
    void execute_whenTaskReferencesDestination_doesNotDelete() {
        destinationRepository.seed(buildDefault());
        taskRepository.markInUse(DEFAULT_ID);

        try {
            useCase.execute(KNOWN_ID);
        } catch (DestinationInUseException ignored) {}

        assertTrue(destinationRepository.existsById(KNOWN_ID));
    }

    // --- idempotent when absent ---

    @Test
    void execute_whenDestinationAbsent_doesNotThrow() {
        // No destination seeded — delete of an absent row is treated as a no-op
        // (hard-delete semantics: the row is already gone, outcome is the same).
        assertDoesNotThrow(() -> useCase.execute(ABSENT_ID));
    }

    // --- null guard ---

    @Test
    void execute_withNullId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }
}
