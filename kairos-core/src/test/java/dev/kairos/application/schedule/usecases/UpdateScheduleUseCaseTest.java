package dev.kairos.application.schedule.usecases;

import dev.kairos.application.schedule.commands.UpdateScheduleCommand;
import dev.kairos.common.exceptions.ValidationException;
import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static dev.kairos.application.schedule.usecases.UseCaseScheduleBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

class UpdateScheduleUseCaseTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private static final String UPDATED_LABEL = "updated-label";
    private static final int UPDATED_INTERVAL = 7_200;

    private InMemoryScheduleRepository scheduleRepository;
    private UpdateScheduleUseCase useCase;

    @BeforeEach
    void setUp() {
        scheduleRepository = new InMemoryScheduleRepository();
        useCase = new UpdateScheduleUseCase(scheduleRepository, FIXED_CLOCK);
    }

    // ── ONCE happy path ───────────────────────────────────────────────────────

    @Test
    void execute_onOnceSchedule_withValidCommand_returnsUpdatedSchedule() {
        scheduleRepository.seed(onceSchedule());

        Schedule result = useCase.execute(SCHEDULE_ID, validOnceUpdateCommand());

        assertNotNull(result);
    }

    @Test
    void execute_onOnceSchedule_savesAfterUpdate() {
        scheduleRepository.seed(onceSchedule());

        useCase.execute(SCHEDULE_ID, validOnceUpdateCommand());

        // seed() doesn't count; 1 save from update
        assertEquals(1, scheduleRepository.saveCallCount());
    }

    @Test
    void execute_onOnceSchedule_updatedAtEqualsClockNow() {
        scheduleRepository.seed(onceSchedule());

        Schedule result = useCase.execute(SCHEDULE_ID, validOnceUpdateCommand());

        assertEquals(FIXED_NOW, result.updatedAt());
    }

    @Test
    void execute_onOnceSchedule_labelIsUpdated() {
        scheduleRepository.seed(onceSchedule());

        Schedule result = useCase.execute(SCHEDULE_ID, validOnceUpdateCommand());

        assertEquals(UPDATED_LABEL, result.label());
    }

    @Test
    void execute_onOnceSchedule_typeRemainsONCE() {
        scheduleRepository.seed(onceSchedule());

        Schedule result = useCase.execute(SCHEDULE_ID, validOnceUpdateCommand());

        assertEquals(dev.kairos.domain.schedule.ScheduleType.ONCE, result.type());
    }

    // ── FIXED happy path ──────────────────────────────────────────────────────

    @Test
    void execute_onFixedSchedule_intervalIsUpdated() {
        scheduleRepository.seed(fixedSchedule());

        Schedule result = useCase.execute(SCHEDULE_ID, validFixedUpdateCommand());

        assertEquals(UPDATED_INTERVAL, result.intervalSeconds());
    }

    // ── CRON happy path ───────────────────────────────────────────────────────

    @Test
    void execute_onCronSchedule_cronExpressionIsUpdated() {
        scheduleRepository.seed(cronSchedule());
        String newExpression = "0 9 * * *";
        UpdateScheduleCommand command = new UpdateScheduleCommand(
                UPDATED_LABEL, null, newExpression, null, DEFAULT_TIMEZONE);

        Schedule result = useCase.execute(SCHEDULE_ID, command);

        assertEquals(newExpression, result.cronExpression());
    }

    // ── missing schedule ──────────────────────────────────────────────────────

    @Test
    void execute_withUnknownId_throwsScheduleNotFoundException() {
        assertThrows(ScheduleNotFoundException.class,
                () -> useCase.execute(UNKNOWN_SCHEDULE_ID, validOnceUpdateCommand()));
    }

    @Test
    void execute_withUnknownId_doesNotSave() {
        try {
            useCase.execute(UNKNOWN_SCHEDULE_ID, validOnceUpdateCommand());
        } catch (ScheduleNotFoundException ignored) {
        }

        assertEquals(0, scheduleRepository.saveCallCount());
    }

    // ── validation propagated from domain ────────────────────────────────────

    @Test
    void execute_onOnceSchedule_withPastRunAt_throwsValidationException() {
        scheduleRepository.seed(onceSchedule());
        UpdateScheduleCommand command = new UpdateScheduleCommand(
                DEFAULT_LABEL, PAST_RUN_AT, null, null, DEFAULT_TIMEZONE);

        assertThrows(ValidationException.class,
                () -> useCase.execute(SCHEDULE_ID, command));
    }

    @Test
    void execute_onFixedSchedule_withZeroInterval_throwsValidationException() {
        scheduleRepository.seed(fixedSchedule());
        UpdateScheduleCommand command = new UpdateScheduleCommand(
                DEFAULT_LABEL, null, null, 0, DEFAULT_TIMEZONE);

        assertThrows(ValidationException.class,
                () -> useCase.execute(SCHEDULE_ID, command));
    }

    @Test
    void execute_onFixedSchedule_withIntervalExceedingMax_throwsValidationException() {
        scheduleRepository.seed(fixedSchedule());
        UpdateScheduleCommand command = new UpdateScheduleCommand(
                DEFAULT_LABEL, null, null, 86_401, DEFAULT_TIMEZONE);

        assertThrows(ValidationException.class,
                () -> useCase.execute(SCHEDULE_ID, command));
    }

    // ── null guards ───────────────────────────────────────────────────────────

    @Test
    void execute_withNullScheduleId_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> useCase.execute(null, validOnceUpdateCommand()));
    }

    @Test
    void execute_withNullCommand_throwsNullPointerException() {
        scheduleRepository.seed(onceSchedule());

        assertThrows(NullPointerException.class,
                () -> useCase.execute(SCHEDULE_ID, null));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static UpdateScheduleCommand validOnceUpdateCommand() {
        return new UpdateScheduleCommand(UPDATED_LABEL, FUTURE_RUN_AT, null, null, DEFAULT_TIMEZONE);
    }

    private static UpdateScheduleCommand validFixedUpdateCommand() {
        return new UpdateScheduleCommand(UPDATED_LABEL, null, null, UPDATED_INTERVAL, DEFAULT_TIMEZONE);
    }
}
