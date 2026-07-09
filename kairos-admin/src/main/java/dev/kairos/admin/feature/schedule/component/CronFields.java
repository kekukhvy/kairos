package dev.kairos.admin.feature.schedule.component;

import dev.kairos.admin.shared.util.Strings;

/**
 * The six parts of a 6-field Spring cron expression:
 * {@code seconds minutes hours dayOfMonth month dayOfWeek}. A plain value
 * object shared by {@link CronBuilderDialog} (which binds each part to a
 * {@code ComboBox}) and {@link CronPreview} (which derives the human-readable
 * description and warnings from the parts). Blank parts fall back to
 * {@code "*"} so the assembled expression is always syntactically complete.
 */
public record CronFields(
        String seconds,
        String minutes,
        String hours,
        String dayOfMonth,
        String month,
        String dayOfWeek) {

    /** Cron wildcard: "any value" for a field. Shared with {@link CronPreview}. */
    static final String ANY = "*";
    /** Cron "no specific value" marker for day-of-month / day-of-week. */
    static final String NO_DAY = "?";
    private static final int PART_COUNT = 6;

    public CronFields {
        seconds = orAny(seconds);
        minutes = orAny(minutes);
        hours = orAny(hours);
        dayOfMonth = orAny(dayOfMonth);
        month = orAny(month);
        dayOfWeek = orAny(dayOfWeek);
    }

    /**
     * Splits a 6-field expression into its parts, or returns {@code null} when
     * the input is blank or does not have exactly six whitespace-separated parts.
     */
    public static CronFields parse(String expression) {
        if (Strings.isBlank(expression)) {
            return null;
        }
        String[] parts = expression.trim().split("\\s+");
        if (parts.length != PART_COUNT) {
            return null;
        }
        return new CronFields(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
    }

    /** Reassembles the six parts into a single space-separated expression. */
    public String expression() {
        return String.join(" ", seconds, minutes, hours, dayOfMonth, month, dayOfWeek);
    }

    private static String orAny(String value) {
        return Strings.isBlank(value) ? ANY : value.trim();
    }
}
