package dev.kairos.application.task.usecases;

import dev.kairos.common.pagination.Pagination;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.liveTask;
import static dev.kairos.application.task.usecases.UseCaseTaskBuilder.liveTaskWithId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void execute_withLiveTask_returnsItFromRepository() {
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

    // --- use case returns whatever the repository returns (no app-layer filtering) ---

    @Test
    void execute_delegatesCompletelyToRepository_returnsRepositoryResultUnmodified() {
        // The use case no longer filters — soft-delete exclusion happens in the
        // repository SQL (deleted_at IS NULL). The in-memory fake's findAll returns
        // whatever was seeded; here we verify the use case passes that through intact.
        TaskId firstId = new TaskId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        TaskId secondId = new TaskId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        taskRepository.seed(liveTask());
        taskRepository.seed(liveTaskWithId(secondId));

        List<Task> result = useCase.execute(new Pagination(PAGE_LIMIT, 0));

        assertEquals(2, result.size());
        assertEquals(firstId, result.get(0).id());
        assertEquals(secondId, result.get(1).id());
    }

    // --- null guard ---

    @Test
    void execute_withNullPagination_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }
}
