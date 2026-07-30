package dev.kairos.admin.feature.schedule.component;

import com.vaadin.flow.component.combobox.ComboBox;
import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.admin.feature.schedule.dto.ScheduleType;
import dev.kairos.admin.shared.ui.Fields;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ScheduleWhenFields}' timezone handling: resolving a
 * {@link ComboBox} value to a {@link ZoneId} and validating a free-typed zone
 * that is not a real {@link ZoneId} — the defect being fixed is a silent UTC
 * fallback that let a typo like {@code Europe/Kyv} pass client-side validation
 * only to be rejected by the API.
 */
class ScheduleWhenFieldsTest {

    @Test
    void selectedZone_blankValue_returnsUtc() {
        ComboBox<String> timezone = timezoneField();

        assertThat(ScheduleWhenFields.selectedZone(timezone)).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void selectedZone_validZone_returnsThatZone() {
        ComboBox<String> timezone = timezoneField();
        timezone.setValue("Europe/Kyiv");

        assertThat(ScheduleWhenFields.selectedZone(timezone)).isEqualTo(ZoneId.of("Europe/Kyiv"));
    }

    @Test
    void validateTimezone_blankValue_isValid() {
        ComboBox<String> timezone = timezoneField();

        assertThat(ScheduleWhenFields.validateTimezone(timezone)).isTrue();
        assertThat(timezone.isInvalid()).isFalse();
    }

    @Test
    void validateTimezone_validShortlistZone_isValid() {
        ComboBox<String> timezone = timezoneField();
        timezone.setValue("Europe/Kyiv");

        assertThat(ScheduleWhenFields.validateTimezone(timezone)).isTrue();
        assertThat(timezone.isInvalid()).isFalse();
    }

    @Test
    void validateTimezone_validCustomZoneOffShortlist_isValid() {
        ComboBox<String> timezone = timezoneField();
        timezone.setValue("Pacific/Chatham");

        assertThat(ScheduleWhenFields.validateTimezone(timezone)).isTrue();
        assertThat(timezone.isInvalid()).isFalse();
    }

    @Test
    void validateTimezone_invalidTypo_marksFieldInvalidAndBlocks() {
        ComboBox<String> timezone = timezoneField();
        timezone.setValue("Europe/Kyv");

        assertThat(ScheduleWhenFields.validateTimezone(timezone)).isFalse();
        assertThat(timezone.isInvalid()).isTrue();
        assertThat(timezone.getErrorMessage()).isEqualTo(ScheduleText.VALIDATION_TIMEZONE_INVALID);
    }

    @Test
    void validateTimezoneIfApplicable_fixedType_invalidValueDoesNotBlock() {
        ComboBox<String> timezone = timezoneField();
        timezone.setValue("Europe/Kyv");

        assertThat(ScheduleWhenFields.validateTimezoneIfApplicable(timezone, ScheduleType.FIXED)).isTrue();
    }

    @Test
    void validateTimezoneIfApplicable_onceType_invalidValueBlocks() {
        ComboBox<String> timezone = timezoneField();
        timezone.setValue("Europe/Kyv");

        assertThat(ScheduleWhenFields.validateTimezoneIfApplicable(timezone, ScheduleType.ONCE)).isFalse();
    }

    private static ComboBox<String> timezoneField() {
        return Fields.comboCustom(
                ScheduleText.COL_TIMEZONE, ScheduleText.HELPER_TIMEZONE, ScheduleText.TIMEZONE_OPTIONS);
    }
}
