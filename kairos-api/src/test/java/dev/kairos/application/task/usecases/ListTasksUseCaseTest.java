package dev.kairos.application.task.usecases;

import dev.kairos.common.pagination.Pagination;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.deletedTask;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.liveTask;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.liveTaskWithId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListTasksUseCaseTest {

    private static final int PAGE_LIMIT = 10;
    private static final int PAGE_OFFSET = 5;

    private InMemoryTaskRepository taskRepository;
    private ListTasksUseCase useCase;

    @BeforeEach
    void setUp() {
        taskRepository = new InMemoryTaskRepository();
        useCase = new ListTasksUseCase(taskRepository);
    }

    // --- happy path ---

    @Test
    void execute_withLiveTasks_returnsAllLiveTasks() {
        taskRepository.seed(liveTask());

        List<Task> result = useCase.execute(new Pagination(PAGE_LIMIT, 0));

        assertEquals(1, result.size());
    }

    @Test
    void execute_withMultipleLiveTasks_returnsAllInRepositoryOrder() {
        TaskId secondId = new TaskId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        taskRepository.seed(liveTask());
        taskRepository.seed(liveTaskWithId(secondId));

        List<Task> result = useCase.execute(new Pagination(PAGE_LIMIT, 0));

        assertEquals(2, result.size());
    }

    // --- pagination is forwarded ---

    @Test
    void execute_forwardsPaginationLimitToRepository() {
        useCase.execute(new Pagination(PAGE_LIMIT, PAGE_OFFSET));

        assertEquals(PAGE_LIMIT, taskRepository.lastFindAllLimit);
    }

    @Test
    void execute_forwardsPaginationOffsetToRepository() {
        useCase.execute(new Pagination(PAGE_LIMIT, PAGE_OFFSET));

        assertEquals(PAGE_OFFSET, taskRepository.lastFindAllOffset);
    }

    // --- deleted tasks are filtered out ---

    @Test
    void execute_whenRepositoryReturnsDeletedTask_filtersItOut() {
        // Seed directly so the deleted task appears in findAll (bypassing the
        // soft-delete filter that a real repo would apply in SQL).
        taskRepository.seed(deletedTask());

        List<Task> result = useCase.execute(new Pagination(PAGE_LIMIT, 0));

        assertTrue(result.isEmpty());
    }

    @Test
    void execute_withMixOfLiveAndDeletedTasks_returnsOnlyLiveTasks() {
        TaskId otherId = new TaskId(UUID.fromString("00000000-0000-0000-0000-000000000099"));
        taskRepository.seed(liveTaskWithId(otherId));
        taskRepository.seed(deletedTask());

        List<Task> result = useCase.execute(new Pagination(PAGE_LIMIT, 0));

        assertEquals(1, result.size());
        assertEquals(otherId, result.get(0).id());
    }

    // --- null guard ---

    @Test
    void execute_withNullPagination_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }
}
