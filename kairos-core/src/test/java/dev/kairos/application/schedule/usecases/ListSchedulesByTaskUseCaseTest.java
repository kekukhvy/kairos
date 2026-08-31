package dev.kairos.application.schedule.usecases;

import dev.kairos.common.pagination.Pagination;
import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.task.TaskId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static dev.kairos.application.schedule.usecases.UseCaseScheduleBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

class ListSchedulesByTaskUseCaseTest {

    private static final int LIMIT_TEN = 10;
    private static final int LIMIT_ONE = 1;
    private static final int OFFSET_ZERO = 0;
    private static final int OFFSET_ONE = 1;

    private InMemoryScheduleRepository scheduleRepository;
    private ListSchedulesByTaskUseCase useCase;

    @BeforeEach
    void setUp() {
        scheduleRepository = new InMemoryScheduleRepository();
        useCase = new ListSchedulesByTaskUseCase(scheduleRepository);
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    void execute_withNoSchedulesForTask_returnsEmptyList() {
        List<Schedule> result = useCase.execute(TASK_ID, Pagination.of(LIMIT_TEN, OFFSET_ZERO));

        assertTrue(result.isEmpty());
    }

    @Test
    void execute_withOneSchedule_returnsOneItem() {
        scheduleRepository.seed(onceSchedule());

        List<Schedule> result = useCase.execute(TASK_ID, Pagination.of(LIMIT_TEN, OFFSET_ZERO));

        assertEquals(1, result.size());
    }

    @Test
    void execute_withOneSchedule_itemBelongsToCorrectTask() {
        scheduleRepository.seed(onceSchedule());

        List<Schedule> result = useCase.execute(TASK_ID, Pagination.of(LIMIT_TEN, OFFSET_ZERO));

        assertEquals(TASK_ID, result.get(0).taskId());
    }

    @Test
    void execute_filtersSchedulesByTaskId() {
        TaskId otherTask = new TaskId(UUID.fromString("99999999-0000-0000-0000-000000000001"));
        scheduleRepository.seed(onceSchedule());                          // belongs to TASK_ID
        scheduleRepository.seed(onceScheduleForTask(otherTask));          // different task

        List<Schedule> result = useCase.execute(TASK_ID, Pagination.of(LIMIT_TEN, OFFSET_ZERO));

        assertEquals(1, result.size());
        assertEquals(TASK_ID, result.get(0).taskId());
    }

    // ── pagination ────────────────────────────────────────────────────────────

    @Test
    void execute_respectsLimit() {
        scheduleRepository.seed(onceScheduleWithId(ScheduleId.newId()));
        scheduleRepository.seed(onceScheduleWithId(ScheduleId.newId()));

        List<Schedule> result = useCase.execute(TASK_ID, Pagination.of(LIMIT_ONE, OFFSET_ZERO));

        assertEquals(1, result.size());
    }

    @Test
    void execute_respectsOffset() {
        scheduleRepository.seed(onceScheduleWithId(ScheduleId.newId()));
        scheduleRepository.seed(onceScheduleWithId(ScheduleId.newId()));

        List<Schedule> result = useCase.execute(TASK_ID, Pagination.of(LIMIT_TEN, OFFSET_ONE));

        assertEquals(1, result.size());
    }

    // ── null guards ───────────────────────────────────────────────────────────

    @Test
    void execute_withNullTaskId_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> useCase.execute(null, Pagination.of(LIMIT_TEN, OFFSET_ZERO)));
    }

    @Test
    void execute_withNullPagination_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> useCase.execute(TASK_ID, null));
    }
}
