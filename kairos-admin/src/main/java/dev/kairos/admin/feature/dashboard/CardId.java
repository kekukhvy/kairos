package dev.kairos.admin.feature.dashboard;

/**
 * Stable identifiers for every dashboard card. The name is persisted to browser
 * storage to remember which cards a user has hidden and in what order, so these
 * constants must not be renamed once shipped. Ordinal order here is the default
 * layout order shown before the user customises anything.
 */
public enum CardId {

    ACTIVE_TASKS,
    TOTAL_TASKS,
    ACTIVE_SCHEDULES,
    TOTAL_SCHEDULES,
    SERVICE_LEADERBOARD,
    SCHEDULE_LEADERBOARD,
    // --- preview cards: no execution data exists yet ---
    NEXT_RUN,
    COMPLETED_TODAY,
    SUCCESS_RATE,
    FAILURE_LEADERBOARD
}
