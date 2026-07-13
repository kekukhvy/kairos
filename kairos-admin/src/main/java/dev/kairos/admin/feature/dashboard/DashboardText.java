package dev.kairos.admin.feature.dashboard;

/**
 * UI string constants and placeholder values for the Dashboard feature — card
 * titles, captions, leaderboard headers, the preview badge, settings labels and
 * the placeholder numbers for cards whose data does not exist yet. Centralised
 * here so no literal strings or numbers live in the view or its components.
 */
public final class DashboardText {

    private DashboardText() {
    }

    // --- header ---
    public static final String TITLE = "Dashboard";
    public static final String SUBTITLE = "Your Kairos scheduler at a glance";
    public static final String CREATE_TASK = "Create Task";

    // --- badge shown on cards not yet backed by real data ---
    public static final String PREVIEW_BADGE = "Preview";

    // --- stat card titles (captions) ---
    public static final String ACTIVE_TASKS = "Active Tasks";
    public static final String TOTAL_TASKS = "Total Tasks";
    public static final String ACTIVE_SCHEDULES = "Active Schedules";
    public static final String TOTAL_SCHEDULES = "Total Schedules";
    public static final String NEXT_RUN = "Next to Run";
    public static final String COMPLETED_TODAY = "Completed Today";
    public static final String SUCCESS_RATE = "Success Rate";

    // --- leaderboard card titles ---
    public static final String SERVICE_LEADERBOARD = "Top Services";
    public static final String SCHEDULE_LEADERBOARD = "Most Scheduled Tasks";
    public static final String FAILURE_LEADERBOARD = "Top Failures";

    /** Shown inside a leaderboard card when it has no rows yet. */
    public static final String LEADERBOARD_EMPTY = "No data yet";

    // --- placeholder values for preview cards ---
    // TODO: replace with a real stats source (planning + executions tables) once the engine is wired.
    public static final String PREVIEW_NEXT_RUN = "in 3m";
    public static final String PREVIEW_COMPLETED_TODAY = "1,204";
    public static final String PREVIEW_SUCCESS_RATE = "99.2%";

    // TODO: replace with real per-service failure counts once executions are wired.
    public static final String PREVIEW_FAILURE_1 = "payments / charge-retry";
    public static final String PREVIEW_FAILURE_2 = "email / digest-send";
    public static final String PREVIEW_FAILURE_3 = "billing / invoice-sync";
    public static final long PREVIEW_FAILURE_COUNT_1 = 12;
    public static final long PREVIEW_FAILURE_COUNT_2 = 7;
    public static final long PREVIEW_FAILURE_COUNT_3 = 3;

    // --- settings dialog ---
    public static final String SETTINGS_TITLE = "Customize dashboard";
    public static final String SETTINGS_HINT =
            "Choose which cards to show. Drag cards on the dashboard to reorder them.";
    public static final String SETTINGS_OPEN = "Customize";
    public static final String SETTINGS_DONE = "Done";
    public static final String SETTINGS_RESET = "Reset to defaults";
}
