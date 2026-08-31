package dev.kairos.domain.schedule;

import dev.kairos.common.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static dev.kairos.domain.schedule.ScheduleBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleTest {

    private static final Instant UPDATE_TIME = Instant.parse("2026-02-01T00:00:00Z");

    // ── ONCE factory ─────────────────────────────────────────────────────────

    @Test
    void once_withFutureRunAt_succeeds() {
        assertDoesNotThrow(ScheduleBuilder::defaultOnce);
    }

    @Test
    void once_withFutureRunAt_typeIsONCE() {
        Schedule s = defaultOnce();

        assertEquals(ScheduleType.ONCE, s.type());
    }

    @Test
    void once_withFutureRunAt_runAtIsPreserved() {
        Schedule s = defaultOnce();

        assertEquals(FUTURE_RUN_AT, s.runAt());
    }

    @Test
    void once_withFutureRunAt_cronExpressionIsNull() {
        Schedule s = defaultOnce();

        assertNull(s.cronExpression());
    }

    @Test
    void once_withFutureRunAt_intervalSecondsIsNull() {
        Schedule s = defaultOnce();

        assertNull(s.intervalSeconds());
    }

    @Test
    void once_withNullRunAt_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Schedule.once(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL, null, FIXED_NOW));
    }

    @Test
    void once_withRunAtInThePast_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Schedule.once(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL, PAST_RUN_AT, FIXED_NOW));
    }

    @Test
    void once_withRunAtEqualToNow_throwsValidationException() {
        // runAt must be strictly after now, not equal
        assertThrows(ValidationException.class,
                () -> Schedule.once(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL, FIXED_NOW, FIXED_NOW));
    }

    // ── CRON factory ─────────────────────────────────────────────────────────

    @Test
    void cron_withValidExpressionAndTimezone_succeeds() {
        assertDoesNotThrow(ScheduleBuilder::defaultCron);
    }

    @Test
    void cron_withValidExpression_typeIsCRON() {
        Schedule s = defaultCron();

        assertEquals(ScheduleType.CRON, s.type());
    }

    @Test
    void cron_withValidExpression_cronExpressionIsPreserved() {
        Schedule s = defaultCron();

        assertEquals(DEFAULT_CRON, s.cronExpression());
    }

    @Test
    void cron_withValidExpression_runAtIsNull() {
        Schedule s = defaultCron();

        assertNull(s.runAt());
    }

    @Test
    void cron_withValidExpression_intervalSecondsIsNull() {
        Schedule s = defaultCron();

        assertNull(s.intervalSeconds());
    }

    @Test
    void cron_withBlankExpression_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Schedule.cron(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL, "   ", DEFAULT_TIMEZONE, FIXED_NOW));
    }

    @Test
    void cron_withNullExpression_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Schedule.cron(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL, null, DEFAULT_TIMEZONE, FIXED_NOW));
    }

    @Test
    void cron_withInvalidTimezone_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Schedule.cron(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL,
                        DEFAULT_CRON, "Not/ATimezone", FIXED_NOW));
    }

    @Test
    void cron_withNullTimezone_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Schedule.cron(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL,
                        DEFAULT_CRON, null, FIXED_NOW));
    }

    @Test
    void cron_withUtcTimezone_succeeds() {
        assertDoesNotThrow(() ->
                Schedule.cron(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL, DEFAULT_CRON, "UTC", FIXED_NOW));
    }

    // ── FIXED factory ────────────────────────────────────────────────────────

    @Test
    void fixed_withValidInterval_succeeds() {
        assertDoesNotThrow(ScheduleBuilder::defaultFixed);
    }

    @Test
    void fixed_withValidInterval_typeIsFIXED() {
        Schedule s = defaultFixed();

        assertEquals(ScheduleType.FIXED, s.type());
    }

    @Test
    void fixed_withValidInterval_intervalSecondsIsPreserved() {
        Schedule s = defaultFixed();

        assertEquals(DEFAULT_INTERVAL_SECONDS, s.intervalSeconds());
    }

    @Test
    void fixed_withValidInterval_runAtIsNull() {
        Schedule s = defaultFixed();

        assertNull(s.runAt());
    }

    @Test
    void fixed_withValidInterval_cronExpressionIsNull() {
        Schedule s = defaultFixed();

        assertNull(s.cronExpression());
    }

    @Test
    void fixed_withZeroInterval_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Schedule.fixed(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL, 0, FIXED_NOW));
    }

    @Test
    void fixed_withNegativeInterval_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Schedule.fixed(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL, -1, FIXED_NOW));
    }

    @Test
    void fixed_withIntervalExceedingMaxDay_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Schedule.fixed(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL,
                        Schedule.MAX_INTERVAL_SECONDS + 1, FIXED_NOW));
    }

    @Test
    void fixed_withIntervalAtMaxBoundary_succeeds() {
        assertDoesNotThrow(() ->
                Schedule.fixed(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL,
                        Schedule.MAX_INTERVAL_SECONDS, FIXED_NOW));
    }

    @Test
    void fixed_withIntervalOfOne_succeeds() {
        assertDoesNotThrow(() ->
                Schedule.fixed(DEFAULT_ID, DEFAULT_TASK_ID, DEFAULT_LABEL, 1, FIXED_NOW));
    }

    // ── common factory defaults ───────────────────────────────────────────────

    @Test
    void factory_activeDefaultsToTrue() {
        Schedule s = defaultOnce();

        assertTrue(s.active());
    }

    @Test
    void factory_createdAtEqualsNow() {
        Schedule s = defaultOnce();

        assertEquals(FIXED_NOW, s.createdAt());
    }

    @Test
    void factory_updatedAtEqualsNow() {
        Schedule s = defaultOnce();

        assertEquals(FIXED_NOW, s.updatedAt());
    }

    @Test
    void factory_idIsPreserved() {
        Schedule s = defaultOnce();

        assertEquals(DEFAULT_ID, s.id());
    }

    @Test
    void factory_taskIdIsPreserved() {
        Schedule s = defaultOnce();

        assertEquals(DEFAULT_TASK_ID, s.taskId());
    }

    @Test
    void factory_labelIsPreserved() {
        Schedule s = defaultOnce();

        assertEquals(DEFAULT_LABEL, s.label());
    }

    @Test
    void factory_nullLabelIsAccepted() {
        assertDoesNotThrow(() ->
                Schedule.once(DEFAULT_ID, DEFAULT_TASK_ID, null, FUTURE_RUN_AT, FIXED_NOW));
    }

    @Test
    void factory_labelExceedingMaxLength_throwsValidationException() {
        String tooLong = "a".repeat(Schedule.MAX_LABEL_LENGTH + 1);

        assertThrows(ValidationException.class,
                () -> Schedule.once(DEFAULT_ID, DEFAULT_TASK_ID, tooLong, FUTURE_RUN_AT, FIXED_NOW));
    }

    // ── type immutability ─────────────────────────────────────────────────────

    @Test
    void update_onOnceSchedule_typeRemainsONCE() {
        Schedule s = defaultOnce();
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, FUTURE_RUN_AT, null, null, "UTC");

        s.update(edit, UPDATE_TIME);

        assertEquals(ScheduleType.ONCE, s.type());
    }

    @Test
    void update_onCronSchedule_typeRemainsCRON() {
        Schedule s = defaultCron();
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, null, DEFAULT_CRON, null, DEFAULT_TIMEZONE);

        s.update(edit, UPDATE_TIME);

        assertEquals(ScheduleType.CRON, s.type());
    }

    @Test
    void update_onFixedSchedule_typeRemainsFIXED() {
        Schedule s = defaultFixed();
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, null, null, DEFAULT_INTERVAL_SECONDS, "UTC");

        s.update(edit, UPDATE_TIME);

        assertEquals(ScheduleType.FIXED, s.type());
    }

    // ── update() on ONCE ─────────────────────────────────────────────────────

    @Test
    void update_onOnceSchedule_withFutureRunAt_updatesRunAt() {
        Schedule s = defaultOnce();
        Instant newRunAt = Instant.parse("2027-01-01T10:00:00Z");
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, newRunAt, null, null, "UTC");

        s.update(edit, UPDATE_TIME);

        assertEquals(newRunAt, s.runAt());
    }

    @Test
    void update_onOnceSchedule_withPastRunAt_throwsValidationException() {
        Schedule s = defaultOnce();
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, PAST_RUN_AT, null, null, "UTC");

        assertThrows(ValidationException.class, () -> s.update(edit, UPDATE_TIME));
    }

    @Test
    void update_onOnceSchedule_withNullRunAt_throwsValidationException() {
        Schedule s = defaultOnce();
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, null, null, null, "UTC");

        assertThrows(ValidationException.class, () -> s.update(edit, UPDATE_TIME));
    }

    // ── update() on CRON ─────────────────────────────────────────────────────

    @Test
    void update_onCronSchedule_withNewExpression_updatesCronExpression() {
        Schedule s = defaultCron();
        String newExpression = "0 9 * * *";
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, null, newExpression, null, DEFAULT_TIMEZONE);

        s.update(edit, UPDATE_TIME);

        assertEquals(newExpression, s.cronExpression());
    }

    @Test
    void update_onCronSchedule_withBlankExpression_throwsValidationException() {
        Schedule s = defaultCron();
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, null, "   ", null, DEFAULT_TIMEZONE);

        assertThrows(ValidationException.class, () -> s.update(edit, UPDATE_TIME));
    }

    @Test
    void update_onCronSchedule_withInvalidTimezone_throwsValidationException() {
        Schedule s = defaultCron();
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, null, DEFAULT_CRON, null, "Bad/Zone");

        assertThrows(ValidationException.class, () -> s.update(edit, UPDATE_TIME));
    }

    // ── update() on FIXED ────────────────────────────────────────────────────

    @Test
    void update_onFixedSchedule_withNewInterval_updatesIntervalSeconds() {
        Schedule s = defaultFixed();
        int newInterval = 7_200;
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, null, null, newInterval, "UTC");

        s.update(edit, UPDATE_TIME);

        assertEquals(newInterval, s.intervalSeconds());
    }

    @Test
    void update_onFixedSchedule_withZeroInterval_throwsValidationException() {
        Schedule s = defaultFixed();
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, null, null, 0, "UTC");

        assertThrows(ValidationException.class, () -> s.update(edit, UPDATE_TIME));
    }

    @Test
    void update_onFixedSchedule_withIntervalExceedingMax_throwsValidationException() {
        Schedule s = defaultFixed();
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, null, null,
                Schedule.MAX_INTERVAL_SECONDS + 1, "UTC");

        assertThrows(ValidationException.class, () -> s.update(edit, UPDATE_TIME));
    }

    @Test
    void update_onFixedSchedule_withNullIntervalSeconds_throwsValidationException() {
        Schedule s = defaultFixed();
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, null, null, null, "UTC");

        assertThrows(ValidationException.class, () -> s.update(edit, UPDATE_TIME));
    }

    // ── update() bumps updatedAt and changes label ────────────────────────────

    @Test
    void update_withValidEdit_bumpsUpdatedAt() {
        Schedule s = defaultOnce();
        ScheduleEdit edit = new ScheduleEdit("new-label", FUTURE_RUN_AT, null, null, "UTC");

        s.update(edit, UPDATE_TIME);

        assertEquals(UPDATE_TIME, s.updatedAt());
    }

    @Test
    void update_withValidEdit_updatesLabel() {
        Schedule s = defaultOnce();
        String newLabel = "new-label";
        ScheduleEdit edit = new ScheduleEdit(newLabel, FUTURE_RUN_AT, null, null, "UTC");

        s.update(edit, UPDATE_TIME);

        assertEquals(newLabel, s.label());
    }

    @Test
    void update_withValidEdit_doesNotChangeCreatedAt() {
        Schedule s = defaultOnce();
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, FUTURE_RUN_AT, null, null, "UTC");

        s.update(edit, UPDATE_TIME);

        assertEquals(FIXED_NOW, s.createdAt());
    }

    @Test
    void update_withNullEdit_throwsNullPointerException() {
        Schedule s = defaultOnce();

        assertThrows(NullPointerException.class, () -> s.update(null, UPDATE_TIME));
    }

    @Test
    void update_withNullNow_throwsNullPointerException() {
        Schedule s = defaultOnce();
        ScheduleEdit edit = new ScheduleEdit(DEFAULT_LABEL, FUTURE_RUN_AT, null, null, "UTC");

        assertThrows(NullPointerException.class, () -> s.update(edit, null));
    }

    // ── pause() / resume() ───────────────────────────────────────────────────

    @Test
    void pause_setsActiveToFalse() {
        Schedule s = defaultOnce();

        s.pause(UPDATE_TIME);

        assertFalse(s.active());
    }

    @Test
    void pause_bumpsUpdatedAt() {
        Schedule s = defaultOnce();

        s.pause(UPDATE_TIME);

        assertEquals(UPDATE_TIME, s.updatedAt());
    }

    @Test
    void resume_setsActiveToTrue() {
        Schedule s = defaultOnce();
        s.pause(UPDATE_TIME);

        s.resume(UPDATE_TIME);

        assertTrue(s.active());
    }

    @Test
    void resume_bumpsUpdatedAt() {
        Schedule s = defaultOnce();
        Instant pauseTime = Instant.parse("2026-02-01T00:00:00Z");
        Instant resumeTime = Instant.parse("2026-03-01T00:00:00Z");
        s.pause(pauseTime);

        s.resume(resumeTime);

        assertEquals(resumeTime, s.updatedAt());
    }

    @Test
    void pause_onAlreadyPausedSchedule_remainsPaused() {
        Schedule s = defaultOnce();
        s.pause(UPDATE_TIME);

        s.pause(UPDATE_TIME);

        assertFalse(s.active());
    }

    @Test
    void resume_onAlreadyActiveSchedule_remainsActive() {
        Schedule s = defaultOnce();

        s.resume(UPDATE_TIME);

        assertTrue(s.active());
    }

    // ── ScheduleId ───────────────────────────────────────────────────────────

    @Test
    void scheduleId_newId_returnsNonNullUniqueId() {
        ScheduleId first = ScheduleId.newId();
        ScheduleId second = ScheduleId.newId();

        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first, second);
    }

    // ── ScheduleNotFoundException ─────────────────────────────────────────────

    @Test
    void scheduleNotFoundException_messageContainsIdValue() {
        ScheduleNotFoundException ex = new ScheduleNotFoundException(DEFAULT_ID);

        assertTrue(ex.getMessage().contains(DEFAULT_ID.value().toString()));
    }

    @Test
    void scheduleNotFoundException_scheduleIdAccessorReturnsOriginalId() {
        ScheduleNotFoundException ex = new ScheduleNotFoundException(DEFAULT_ID);

        assertEquals(DEFAULT_ID, ex.scheduleId());
    }

    // ── ScheduleType.parse() ─────────────────────────────────────────────────

    @Test
    void scheduleTypeParse_once_returnsONCE() {
        assertEquals(ScheduleType.ONCE, ScheduleType.parse("ONCE"));
    }

    @Test
    void scheduleTypeParse_cron_returnsCRON() {
        assertEquals(ScheduleType.CRON, ScheduleType.parse("CRON"));
    }

    @Test
    void scheduleTypeParse_fixed_returnsFIXED() {
        assertEquals(ScheduleType.FIXED, ScheduleType.parse("FIXED"));
    }

    @Test
    void scheduleTypeParse_unknownValue_throwsValidationException() {
        assertThrows(ValidationException.class, () -> ScheduleType.parse("MONTHLY"));
    }

    @Test
    void scheduleTypeParse_null_throwsValidationException() {
        assertThrows(ValidationException.class, () -> ScheduleType.parse(null));
    }

    @Test
    void scheduleTypeParse_blank_throwsValidationException() {
        assertThrows(ValidationException.class, () -> ScheduleType.parse("   "));
    }
}
