package dev.kairos.application.task.usecases;

import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import dev.kairos.domain.task.TaskNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.TASK_ID;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.deletedTask;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.liveTask;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetTaskUseCaseTest {

    private static final TaskId UNKNOWN_TASK_ID =
            new TaskId(UUID.fromString("ffffffff-0000-0000-0000-000000000099"));

    private InMemoryTaskRepository taskRepository;
    private GetTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        taskRepository = new InMemoryTaskRepository();
        useCase = new GetTaskUseCase(taskRepository);
    }

    // --- happy path ---

    @Test
    void execute_withLiveTask_returnsTask() {
        taskRepository.seed(liveTask());

        Task result = useCase.execute(TASK_ID);

        assertEquals(TASK_ID, result.id());
    }

    // --- missing task ---

    @Test
    void execute_withUnknownTaskId_throwsTaskNotFoundException() {
        assertThrows(TaskNotFoundException.class,
                () -> useCase.execute(UNKNOWN_TASK_ID));
    }

    // --- soft-deleted task treated as not found ---

    @Test
    void execute_onSoftDeletedTask_throwsTaskNotFoundException() {
        taskRepository.seed(deletedTask());

        assertThrows(TaskNotFoundException.class,
                () -> useCase.execute(TASK_ID));
    }

    // --- null guard ---

    @Test
    void execute_withNullTaskId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }
}
