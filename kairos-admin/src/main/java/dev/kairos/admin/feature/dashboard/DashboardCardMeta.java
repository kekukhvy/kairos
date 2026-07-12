package dev.kairos.admin.feature.dashboard;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Static per-card metadata shared by the view and the settings dialog: the
 * human-readable title and whether the card is a preview (placeholder data).
 * Keeping it in one place stops the two consumers drifting apart.
 */
public final class DashboardCardMeta {

    private DashboardCardMeta() {
    }

    private static final Map<CardId, String> TITLES = Map.ofEntries(
            Map.entry(CardId.ACTIVE_TASKS, DashboardText.ACTIVE_TASKS),
            Map.entry(CardId.TOTAL_TASKS, DashboardText.TOTAL_TASKS),
            Map.entry(CardId.ACTIVE_SCHEDULES, DashboardText.ACTIVE_SCHEDULES),
            Map.entry(CardId.TOTAL_SCHEDULES, DashboardText.TOTAL_SCHEDULES),
            Map.entry(CardId.SERVICE_LEADERBOARD, DashboardText.SERVICE_LEADERBOARD),
            Map.entry(CardId.SCHEDULE_LEADERBOARD, DashboardText.SCHEDULE_LEADERBOARD),
            Map.entry(CardId.NEXT_RUN, DashboardText.NEXT_RUN),
            Map.entry(CardId.COMPLETED_TODAY, DashboardText.COMPLETED_TODAY),
            Map.entry(CardId.SUCCESS_RATE, DashboardText.SUCCESS_RATE),
            Map.entry(CardId.FAILURE_LEADERBOARD, DashboardText.FAILURE_LEADERBOARD));

    /** Cards whose values are placeholders until executions are wired. */
    private static final Set<CardId> PREVIEW = EnumSet.of(
            CardId.NEXT_RUN, CardId.COMPLETED_TODAY, CardId.SUCCESS_RATE, CardId.FAILURE_LEADERBOARD);

    /**
     * Returns the human-readable title for {@code id}, falling back to the
     * enum constant name if no entry is registered (guards against future
     * enum additions that forget to update this map).
     *
     * @param id the card whose title to look up
     * @return the display title, never {@code null}
     */
    public static String title(CardId id) {
        return TITLES.getOrDefault(id, id.name());
    }

    /**
     * Returns {@code true} when the card's value is a placeholder that will be
     * replaced once execution data is available.
     *
     * @param id the card to test
     * @return {@code true} for preview-only cards, {@code false} for cards
     *         backed by real data
     */
    public static boolean isPreview(CardId id) {
        return PREVIEW.contains(id);
    }
}
