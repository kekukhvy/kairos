package dev.kairos.admin.feature.dashboard;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardCardMetaTest {

    // --- title ---

    @Test
    void title_activeTasks_returnsActiveTasksText() {
        assertThat(DashboardCardMeta.title(CardId.ACTIVE_TASKS)).isEqualTo(DashboardText.ACTIVE_TASKS);
    }

    @Test
    void title_totalTasks_returnsTotalTasksText() {
        assertThat(DashboardCardMeta.title(CardId.TOTAL_TASKS)).isEqualTo(DashboardText.TOTAL_TASKS);
    }

    @Test
    void title_activeSchedules_returnsActiveSchedulesText() {
        assertThat(DashboardCardMeta.title(CardId.ACTIVE_SCHEDULES)).isEqualTo(DashboardText.ACTIVE_SCHEDULES);
    }

    @Test
    void title_totalSchedules_returnsTotalSchedulesText() {
        assertThat(DashboardCardMeta.title(CardId.TOTAL_SCHEDULES)).isEqualTo(DashboardText.TOTAL_SCHEDULES);
    }

    @Test
    void title_serviceLeaderboard_returnsServiceLeaderboardText() {
        assertThat(DashboardCardMeta.title(CardId.SERVICE_LEADERBOARD)).isEqualTo(DashboardText.SERVICE_LEADERBOARD);
    }

    @Test
    void title_scheduleLeaderboard_returnsScheduleLeaderboardText() {
        assertThat(DashboardCardMeta.title(CardId.SCHEDULE_LEADERBOARD))
                .isEqualTo(DashboardText.SCHEDULE_LEADERBOARD);
    }

    @Test
    void title_nextRun_returnsNextRunText() {
        assertThat(DashboardCardMeta.title(CardId.NEXT_RUN)).isEqualTo(DashboardText.NEXT_RUN);
    }

    @Test
    void title_completedToday_returnsCompletedTodayText() {
        assertThat(DashboardCardMeta.title(CardId.COMPLETED_TODAY)).isEqualTo(DashboardText.COMPLETED_TODAY);
    }

    @Test
    void title_successRate_returnsSuccessRateText() {
        assertThat(DashboardCardMeta.title(CardId.SUCCESS_RATE)).isEqualTo(DashboardText.SUCCESS_RATE);
    }

    @Test
    void title_failureLeaderboard_returnsFailureLeaderboardText() {
        assertThat(DashboardCardMeta.title(CardId.FAILURE_LEADERBOARD)).isEqualTo(DashboardText.FAILURE_LEADERBOARD);
    }

    @Test
    void title_everyCardId_hasARegisteredTitle() {
        for (CardId id : CardId.values()) {
            assertThat(DashboardCardMeta.title(id))
                    .as("title for %s should not fall back to the enum name", id)
                    .isNotEqualTo(id.name());
        }
    }

    // --- isPreview ---

    @Test
    void isPreview_nextRun_isTrue() {
        assertThat(DashboardCardMeta.isPreview(CardId.NEXT_RUN)).isTrue();
    }

    @Test
    void isPreview_completedToday_isTrue() {
        assertThat(DashboardCardMeta.isPreview(CardId.COMPLETED_TODAY)).isTrue();
    }

    @Test
    void isPreview_successRate_isTrue() {
        assertThat(DashboardCardMeta.isPreview(CardId.SUCCESS_RATE)).isTrue();
    }

    @Test
    void isPreview_failureLeaderboard_isTrue() {
        assertThat(DashboardCardMeta.isPreview(CardId.FAILURE_LEADERBOARD)).isTrue();
    }

    @Test
    void isPreview_activeTasks_isFalse() {
        assertThat(DashboardCardMeta.isPreview(CardId.ACTIVE_TASKS)).isFalse();
    }

    @Test
    void isPreview_serviceLeaderboard_isFalse() {
        assertThat(DashboardCardMeta.isPreview(CardId.SERVICE_LEADERBOARD)).isFalse();
    }

    @Test
    void isPreview_exactlyFourCardsArePreview() {
        long previewCount = Arrays.stream(CardId.values())
                .filter(DashboardCardMeta::isPreview)
                .count();

        assertThat(previewCount).isEqualTo(4);
    }
}
