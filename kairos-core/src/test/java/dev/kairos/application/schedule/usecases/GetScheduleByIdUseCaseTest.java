package dev.kairos.application.schedule.usecases;

import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dev.kairos.application.schedule.usecases.UseCaseScheduleBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

class GetScheduleByIdUseCaseTest {

    private InMemoryScheduleRepository scheduleRepository;
    private GetScheduleByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        scheduleRepository = new InMemoryScheduleRepository();
        useCase = new GetScheduleByIdUseCase(scheduleRepository);
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    void execute_withExistingId_returnsSchedule() {
        scheduleRepository.seed(onceSchedule());

        Schedule result = useCase.execute(SCHEDULE_ID);

        assertNotNull(result);
    }

    @Test
    void execute_withExistingId_returnsCorrectSchedule() {
        scheduleRepository.seed(onceSchedule());

        Schedule result = useCase.execute(SCHEDULE_ID);

        assertEquals(SCHEDULE_ID, result.id());
    }

    // ── missing schedule ──────────────────────────────────────────────────────

    @Test
    void execute_withUnknownId_throwsScheduleNotFoundException() {
        assertThrows(ScheduleNotFoundException.class,
                () -> useCase.execute(UNKNOWN_SCHEDULE_ID));
    }

    @Test
    void execute_withUnknownId_exceptionCarriesCorrectId() {
        ScheduleNotFoundException thrown = assertThrows(ScheduleNotFoundException.class,
                () -> useCase.execute(UNKNOWN_SCHEDULE_ID));

        assertEquals(UNKNOWN_SCHEDULE_ID, thrown.scheduleId());
    }

    // ── null guard ────────────────────────────────────────────────────────────

    @Test
    void execute_withNullId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }
}
