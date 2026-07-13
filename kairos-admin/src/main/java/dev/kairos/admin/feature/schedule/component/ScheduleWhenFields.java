package dev.kairos.admin.feature.schedule.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import dev.kairos.admin.feature.schedule.CronText;
import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.util.Strings;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Validation and parsing for a schedule's type-driven "when" fields (run-at,
 * interval, timezone), shared between {@link ScheduleForm} and the wizard's
 * {@code ScheduleStep} so the future-run-at rule, the interval bounds, and
 * the UTC timezone fallback are defined exactly once. Plain static methods
 * over the concrete Vaadin fields — no new interface or base class.
 */
public final class ScheduleWhenFields {

    private ScheduleWhenFields() {
    }

    /**
     * Builds the CRON row: {@code cronExpression} paired with a "Build cron"
     * button that opens the visual {@link CronBuilderDialog} and writes the
     * generated expression back into the field.
     *
     * <p>Shared by {@link ScheduleForm} and the wizard's {@code ScheduleStep} so
     * both offer the builder — callers toggle the returned row's visibility
     * instead of the bare field.
     *
     * @param cronExpression the field the builder reads its initial value from and writes back to
     * @return the row to place in the form, containing {@code cronExpression}
     */
    public static HorizontalLayout cronRow(TextField cronExpression) {
        Button build = Buttons.primary(CronText.BUILD_BUTTON,
                e -> CronBuilderDialog.open(cronExpression.getValue(), cronExpression::setValue).open());
        build.setIcon(VaadinIcon.MAGIC.create());
        build.setTooltipText(CronText.BUILD_TOOLTIP);
        build.setMinWidth(Tokens.BUTTON_MIN_WIDTH);

        cronExpression.setWidthFull();
        HorizontalLayout row = new HorizontalLayout(cronExpression, build);
        row.setAlignItems(HorizontalLayout.Alignment.END); // button bottom-aligns with the field box
        row.setWidthFull();
        row.setFlexGrow(1, cronExpression); // field fills the row
        row.setFlexShrink(0, build);        // button keeps its full label, no clipping
        return row;
    }

    /**
     * Marks {@code runAt} invalid when empty or not strictly in the future
     * (interpreted in {@code timezone}, falling back to UTC).
     *
     * @return {@code true} when the field holds a valid future instant
     */
    public static boolean validateRunAt(DateTimePicker runAt, TextField timezone) {
        if (runAt.isEmpty()) {
            runAt.setErrorMessage(UiText.VALIDATION_REQUIRED);
            runAt.setInvalid(true);
            return false;
        }
        Instant value = runAtInstant(runAt, timezone);
        boolean future = value != null && value.isAfter(Instant.now());
        runAt.setErrorMessage(ScheduleText.VALIDATION_RUN_AT_FUTURE);
        runAt.setInvalid(!future);
        return future;
    }

    /**
     * Marks {@code intervalSeconds} invalid when outside the configured bounds.
     *
     * @return {@code true} when the field holds a value within bounds
     */
    public static boolean validateInterval(IntegerField intervalSeconds) {
        Integer value = intervalSeconds.getValue();
        boolean valid = value != null && value >= ScheduleText.INTERVAL_MIN && value <= ScheduleText.INTERVAL_MAX;
        intervalSeconds.setErrorMessage(ScheduleText.VALIDATION_INTERVAL_RANGE);
        intervalSeconds.setInvalid(!valid);
        return valid;
    }

    /** Converts {@code runAt}'s wall-clock value to an {@link Instant} in {@code timezone}'s zone. */
    public static Instant runAtInstant(DateTimePicker runAt, TextField timezone) {
        LocalDateTime value = runAt.getValue();
        return value == null ? null : value.atZone(selectedZone(timezone)).toInstant();
    }

    /**
     * Resolves the timezone entered in {@code timezone}, so the ONCE wall-clock
     * picker is interpreted in that zone rather than the admin JVM's system
     * zone. Falls back to UTC when the field is blank or not a valid {@link ZoneId}.
     */
    public static ZoneId selectedZone(TextField timezone) {
        String zone = Strings.trimToNull(timezone.getValue());
        if (zone == null) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(zone);
        } catch (DateTimeException ex) {
            return ZoneOffset.UTC;
        }
    }
}
