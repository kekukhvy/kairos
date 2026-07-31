package dev.kairos.admin.feature.schedule.component;

import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.admin.feature.schedule.dto.ScheduleType;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.common.dto.schedule.CreateScheduleRequest;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import dev.kairos.common.dto.schedule.UpdateScheduleRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the timezone picker in {@code ScheduleForm}: a searchable drop-down of
 * common IANA zones ({@link ScheduleText#TIMEZONE_OPTIONS}) that still accepts
 * a hand-typed zone, defaults to UTC on create, and — the defect this closes —
 * rejects an unparseable free-typed zone instead of silently falling back to
 * UTC (which used to let a typo pass client-side only to be rejected by the
 * API with a 400).
 */
class ScheduleFormTest {

    private static final UUID TASK_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    /** Far enough ahead to stay in the future in any timezone's wall-clock reinterpretation. */
    private static final Instant FAR_FUTURE = Instant.now().plus(Duration.ofDays(2));

    @Test
    void forCreate_defaultsTimezoneToUtc() {
        ScheduleForm form = ScheduleForm.forCreate(List.of(task()), noopCreate());

        assertThat(form.timezone().getValue()).isEqualTo(ScheduleText.DEFAULT_TIMEZONE);
    }

    @Test
    void forCreate_timezoneOffersSharedShortlist() {
        ScheduleForm form = ScheduleForm.forCreate(List.of(task()), noopCreate());

        assertThat(form.timezone().getListDataView().getItems().toList())
                .isEqualTo(ScheduleText.TIMEZONE_OPTIONS);
    }

    @Test
    void forCreate_shortlistZone_selectable() {
        ScheduleForm form = ScheduleForm.forCreate(List.of(task()), noopCreate());

        form.timezone().setValue("Europe/Kyiv");

        assertThat(form.timezone().getValue()).isEqualTo("Europe/Kyiv");
    }

    @Test
    void forCreate_validCustomZoneOffShortlist_acceptedByValidate() {
        ScheduleForm form = ScheduleForm.forCreate(List.of(task()), noopCreate());
        form.task().setValue(task());
        form.type().setValue(ScheduleType.ONCE);
        form.runAt().setValue(LocalDateTime.ofInstant(FAR_FUTURE, ZoneOffset.UTC));
        form.timezone().setValue("Pacific/Chatham");

        assertThat(form.validate()).isTrue();
        assertThat(form.timezone().isInvalid()).isFalse();
    }

    @Test
    void forCreate_invalidCustomZone_blocksValidateAndMarksInvalid() {
        ScheduleForm form = ScheduleForm.forCreate(List.of(task()), noopCreate());
        form.task().setValue(task());
        form.type().setValue(ScheduleType.ONCE);
        form.runAt().setValue(LocalDateTime.ofInstant(FAR_FUTURE, ZoneOffset.UTC));
        form.timezone().setValue("Europe/Kyv");

        assertThat(form.validate()).isFalse();
        assertThat(form.timezone().isInvalid()).isTrue();
        assertThat(form.timezone().getErrorMessage()).isEqualTo(ScheduleText.VALIDATION_TIMEZONE_INVALID);
    }

    @Test
    void forCreate_fixedType_timezoneFieldHidden() {
        ScheduleForm form = ScheduleForm.forCreate(List.of(task()), noopCreate());

        form.type().setValue(ScheduleType.FIXED);

        assertThat(form.timezone().isVisible()).isFalse();
    }

    @Test
    void forCreate_fixedType_invalidTimezoneDoesNotBlockValidate() {
        ScheduleForm form = ScheduleForm.forCreate(List.of(task()), noopCreate());
        form.task().setValue(task());
        form.type().setValue(ScheduleType.FIXED);
        form.timezone().setValue("Europe/Kyv");
        form.intervalSeconds().setValue(60);

        assertThat(form.validate()).isTrue();
    }

    @Test
    void forEdit_preselectsStoredShortlistTimezone() {
        ScheduleForm form = ScheduleForm.forEdit(scheduleWithTimezone("Europe/Kyiv"), noopUpdate());

        assertThat(form.timezone().getValue()).isEqualTo("Europe/Kyiv");
    }

    @Test
    void forEdit_preselectsStoredOffShortlistTimezone() {
        ScheduleForm form = ScheduleForm.forEdit(scheduleWithTimezone("Pacific/Chatham"), noopUpdate());

        assertThat(form.timezone().getValue()).isEqualTo("Pacific/Chatham");
    }

    @Test
    void forEdit_submittingUnchangedOffShortlistTimezone_roundTripsUnchanged() {
        ScheduleResponse editing = scheduleWithTimezone("Pacific/Chatham");
        UpdateScheduleRequest[] captured = new UpdateScheduleRequest[1];
        ScheduleForm form = ScheduleForm.forEdit(editing, request -> captured[0] = request);

        assertThat(form.validate()).isTrue();
        form.save();

        assertThat(captured[0].timezone()).isEqualTo("Pacific/Chatham");
    }

    private static ScheduleResponse scheduleWithTimezone(String timezone) {
        return new ScheduleResponse(UUID.randomUUID(), TASK_ID, "CRON", "label", null,
                "0 0 * * *", null, timezone, true, NOW, NOW);
    }

    private static TaskDto task() {
        return new TaskDto(TASK_ID, "billing", "invoice-sync", null, true,
                "dest-1", "InvoiceReady", null, 5000, false, NOW, NOW, 0);
    }

    private static BiConsumer<UUID, CreateScheduleRequest> noopCreate() {
        return (taskId, request) -> { };
    }

    private static Consumer<UpdateScheduleRequest> noopUpdate() {
        return request -> { };
    }
}
