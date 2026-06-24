package dev.kairos.application.task.usecases;

import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskAlreadyDeletedException;
import dev.kairos.domain.task.TaskId;
import dev.kairos.domain.task.TaskNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;

import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.FIXED_NOW;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.TASK_ID;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.deletedTask;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.liveTask;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoftDeleteTaskUseCaseTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private static final TaskId UNKNOWN_TASK_ID =
            new TaskId(UUID.fromString("ffffffff-0000-0000-0000-000000000099"));

    private InMemoryTaskRepository taskRepository;
    private SoftDeleteTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        taskRepository = new InMemoryTaskRepository();
        useCase = new SoftDeleteTaskUseCase(taskRepository, FIXED_CLOCK);
    }

    // --- happy path ---

    @Test
    void execute_onLiveTask_marksTaskAsDeleted() {
        taskRepository.seed(liveTask());

        useCase.execute(TASK_ID);

        Task stored = taskRepository.findById(TASK_ID).orElseThrow();
        assertTrue(stored.isDeleted());
    }

    @Test
    void execute_onLiveTask_stampsDeletedAtFromClock() {
        taskRepository.seed(liveTask());

        useCase.execute(TASK_ID);

        Task stored = taskRepository.findById(TASK_ID).orElseThrow();
        assertEquals(FIXED_NOW, stored.deletedAt());
    }

    // --- missing task ---

    @Test
    void execute_withUnknownTaskId_throwsTaskNotFoundException() {
        assertThrows(TaskNotFoundException.class,
                () -> useCase.execute(UNKNOWN_TASK_ID));
    }

    // --- already-deleted task (409) ---

    @Test
    void execute_onAlreadyDeletedTask_throwsTaskAlreadyDeletedException() {
        taskRepository.seed(deletedTask());

        assertThrows(TaskAlreadyDeletedException.class,
                () -> useCase.execute(TASK_ID));
    }

    // --- null guard ---

    @Test
    void execute_withNullTaskId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }
}
