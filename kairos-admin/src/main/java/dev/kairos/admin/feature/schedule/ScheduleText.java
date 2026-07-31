package dev.kairos.admin.feature.schedule;

import java.util.List;

/**
 * UI string constants for the Schedules feature — column headers, field labels,
 * action labels, and notification messages. Centralised here so that literal
 * strings do not scatter across view, grid, form and details classes.
 */
public final class ScheduleText {

    private ScheduleText() {
    }

    public static final String TITLE = "Schedules";
    public static final String NEW_SCHEDULE = "New schedule";
    public static final String EDIT_SCHEDULE = "Edit schedule";

    public static final String PICK_TASK = "Task";

    public static final String COL_TASK = "Task";
    public static final String COL_TYPE = "Type";
    public static final String COL_LABEL = "Label";
    public static final String COL_WHEN = "When";
    public static final String COL_TIMEZONE = "Timezone";
    public static final String COL_ACTIVE = "Active";

    public static final String FIELD_RUN_AT = "Run at";
    public static final String FIELD_CRON = "Cron expression";
    public static final String FIELD_INTERVAL = "Interval (seconds)";

    public static final String ACTION_EDIT = "Edit";
    public static final String ACTION_PAUSE = "Pause";
    public static final String ACTION_RESUME = "Resume";

    public static final String CONFIRM_ENABLE_TITLE = "Enable schedule";
    public static final String CONFIRM_ENABLE_TEXT = "Do you want to enable this schedule?";
    public static final String CONFIRM_DISABLE_TITLE = "Disable schedule";
    public static final String CONFIRM_DISABLE_TEXT = "Do you want to disable this schedule?";

    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_PAUSED = "Paused";

    public static final String FILTER_TASK = "Task";
    public static final String FILTER_TYPE = "Type";
    public static final String FILTER_STATUS = "Status";

    public static final String DEFAULT_TIMEZONE = "UTC";
    public static final String WHEN_EMPTY = "—";

    /**
     * Common IANA zones offered by the timezone picker; any other valid zone can
     * still be typed in. UTC and the local zone (Vienna) lead the list as the two
     * most-used entries, the rest follow west-to-east by region. Rendered with
     * their current UTC offset — see {@code ScheduleWhenFields#zoneLabel}.
     */
    public static final List<String> TIMEZONE_OPTIONS = List.of(
            "UTC",
            "Europe/Vienna",
            "Europe/London",
            "Europe/Lisbon",
            "Europe/Madrid",
            "Europe/Paris",
            "Europe/Berlin",
            "Europe/Rome",
            "Europe/Prague",
            "Europe/Warsaw",
            "Europe/Athens",
            "Europe/Bucharest",
            "Europe/Kyiv",
            "Europe/Istanbul",
            "Europe/Moscow",
            "America/Los_Angeles",
            "America/Denver",
            "America/Chicago",
            "America/New_York",
            "America/Sao_Paulo",
            "Africa/Lagos",
            "Africa/Cairo",
            "Africa/Johannesburg",
            "Asia/Jerusalem",
            "Asia/Dubai",
            "Asia/Karachi",
            "Asia/Kolkata",
            "Asia/Bangkok",
            "Asia/Shanghai",
            "Asia/Hong_Kong",
            "Asia/Singapore",
            "Asia/Seoul",
            "Asia/Tokyo",
            "Australia/Perth",
            "Australia/Sydney",
            "Pacific/Auckland");
    public static final String HELPER_TIMEZONE = "Pick a common zone or type any IANA timezone id";
    public static final String VALIDATION_TIMEZONE_INVALID = "Not a valid timezone";

    /** Wraps the current UTC offset appended to a zone id, e.g. {@code Europe/Vienna (UTC+02:00)}. */
    public static final String TIMEZONE_OFFSET_PREFIX = " (UTC";
    public static final String TIMEZONE_OFFSET_SUFFIX = ")";
    public static final String TIMEZONE_OFFSET_ZERO = "+00:00";

    public static final String DETAILS_TITLE = "Schedule details";
    public static final String DETAIL_ID = "ID";
    public static final String DETAIL_TASK_ID = "Task ID";
    public static final String DETAIL_CREATED = "Created";
    public static final String DETAIL_UPDATED = "Updated";

    public static final String NOTIFY_CREATED = "Schedule created";
    public static final String NOTIFY_CREATE_FAILED = "Failed to create schedule";
    public static final String NOTIFY_UPDATED = "Schedule updated";
    public static final String NOTIFY_UPDATE_FAILED = "Failed to update schedule";
    public static final String NOTIFY_PAUSED = "Schedule paused";
    public static final String NOTIFY_RESUMED = "Schedule resumed";
    public static final String NOTIFY_DELETED = "Schedule deleted";
    public static final String NOTIFY_DELETE_FAILED = "Failed to delete schedule";
    public static final String NOTIFY_LOAD_FAILED = "Failed to load schedules";

    public static final String CONFIRM_DELETE_TITLE = "Delete schedule";
    public static final String CONFIRM_DELETE_TEXT = "This schedule will be removed. You can't undo this.";

    public static final int INTERVAL_MIN = 1;
    public static final int INTERVAL_MAX = 86_400;

    public static final String VALIDATION_INTERVAL_RANGE =
            "Must be between " + INTERVAL_MIN + " and " + INTERVAL_MAX;
    public static final String VALIDATION_RUN_AT_FUTURE = "Must be in the future";

    /** Unit suffix appended to a FIXED schedule's interval when rendered. */
    public static final String INTERVAL_UNIT_SUFFIX = " s";
}
