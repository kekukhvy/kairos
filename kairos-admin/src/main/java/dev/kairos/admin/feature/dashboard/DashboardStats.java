package dev.kairos.admin.feature.dashboard;

import java.util.List;

/**
 * Computed dashboard metrics. Only the fields backed by data that exists today
 * (tasks and schedules) are real; execution-derived metrics (completed today,
 * success rate, next run, failure leaderboard) are not part of this record and
 * remain placeholders in {@link DashboardText} until executions are wired.
 *
 * @param totalTasks       total number of tasks
 * @param activeTasks      number of tasks currently active
 * @param totalSchedules   total number of schedules across all tasks
 * @param activeSchedules  number of schedules currently active
 * @param serviceRanking   services ranked by how many tasks they own, highest first
 * @param scheduleRanking  tasks ranked by how many schedules they have, highest first
 */
public record DashboardStats(
        int totalTasks,
        int activeTasks,
        int totalSchedules,
        int activeSchedules,
        List<Ranked> serviceRanking,
        List<Ranked> scheduleRanking
) {

    /** Maximum number of entries a leaderboard ranking holds. */
    public static final int MAX_LEADERBOARD_SIZE = 5;

    /** A single leaderboard entry: a label and its count. */
    public record Ranked(String label, long count) {
    }

    /** Empty stats, used when the underlying data cannot be loaded. */
    public static DashboardStats empty() {
        return new DashboardStats(0, 0, 0, 0, List.of(), List.of());
    }
}
