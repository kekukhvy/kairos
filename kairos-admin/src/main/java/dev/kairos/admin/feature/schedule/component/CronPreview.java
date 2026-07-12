package dev.kairos.admin.feature.schedule.component;

import dev.kairos.admin.feature.schedule.CronText;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure, Vaadin-free cron logic backing {@link CronBuilderDialog}: it validates a
 * 6-field Spring cron expression, computes the next N execution times, renders a
 * plain-English description, and detects advisory (non-blocking) warnings.
 * Framework validation goes through
 * {@link org.springframework.scheduling.support.CronExpression}; everything else
 * is derived from {@link CronFields}. Kept UI-free so it is unit-testable
 * without a running Vaadin session.
 */
public final class CronPreview {

    private static final int FEBRUARY = 2;
    private static final int APRIL = 4;
    private static final int JUNE = 6;
    private static final int SEPTEMBER = 9;
    private static final int NOVEMBER = 11;
    private static final int FEB_MAX_DAY = 29;
    private static final int SHORT_MONTH_MAX_DAY = 30;

    private CronPreview() {
    }

    /** Outcome of validating an expression: valid, or invalid with a message. */
    public record Validation(boolean valid, String message) {

        static Validation ok() {
            return new Validation(true, null);
        }

        static Validation invalid(String message) {
            return new Validation(false, message);
        }
    }

    /**
     * Parses {@code expression} via {@link CronExpression#parse}, returning a
     * valid result or an invalid one carrying the parser's message.
     */
    public static Validation validate(String expression) {
        try {
            CronExpression.parse(expression);
            return Validation.ok();
        } catch (IllegalArgumentException ex) {
            return Validation.invalid(messageOf(ex));
        }
    }

