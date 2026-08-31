package dev.kairos.application.task.usecases;

import dev.kairos.application.task.commands.CreateTaskCommand;
import dev.kairos.common.exceptions.ValidationException;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskNameAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.DESTINATION_ID;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.FIXED_NOW;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.EVENT_NAME;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.NAME;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.SERVICE;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.TIMEOUT_MS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateTaskUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private static final String MISSING_DESTINATION_ID = "dest-does-not-exist";

    private InMemoryTaskRepository taskRepository;
    private StubDestinationRepository destinationRepository;
    private CreateTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        taskRepository = new InMemoryTaskRepository();
        destinationRepository = new StubDestinationRepository();
        destinationRepository.register(DESTINATION_ID);
        useCase = new CreateTaskUseCase(destinationRepository, taskRepository, FIXED_CLOCK);
    }

    // --- happy path ---

    @Test
    void execute_withValidCommand_returnsTask() {
        Task result = useCase.execute(validCommand());

        assertNotNull(result);
    }

    @Test
    void execute_withValidCommand_savesTask() {
        useCase.execute(validCommand());

        assertEquals(1, taskRepository.saveCallCount());
    }

    @Test
    void execute_withValidCommand_idIsGenerated() {
        Task result = useCase.execute(validCommand());

        assertNotNull(result.id());
        assertNotNull(result.id().value());
    }

    @Test
    void execute_withValidCommand_timestampsEqualClock() {
        Task result = useCase.execute(validCommand());

        assertEquals(FIXED_NOW, result.createdAt());
        assertEquals(FIXED_NOW, result.updatedAt());
    }

    @Test
    void execute_withValidCommand_taskIsNotDeleted() {
        Task result = useCase.execute(validCommand());

        assertFalse(result.isDeleted());
    }

    // --- nullable active / supportsRetry defaults ---

    @Test
    void execute_withNullActive_defaultsToTrue() {
        CreateTaskCommand command = commandWithNullableFlags(null, null);

        Task result = useCase.execute(command);

        assertTrue(result.active());
    }

    @Test
    void execute_withNullSupportsRetry_defaultsToFalse() {
        CreateTaskCommand command = commandWithNullableFlags(null, null);

        Task result = useCase.execute(command);

        assertFalse(result.supportsRetry());
    }

    @Test
    void execute_withActiveFalse_persistsFalse() {
        CreateTaskCommand command = commandWithNullableFlags(false, null);

        Task result = useCase.execute(command);

        assertFalse(result.active());
    }

    @Test
    void execute_withSupportsRetryTrue_persistsTrue() {
        CreateTaskCommand command = commandWithNullableFlags(null, true);

        Task result = useCase.execute(command);

        assertTrue(result.supportsRetry());
    }

    // --- destination validation ---

    @Test
    void execute_withMissingDestination_throwsValidationException() {
        CreateTaskCommand command = new CreateTaskCommand(
                SERVICE, NAME, null, null,
                MISSING_DESTINATION_ID, EVENT_NAME, null, TIMEOUT_MS, null);

        assertThrows(ValidationException.class, () -> useCase.execute(command));
    }

    @Test
    void execute_withMissingDestination_doesNotSave() {
        CreateTaskCommand command = new CreateTaskCommand(
                SERVICE, NAME, null, null,
                MISSING_DESTINATION_ID, EVENT_NAME, null, TIMEOUT_MS, null);

        try {
            useCase.execute(command);
        } catch (ValidationException ignored) {
        }

        assertEquals(0, taskRepository.saveCallCount());
    }

    // --- unique (service, name) ---

    @Test
    void execute_withDuplicateServiceAndName_throwsTaskNameAlreadyExistsException() {
        useCase.execute(validCommand());

        assertThrows(TaskNameAlreadyExistsException.class, () -> useCase.execute(validCommand()));
    }

    @Test
    void execute_withDuplicateServiceAndName_doesNotSaveSecondTask() {
        useCase.execute(validCommand());

        try {
            useCase.execute(validCommand());
        } catch (TaskNameAlreadyExistsException ignored) {
        }

        assertEquals(1, taskRepository.saveCallCount());
    }

    // --- null guard ---

    @Test
    void execute_withNullCommand_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    // --- helpers ---

    private static CreateTaskCommand validCommand() {
        return new CreateTaskCommand(
                SERVICE, NAME, null, null,
                DESTINATION_ID, EVENT_NAME, null, TIMEOUT_MS, null);
    }

    private static CreateTaskCommand commandWithNullableFlags(Boolean active, Boolean supportsRetry) {
        return new CreateTaskCommand(
                SERVICE, NAME, null, active,
                DESTINATION_ID, EVENT_NAME, null, TIMEOUT_MS, supportsRetry);
    }
}
