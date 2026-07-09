package dev.kairos.admin.feature.schedule;

import java.util.List;

/**
 * UI string constants and option lists for the CRON Expression Builder — dialog
 * labels, the six field labels and helper texts, their option lists, preset
 * names, advisory warnings, description fragments, notifications and the
 * clipboard JS snippet. Centralised so no literal strings scatter across
 * {@link dev.kairos.admin.feature.schedule.component.CronBuilderDialog},
 * {@link dev.kairos.admin.feature.schedule.component.CronPreview} and
 * {@link dev.kairos.admin.feature.schedule.component.CronPresets}, mirroring
 * {@link ScheduleText}.
 */
public final class CronText {

    private CronText() {
    }

    // --- dialog ---
    public static final String TITLE = "Build cron expression";
    public static final String BUILD_BUTTON = "Build cron";
    public static final String BUILD_TOOLTIP = "Build cron expression visually";

    // --- default expression: Daily at 2 AM ---
    public static final String DEFAULT_EXPRESSION = "0 0 2 * * ?";

    // --- quick start ---
    public static final String QUICK_START_LABEL = "Quick start";
    public static final String QUICK_START_HELPER = "Pick a common schedule to fill the fields below";

    public static final String PRESET_EVERY_MINUTE = "Every minute";
    public static final String PRESET_EVERY_5_MINUTES = "Every 5 minutes";
    public static final String PRESET_EVERY_15_MINUTES = "Every 15 minutes";
    public static final String PRESET_EVERY_HOUR = "Every hour";
    public static final String PRESET_DAILY_MIDNIGHT = "Daily at midnight";
    public static final String PRESET_DAILY_2AM = "Daily at 2 AM";
    public static final String PRESET_DAILY_6AM = "Daily at 6 AM";
    public static final String PRESET_WEEKDAYS_9AM = "Weekdays at 9 AM";
    public static final String PRESET_WEEKLY_SUNDAY = "Weekly on Sunday";
    public static final String PRESET_MONTHLY_FIRST = "Monthly on the 1st";

    // --- six field labels ---
    public static final String FIELD_SECONDS = "Seconds";
    public static final String FIELD_MINUTES = "Minutes";
    public static final String FIELD_HOURS = "Hours";
    public static final String FIELD_DAY_OF_MONTH = "Day of month";
    public static final String FIELD_MONTH = "Month";
    public static final String FIELD_DAY_OF_WEEK = "Day of week";

    // --- field helper texts ---
    public static final String HELPER_SECONDS = "0-59, * (every), */5 (step), 0,30 (list)";
    public static final String HELPER_MINUTES = "0-59, * (every), */15 (step), 0,30 (list)";
    public static final String HELPER_HOURS = "0-23, * (every), 9-17 (range)";
    public static final String HELPER_DAY_OF_MONTH = "1-31, * (every), ? (any), L (last)";
    public static final String HELPER_MONTH = "1-12 or JAN-DEC, * (every)";
    public static final String HELPER_DAY_OF_WEEK = "MON-SUN, * (every), ? (any), MON-FRI (range)";

    // --- field option lists (custom values also allowed) ---
    public static final List<String> OPTIONS_SECONDS = List.of("0", "*", "*/5", "*/10", "*/15", "*/30");
    public static final List<String> OPTIONS_MINUTES = List.of("0", "*", "*/5", "*/10", "*/15", "*/30");
    public static final List<String> OPTIONS_HOURS =
            List.of("*", "0", "2", "6", "9", "12", "18", "9-17", "*/2");
    public static final List<String> OPTIONS_DAY_OF_MONTH = List.of("*", "?", "1", "15", "L");
    public static final List<String> OPTIONS_MONTH =
            List.of("*", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
    public static final List<String> OPTIONS_DAY_OF_WEEK =
            List.of("*", "?", "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN", "MON-FRI");

    // --- generated expression ---
    public static final String GENERATED_LABEL = "Generated expression";
    public static final String COPY_TOOLTIP = "Copy expression";
    public static final String COPY_SUCCESS = "Expression copied to clipboard";

    // --- description ---
    public static final String DESCRIPTION_LABEL = "Description";
    public static final String DESCRIBE_AT_PREFIX = "At ";
    public static final String DESCRIBE_EVERY_DAY = " every day";
    public static final String DESCRIBE_ON_DOW_PREFIX = " on ";
    public static final String DESCRIBE_ON_DOM_PREFIX = " on day ";
    public static final String DESCRIBE_IN_MONTH_PREFIX = ", in ";
    /** Time format for the "At HH:MM" description fragment. */
    public static final String DESCRIBE_TIME_FORMAT = "%02d:%02d";

    // frequency phrases
    public static final String DESCRIBE_EVERY_SECOND = "Every second";
    public static final String DESCRIBE_EVERY_MINUTE = "Every minute";
    public static final String DESCRIBE_EVERY_HOUR = "Every hour";
    /** Shared "Every N …" prefix for the second/minute/hour step phrases. */
    public static final String DESCRIBE_EVERY_N_PREFIX = "Every ";
    public static final String DESCRIBE_EVERY_N_SECONDS_SUFFIX = " seconds";
    public static final String DESCRIBE_EVERY_N_MINUTES_SUFFIX = " minutes";
    public static final String DESCRIBE_EVERY_N_HOURS_SUFFIX = " hours";
    public static final String DESCRIBE_AT_MINUTE_PREFIX = "At minute ";
    public static final String DESCRIBE_PAST_HOUR = " past every hour";

    // month names, index 1-12 (index 0 unused)
    public static final String[] MONTH_NAMES = {
            "", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"};

    // --- next executions ---
    public static final String NEXT_RUNS_LABEL = "Next execution times";
    public static final String NEXT_RUNS_INVALID = "Fix the expression to preview execution times";
    public static final String NEXT_RUNS_NONE = "No upcoming executions";
    public static final int NEXT_RUNS_COUNT = 10;
    public static final String NEXT_RUNS_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    // --- validation / errors ---
    public static final String ERROR_INVALID = "Invalid cron expression";

    // --- advisory warnings ---
    public static final String WARN_DAY_CONFLICT =
            "Both day-of-month and day-of-week are set; Spring cron treats these as an OR — this may fire more often than expected.";
    public static final String WARN_FEB_DAY = "February never has this day; it will be skipped that month.";
    public static final String WARN_SHORT_MONTH_DAY =
            "This month has only 30 days; day 31 will be skipped that month.";

    // --- footer ---
    public static final String BTN_RESET = "Reset";
    public static final String BTN_APPLY = "Apply";

    // --- clipboard ---
    /** Copies {@code $0} (the expression) to the clipboard via the browser API. */
    public static final String COPY_JS = "navigator.clipboard.writeText($0)";
}
