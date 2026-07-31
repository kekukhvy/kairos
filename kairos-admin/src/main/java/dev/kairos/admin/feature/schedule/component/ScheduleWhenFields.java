package dev.kairos.admin.feature.schedule.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import dev.kairos.admin.feature.schedule.CronText;
import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.admin.feature.schedule.dto.ScheduleType;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Fields;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Validation and parsing for a schedule's type-driven "when" fields (run-at,
 * interval, timezone), shared between {@link ScheduleForm} and the wizard's
 * {@code ScheduleStep} so the future-run-at rule, the interval bounds, and the
 * timezone validity check are defined exactly once. Plain static methods over
 * the concrete Vaadin fields — no new interface or base class.
 */
public final class ScheduleWhenFields {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleWhenFields.class);

    private ScheduleWhenFields() {
    }

    /**
     * Builds the timezone picker: the shared shortlist from
     * {@link ScheduleText#TIMEZONE_OPTIONS}, rendered with each zone's current
     * UTC offset, still accepting any free-typed valid zone.
     *
     * <p>Created here rather than at each call site so {@link ScheduleForm} and
     * the wizard's {@code ScheduleStep} cannot drift apart in either the option
     * list or the offset rendering.
     *
     * @return a combo whose <em>value</em> is always the bare zone id (e.g.
     *         {@code Europe/Vienna}), so it round-trips to the API unchanged —
     *         only the displayed label carries the offset
     */
    public static ComboBox<String> timezoneField() {
        ComboBox<String> field = Fields.comboCustom(
                ScheduleText.COL_TIMEZONE, ScheduleText.HELPER_TIMEZONE, ScheduleText.TIMEZONE_OPTIONS);
        field.setItemLabelGenerator(ScheduleWhenFields::zoneLabel);
        return field;
    }

    /**
     * Renders {@code zone} with its offset as it is <em>right now</em>, e.g.
     * {@code Europe/Vienna (UTC+02:00)}. The offset is computed per call rather
     * than stored alongside the zone id because it shifts with daylight saving
     * time — a hard-coded offset would be wrong for half the year.
     *
     * <p>A zone id that no longer parses is returned as-is rather than throwing,
     * so a stored legacy value still renders in the picker.
     */
    static String zoneLabel(String zone) {
        try {
            ZoneOffset offset = ZoneId.of(zone).getRules().getOffset(Instant.now());
            return zone + ScheduleText.TIMEZONE_OFFSET_PREFIX + offsetLabel(offset) + ScheduleText.TIMEZONE_OFFSET_SUFFIX;
        } catch (DateTimeException ex) {
            return zone;
        }
    }

    /** {@link ZoneOffset#getId()} renders UTC as {@code "Z"}; spell it out as {@code +00:00} instead. */
    private static String offsetLabel(ZoneOffset offset) {
        return offset.getTotalSeconds() == 0 ? ScheduleText.TIMEZONE_OFFSET_ZERO : offset.getId();
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
    public static boolean validateRunAt(DateTimePicker runAt, ComboBox<String> timezone) {
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
    public static Instant runAtInstant(DateTimePicker runAt, ComboBox<String> timezone) {
        LocalDateTime value = runAt.getValue();
        return value == null ? null : value.atZone(selectedZone(timezone)).toInstant();
    }

    /**
     * Resolves the timezone entered in {@code timezone}, so the ONCE wall-clock
     * picker is interpreted in that zone rather than the admin JVM's system
     * zone. Falls back to UTC when the field is blank or not a valid {@link ZoneId}.
     *
     * <p>Used for interpreting an already-valid value (e.g. after
     * {@link #validateTimezone}, or while prefilling from a stored schedule);
     * it does not itself surface an invalid free-typed zone to the user.
     */
    public static ZoneId selectedZone(ComboBox<String> timezone) {
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

    /**
     * Marks {@code timezone} invalid when it holds a non-blank value that is not
     * a valid {@link ZoneId} — e.g. a typo like {@code Europe/Kyv}. A blank value
     * is valid (the API defaults it to UTC); replaces the previous silent
     * fallback to UTC, which let such a typo through client-side only to be
     * rejected by the API with a 400.
     *
     * @return {@code true} when the field is blank or holds a valid zone id
     */
    public static boolean validateTimezone(ComboBox<String> timezone) {
        String zone = Strings.trimToNull(timezone.getValue());
        boolean valid = zone == null || isValidZone(zone);
        if (!valid) {
            logger.debug("Invalid timezone rejected in the schedule form: timezone='{}'", zone);
        }
        timezone.setErrorMessage(ScheduleText.VALIDATION_TIMEZONE_INVALID);
        timezone.setInvalid(!valid);
        return valid;
    }

    /**
     * Validates {@code timezone} only when {@code selectedType} uses it —
     * {@code FIXED} is a plain interval with no wall-clock/timezone meaning, so
     * the field is hidden and skipped there. Shared by {@link ScheduleForm} and
     * the wizard's {@code ScheduleStep} so the FIXED exemption is defined once.
     *
     * @return {@code true} when {@code selectedType} is {@code FIXED}, or the
     *         field is blank or holds a valid zone id
     */
    public static boolean validateTimezoneIfApplicable(ComboBox<String> timezone, ScheduleType selectedType) {
        return selectedType == ScheduleType.FIXED || validateTimezone(timezone);
    }

    private static boolean isValidZone(String zone) {
        try {
            ZoneId.of(zone);
            return true;
        } catch (DateTimeException ex) {
            return false;
        }
    }
}
