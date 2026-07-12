package dev.kairos.application.task.usecases;

import dev.kairos.application.task.commands.UpdateTaskCommand;
import dev.kairos.common.exceptions.ValidationException;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import dev.kairos.domain.task.TaskNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;

import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.DESTINATION_ID;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.FIXED_NOW;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.SERVICE;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.TASK_ID;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.deletedTask;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.liveTask;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateTaskUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private static final String UPDATED_NAME = "updated-name";
    private static final String UPDATED_DESCRIPTION = "updated description";
    private static final String UPDATED_DESTINATION_ID = "dest-kafka-2";
    private static final String UPDATED_EVENT_NAME = "payment.receipt.updated";
    private static final String UPDATED_PAYLOAD = "{\"updated\":true}";
    private static final int UPDATED_TIMEOUT_MS = 10_000;
    private static final String MISSING_DESTINATION_ID = "dest-does-not-exist";

    private InMemoryTaskRepository taskRepository;
    private StubDestinationRepository destinationRepository;
    private UpdateTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        taskRepository = new InMemoryTaskRepository();
        destinationRepository = new StubDestinationRepository();
        destinationRepository.register(DESTINATION_ID);
        destinationRepository.register(UPDATED_DESTINATION_ID);
        useCase = new UpdateTaskUseCase(taskRepository, destinationRepository, FIXED_CLOCK);
    }

    // --- happy path ---

    @Test
    void execute_withValidCommand_returnsUpdatedTask() {
        taskRepository.seed(liveTask());

        Task result = useCase.execute(TASK_ID, validUpdateCommand());

        assertEquals(UPDATED_NAME, result.name());
    }

    @Test
    void execute_withValidCommand_savesTask() {
        taskRepository.seed(liveTask());

        useCase.execute(TASK_ID, validUpdateCommand());

        assertEquals(1, taskRepository.saveCallCount());
    }

    @Test
    void execute_withValidCommand_updatedAtEqualsClockNow() {
        taskRepository.seed(liveTask());

        Task result = useCase.execute(TASK_ID, validUpdateCommand());

        assertEquals(FIXED_NOW, result.updatedAt());
    }

    @Test
    void execute_withValidCommand_allEditableFieldsAreChanged() {
        taskRepository.seed(liveTask());
        UpdateTaskCommand command = validUpdateCommand();

        Task result = useCase.execute(TASK_ID, command);

        assertEquals(UPDATED_NAME, result.name());
        assertEquals(UPDATED_DESCRIPTION, result.description());
        assertEquals(UPDATED_EVENT_NAME, result.eventName());
        assertEquals(UPDATED_PAYLOAD, result.payload());
        assertEquals(UPDATED_TIMEOUT_MS, result.timeoutMs());
    }

    @Test
    void execute_withValidCommand_serviceIsNotChanged() {
        taskRepository.seed(liveTask());

        Task result = useCase.execute(TASK_ID, validUpdateCommand());

        assertEquals(SERVICE, result.service());
    }

    // --- missing task ---

    @Test
    void execute_withUnknownTaskId_throwsTaskNotFoundException() {
        TaskId unknownId = new TaskId(UUID.fromString("ffffffff-0000-0000-0000-000000000099"));

        assertThrows(TaskNotFoundException.class,
                () -> useCase.execute(unknownId, validUpdateCommand()));
    }

    // --- soft-deleted task treated as not found ---

    @Test
    void execute_onSoftDeletedTask_throwsTaskNotFoundException() {
        taskRepository.seed(deletedTask());

        assertThrows(TaskNotFoundException.class,
                () -> useCase.execute(TASK_ID, validUpdateCommand()));
    }

    // --- destination validation ---

    @Test
    void execute_withMissingDestination_throwsValidationException() {
        taskRepository.seed(liveTask());
        UpdateTaskCommand command = new UpdateTaskCommand(
                UPDATED_NAME, UPDATED_DESCRIPTION, true,
                MISSING_DESTINATION_ID, UPDATED_EVENT_NAME,
                UPDATED_PAYLOAD, UPDATED_TIMEOUT_MS, false);

        assertThrows(ValidationException.class,
                () -> useCase.execute(TASK_ID, command));
    }

    // --- null guards ---

    @Test
    void execute_withNullTaskId_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> useCase.execute(null, validUpdateCommand()));
    }

    @Test
    void execute_withNullCommand_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> useCase.execute(TASK_ID, null));
    }

    // --- helpers ---

    private static UpdateTaskCommand validUpdateCommand() {
        return new UpdateTaskCommand(
                UPDATED_NAME, UPDATED_DESCRIPTION, true,
                UPDATED_DESTINATION_ID, UPDATED_EVENT_NAME,
                UPDATED_PAYLOAD, UPDATED_TIMEOUT_MS, false);
    }
}
