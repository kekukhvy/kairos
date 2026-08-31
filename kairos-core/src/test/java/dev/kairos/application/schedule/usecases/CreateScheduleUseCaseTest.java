package dev.kairos.application.schedule.usecases;

import dev.kairos.application.schedule.commands.CreateScheduleCommand;
import dev.kairos.common.exceptions.ValidationException;
import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleType;
import dev.kairos.domain.task.TaskNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static dev.kairos.application.schedule.usecases.UseCaseScheduleBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

class CreateScheduleUseCaseTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    private static final String UNKNOWN_TASK_ID = "ffffffff-0000-0000-0000-000000000099";

    private InMemoryScheduleRepository scheduleRepository;
    private StubTaskRepositoryForSchedule taskRepository;
    private CreateScheduleUseCase useCase;

    @BeforeEach
    void setUp() {
        scheduleRepository = new InMemoryScheduleRepository();
        taskRepository = new StubTaskRepositoryForSchedule();
        taskRepository.seed(liveTask());
        useCase = new CreateScheduleUseCase(scheduleRepository, taskRepository, FIXED_CLOCK);
    }

    // ── ONCE happy path ───────────────────────────────────────────────────────

    @Test
    void execute_onceWithValidCommand_returnsSchedule() {
        Schedule result = useCase.execute(validOnceCommand());

        assertNotNull(result);
    }

    @Test
    void execute_onceWithValidCommand_savesSchedule() {
        useCase.execute(validOnceCommand());

        assertEquals(1, scheduleRepository.saveCallCount());
    }

    @Test
    void execute_onceWithValidCommand_typeIsONCE() {
        Schedule result = useCase.execute(validOnceCommand());

        assertEquals(ScheduleType.ONCE, result.type());
    }

    @Test
    void execute_onceWithValidCommand_idIsGenerated() {
        Schedule result = useCase.execute(validOnceCommand());

        assertNotNull(result.id());
        assertNotNull(result.id().value());
    }

    @Test
    void execute_onceWithValidCommand_taskIdMatchesCommand() {
        Schedule result = useCase.execute(validOnceCommand());

        assertEquals(TASK_ID, result.taskId());
    }

    @Test
    void execute_onceWithValidCommand_timestampsEqualClock() {
        Schedule result = useCase.execute(validOnceCommand());

        assertEquals(FIXED_NOW, result.createdAt());
        assertEquals(FIXED_NOW, result.updatedAt());
    }

    @Test
    void execute_onceWithValidCommand_activeDefaultsToTrue() {
        Schedule result = useCase.execute(validOnceCommand());

        assertTrue(result.active());
    }

    @Test
    void execute_onceWithValidCommand_runAtIsPreserved() {
        Schedule result = useCase.execute(validOnceCommand());

        assertEquals(FUTURE_RUN_AT, result.runAt());
    }

    // ── CRON happy path ───────────────────────────────────────────────────────

    @Test
    void execute_cronWithValidCommand_typeIsCRON() {
        Schedule result = useCase.execute(validCronCommand());

        assertEquals(ScheduleType.CRON, result.type());
    }

    @Test
    void execute_cronWithValidCommand_cronExpressionIsPreserved() {
        Schedule result = useCase.execute(validCronCommand());

        assertEquals(DEFAULT_CRON, result.cronExpression());
    }

    // ── FIXED happy path ──────────────────────────────────────────────────────

    @Test
    void execute_fixedWithValidCommand_typeIsFIXED() {
        Schedule result = useCase.execute(validFixedCommand());

        assertEquals(ScheduleType.FIXED, result.type());
    }

    @Test
    void execute_fixedWithValidCommand_intervalSecondsIsPreserved() {
        Schedule result = useCase.execute(validFixedCommand());

        assertEquals(DEFAULT_INTERVAL_SECONDS, result.intervalSeconds());
    }

    // ── task existence guard ──────────────────────────────────────────────────

    @Test
    void execute_withUnknownTaskId_throwsTaskNotFoundException() {
        CreateScheduleCommand command = new CreateScheduleCommand(
                UNKNOWN_TASK_ID, "ONCE", DEFAULT_LABEL, FUTURE_RUN_AT, null, null, DEFAULT_TIMEZONE);

        assertThrows(TaskNotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    void execute_withUnknownTaskId_doesNotSave() {
        CreateScheduleCommand command = new CreateScheduleCommand(
                UNKNOWN_TASK_ID, "ONCE", DEFAULT_LABEL, FUTURE_RUN_AT, null, null, DEFAULT_TIMEZONE);

        try {
            useCase.execute(command);
        } catch (TaskNotFoundException ignored) {
        }

        assertEquals(0, scheduleRepository.saveCallCount());
    }

    @Test
    void execute_withDeletedTask_throwsTaskNotFoundException() {
        taskRepository.seed(deletedTask());
        // Rebuild useCase with a repo that has only the deleted task
        StubTaskRepositoryForSchedule deletedOnlyRepo = new StubTaskRepositoryForSchedule();
        deletedOnlyRepo.seed(deletedTask());
        CreateScheduleUseCase useCaseWithDeletedTask =
                new CreateScheduleUseCase(scheduleRepository, deletedOnlyRepo, FIXED_CLOCK);

        assertThrows(TaskNotFoundException.class,
                () -> useCaseWithDeletedTask.execute(validOnceCommand()));
    }

    // ── invalid type ──────────────────────────────────────────────────────────

    @Test
    void execute_withUnknownType_throwsValidationException() {
        CreateScheduleCommand command = new CreateScheduleCommand(
                TASK_ID.value().toString(), "WEEKLY", DEFAULT_LABEL, null, null, null, DEFAULT_TIMEZONE);

        assertThrows(ValidationException.class, () -> useCase.execute(command));
    }

    // ── ONCE validation ───────────────────────────────────────────────────────

    @Test
    void execute_onceWithPastRunAt_throwsValidationException() {
        CreateScheduleCommand command = new CreateScheduleCommand(
                TASK_ID.value().toString(), "ONCE", DEFAULT_LABEL, PAST_RUN_AT, null, null, DEFAULT_TIMEZONE);

        assertThrows(ValidationException.class, () -> useCase.execute(command));
    }

    @Test
    void execute_onceWithNullRunAt_throwsValidationException() {
        CreateScheduleCommand command = new CreateScheduleCommand(
                TASK_ID.value().toString(), "ONCE", DEFAULT_LABEL, null, null, null, DEFAULT_TIMEZONE);

        assertThrows(ValidationException.class, () -> useCase.execute(command));
    }

    // ── FIXED validation ──────────────────────────────────────────────────────

    @Test
    void execute_fixedWithZeroInterval_throwsValidationException() {
        CreateScheduleCommand command = new CreateScheduleCommand(
                TASK_ID.value().toString(), "FIXED", DEFAULT_LABEL, null, null, 0, DEFAULT_TIMEZONE);

        assertThrows(ValidationException.class, () -> useCase.execute(command));
    }

    @Test
    void execute_fixedWithNullInterval_throwsValidationException() {
        // null interval gets converted to 0 by intervalOrZero(), which fails the > 0 check
        CreateScheduleCommand command = new CreateScheduleCommand(
                TASK_ID.value().toString(), "FIXED", DEFAULT_LABEL, null, null, null, DEFAULT_TIMEZONE);

        assertThrows(ValidationException.class, () -> useCase.execute(command));
    }

    @Test
    void execute_fixedWithIntervalExceedingMax_throwsValidationException() {
        CreateScheduleCommand command = new CreateScheduleCommand(
                TASK_ID.value().toString(), "FIXED", DEFAULT_LABEL, null, null, 86_401, DEFAULT_TIMEZONE);

        assertThrows(ValidationException.class, () -> useCase.execute(command));
    }

    // ── CRON validation ───────────────────────────────────────────────────────

    @Test
    void execute_cronWithBlankExpression_throwsValidationException() {
        CreateScheduleCommand command = new CreateScheduleCommand(
                TASK_ID.value().toString(), "CRON", DEFAULT_LABEL, null, "   ", null, DEFAULT_TIMEZONE);

        assertThrows(ValidationException.class, () -> useCase.execute(command));
    }

    @Test
    void execute_cronWithInvalidTimezone_throwsValidationException() {
        CreateScheduleCommand command = new CreateScheduleCommand(
                TASK_ID.value().toString(), "CRON", DEFAULT_LABEL, null, DEFAULT_CRON, null, "Bad/Zone");

        assertThrows(ValidationException.class, () -> useCase.execute(command));
    }

    // ── null guard ────────────────────────────────────────────────────────────

    @Test
    void execute_withNullCommand_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static CreateScheduleCommand validOnceCommand() {
        return new CreateScheduleCommand(
                TASK_ID.value().toString(), "ONCE", DEFAULT_LABEL,
                FUTURE_RUN_AT, null, null, DEFAULT_TIMEZONE);
    }

    private static CreateScheduleCommand validCronCommand() {
        return new CreateScheduleCommand(
                TASK_ID.value().toString(), "CRON", DEFAULT_LABEL,
                null, DEFAULT_CRON, null, DEFAULT_TIMEZONE);
    }

    private static CreateScheduleCommand validFixedCommand() {
        return new CreateScheduleCommand(
                TASK_ID.value().toString(), "FIXED", DEFAULT_LABEL,
                null, null, DEFAULT_INTERVAL_SECONDS, DEFAULT_TIMEZONE);
    }
}