    /**
     * The next {@code count} execution times after now, computed from
     * {@code expression}. Returns an empty list when the expression is invalid
     * or has no further executions.
     */
    public static List<LocalDateTime> nextExecutions(String expression, int count) {
        List<LocalDateTime> times = new ArrayList<>();
        try {
            CronExpression cron = CronExpression.parse(expression);
            LocalDateTime cursor = LocalDateTime.now();
            for (int i = 0; i < count; i++) {
                LocalDateTime next = cron.next(cursor);
                if (next == null) {
                    break;
                }
                times.add(next);
                cursor = next;
            }
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
        return times;
    }

    private static final String STEP = "*/";

    /** Plain-English description of when the schedule fires. */
    public static String describe(CronFields fields) {
        return describeFrequency(fields) + describeDay(fields, isDailyTime(fields)) + describeMonth(fields);
    }

    /** True when the frequency reads as a single daily time ("At HH:MM"). */
    private static boolean isDailyTime(CronFields fields) {
        return literalInt(fields.hours()) != null && literalInt(fields.minutes()) != null;
    }

    private static String describeFrequency(CronFields fields) {
        Integer hour = literalInt(fields.hours());
        Integer minute = literalInt(fields.minutes());
        if (hour != null && minute != null) {
            return CronText.DESCRIBE_AT_PREFIX
                    + String.format(CronText.DESCRIBE_TIME_FORMAT, hour, minute);
        }
        return describeSubHourly(fields, minute);
    }

    private static String describeSubHourly(CronFields fields, Integer minute) {
        Integer hourStep = step(fields.hours());
        if (hourStep != null) {
            return CronText.DESCRIBE_EVERY_N_PREFIX + hourStep + CronText.DESCRIBE_EVERY_N_HOURS_SUFFIX;
        }
        if (CronFields.ANY.equals(fields.hours()) && minute != null) {
            return minute == 0
                    ? CronText.DESCRIBE_EVERY_HOUR
                    : CronText.DESCRIBE_AT_MINUTE_PREFIX + minute + CronText.DESCRIBE_PAST_HOUR;
        }
        return describeMinuteLevel(fields);
    }

    private static String describeMinuteLevel(CronFields fields) {
        Integer minuteStep = step(fields.minutes());
        if (minuteStep != null) {
            return CronText.DESCRIBE_EVERY_N_PREFIX + minuteStep + CronText.DESCRIBE_EVERY_N_MINUTES_SUFFIX;
        }
        if (CronFields.ANY.equals(fields.minutes())) {
            return describeSecondLevel(fields);
        }
        return CronText.DESCRIBE_EVERY_HOUR;
    }

    private static String describeSecondLevel(CronFields fields) {
        Integer secondStep = step(fields.seconds());
        if (secondStep != null) {
            return CronText.DESCRIBE_EVERY_N_PREFIX + secondStep + CronText.DESCRIBE_EVERY_N_SECONDS_SUFFIX;
        }
        return CronFields.ANY.equals(fields.seconds())
                ? CronText.DESCRIBE_EVERY_SECOND : CronText.DESCRIBE_EVERY_MINUTE;
    }

    /** The N in a {@code &#42;/N} step field, or {@code null} when not a step. */
    private static Integer step(String value) {
        return value != null && value.startsWith(STEP) ? literalInt(value.substring(STEP.length())) : null;
    }

    private static String describeMonth(CronFields fields) {
        Integer month = literalInt(fields.month());
        if (month == null || month < 1 || month >= CronText.MONTH_NAMES.length) {
            return "";
        }
        return CronText.DESCRIBE_IN_MONTH_PREFIX + CronText.MONTH_NAMES[month];
    }

    /**
     * Advisory (non-blocking) warnings: impossible February days, day-of-month
     * and day-of-week both constrained, and day 31 in a 30-day month.
     */
    public static List<String> warnings(CronFields fields) {
        List<String> warnings = new ArrayList<>();
        addDayFieldConflict(fields, warnings);
        addImpossibleDayOfMonth(fields, warnings);
        return warnings;
    }

    private static void addDayFieldConflict(CronFields fields, List<String> warnings) {
        if (isConstrained(fields.dayOfMonth()) && isConstrained(fields.dayOfWeek())) {
            warnings.add(CronText.WARN_DAY_CONFLICT);
        }
    }

    private static void addImpossibleDayOfMonth(CronFields fields, List<String> warnings) {
        Integer day = literalInt(fields.dayOfMonth());
        if (day == null) {
            return;
        }
        Integer month = literalInt(fields.month());
        if (month != null && month == FEBRUARY && day > FEB_MAX_DAY) {
            warnings.add(CronText.WARN_FEB_DAY);
        } else if (isThirtyDayMonth(month) && day > SHORT_MONTH_MAX_DAY) {
            warnings.add(CronText.WARN_SHORT_MONTH_DAY);
        }
    }

    private static boolean isThirtyDayMonth(Integer month) {
        return month != null
                && (month == APRIL || month == JUNE || month == SEPTEMBER || month == NOVEMBER);
    }

    private static String describeDay(CronFields fields, boolean dailyTime) {
        boolean anyDayOfMonth = !isConstrained(fields.dayOfMonth());
        boolean anyDayOfWeek = !isConstrained(fields.dayOfWeek());
        if (anyDayOfMonth && anyDayOfWeek) {
            // only spell out "every day" when the frequency is a single daily time;
            // for sub-hourly frequencies ("Every 5 minutes") it would read oddly.
            return dailyTime ? CronText.DESCRIBE_EVERY_DAY : "";
        }
        if (!anyDayOfWeek) {
            return CronText.DESCRIBE_ON_DOW_PREFIX + fields.dayOfWeek();
        }
        return CronText.DESCRIBE_ON_DOM_PREFIX + fields.dayOfMonth();
    }

    private static boolean isConstrained(String value) {
        return !CronFields.ANY.equals(value) && !CronFields.NO_DAY.equals(value);
    }

    private static Integer literalInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String messageOf(IllegalArgumentException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? CronText.ERROR_INVALID : message;
    }
}
