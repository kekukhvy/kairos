package dev.kairos.application.schedule.usecases;

import dev.kairos.domain.schedule.ScheduleNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static dev.kairos.application.schedule.usecases.UseCaseScheduleBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

class DeleteScheduleUseCaseTest {

    private InMemoryScheduleRepository scheduleRepository;
    private DeleteScheduleUseCase useCase;

    @BeforeEach
    void setUp() {
        scheduleRepository = new InMemoryScheduleRepository();
        useCase = new DeleteScheduleUseCase(scheduleRepository);
    }

    // ── happy path ────────────────────────────────────────────────────────────

    @Test
    void execute_withExistingSchedule_removesItFromRepository() {
        scheduleRepository.seed(onceSchedule());

        useCase.execute(SCHEDULE_ID);

        assertFalse(scheduleRepository.contains(SCHEDULE_ID));
    }

    @Test
    void execute_withExistingSchedule_doesNotThrow() {
        scheduleRepository.seed(onceSchedule());

        assertDoesNotThrow(() -> useCase.execute(SCHEDULE_ID));
    }

    // ── idempotency (delete of absent schedule is not an error) ──────────────

    @Test
    void execute_withAbsentSchedule_doesNotThrow() {
        assertDoesNotThrow(() -> useCase.execute(UNKNOWN_SCHEDULE_ID));
    }

    @Test
    void execute_calledTwice_doesNotThrow() {
        scheduleRepository.seed(onceSchedule());
        useCase.execute(SCHEDULE_ID);

        assertDoesNotThrow(() -> useCase.execute(SCHEDULE_ID));
    }

    // ── null guard ────────────────────────────────────────────────────────────

    @Test
    void execute_withNullId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }
}
