package dev.kairos.application.schedule.usecases;

import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static dev.kairos.application.schedule.usecases.UseCaseScheduleBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

class SetScheduleActiveUseCaseTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private InMemoryScheduleRepository scheduleRepository;
    private SetScheduleActiveUseCase useCase;

    @BeforeEach
    void setUp() {
        scheduleRepository = new InMemoryScheduleRepository();
        useCase = new SetScheduleActiveUseCase(scheduleRepository, FIXED_CLOCK);
    }

    // ── pause (active = false) ────────────────────────────────────────────────

    @Test
    void execute_pauseOnActiveSchedule_setsActiveToFalse() {
        scheduleRepository.seed(onceSchedule());

        Schedule result = useCase.execute(SCHEDULE_ID, false);

        assertFalse(result.active());
    }

    @Test
    void execute_pauseOnActiveSchedule_savesSchedule() {
        scheduleRepository.seed(onceSchedule());

        useCase.execute(SCHEDULE_ID, false);

        assertEquals(1, scheduleRepository.saveCallCount());
    }

    @Test
    void execute_pauseOnActiveSchedule_updatedAtEqualsClockNow() {
        scheduleRepository.seed(onceSchedule());

        Schedule result = useCase.execute(SCHEDULE_ID, false);

        assertEquals(FIXED_NOW, result.updatedAt());
    }

    // ── resume (active = true) ────────────────────────────────────────────────

    @Test
    void execute_resumeOnPausedSchedule_setsActiveToTrue() {
        Schedule paused = onceSchedule();
        paused.pause(FIXED_NOW);
        scheduleRepository.seed(paused);

        Schedule result = useCase.execute(SCHEDULE_ID, true);

        assertTrue(result.active());
    }

    @Test
    void execute_resumeOnPausedSchedule_savesSchedule() {
        Schedule paused = onceSchedule();
        paused.pause(FIXED_NOW);
        scheduleRepository.seed(paused);

        useCase.execute(SCHEDULE_ID, true);

        assertEquals(1, scheduleRepository.saveCallCount());
    }

    @Test
    void execute_resumeOnPausedSchedule_updatedAtEqualsClockNow() {
        Schedule paused = onceSchedule();
        paused.pause(FIXED_NOW);
        scheduleRepository.seed(paused);

        Schedule result = useCase.execute(SCHEDULE_ID, true);

        assertEquals(FIXED_NOW, result.updatedAt());
    }

    // ── idempotency ───────────────────────────────────────────────────────────

    @Test
    void execute_pauseAlreadyPausedSchedule_remainsPaused() {
        Schedule paused = onceSchedule();
        paused.pause(FIXED_NOW);
        scheduleRepository.seed(paused);

        Schedule result = useCase.execute(SCHEDULE_ID, false);

        assertFalse(result.active());
    }

    @Test
    void execute_resumeAlreadyActiveSchedule_remainsActive() {
        scheduleRepository.seed(onceSchedule());

        Schedule result = useCase.execute(SCHEDULE_ID, true);

        assertTrue(result.active());
    }

    // ── missing schedule ──────────────────────────────────────────────────────

    @Test
    void execute_pause_withUnknownId_throwsScheduleNotFoundException() {
        assertThrows(ScheduleNotFoundException.class,
                () -> useCase.execute(UNKNOWN_SCHEDULE_ID, false));
    }

    @Test
    void execute_resume_withUnknownId_throwsScheduleNotFoundException() {
        assertThrows(ScheduleNotFoundException.class,
                () -> useCase.execute(UNKNOWN_SCHEDULE_ID, true));
    }

    @Test
    void execute_withUnknownId_doesNotSave() {
        try {
            useCase.execute(UNKNOWN_SCHEDULE_ID, false);
        } catch (ScheduleNotFoundException ignored) {
        }

        assertEquals(0, scheduleRepository.saveCallCount());
    }

    // ── null guard ────────────────────────────────────────────────────────────

    @Test
    void execute_withNullScheduleId_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null, false));
    }
}
