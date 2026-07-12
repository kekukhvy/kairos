package dev.kairos.admin.feature.dashboard.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import dev.kairos.admin.feature.dashboard.DashboardStats;
import dev.kairos.admin.feature.dashboard.DashboardText;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LeaderboardCardTest {

    private static final String TITLE_TOP_SERVICES = DashboardText.SERVICE_LEADERBOARD;
    private static final String TITLE_MOST_SCHEDULED = DashboardText.SCHEDULE_LEADERBOARD;

    private static final String LABEL_BILLING = "billing / invoice";
    private static final String LABEL_EMAIL = "email / digest";
    private static final String LABEL_PAYMENTS = "payments / charge";

    // --- title ---

    @Test
    void constructor_withRows_titleSpanPresent() {
        List<DashboardStats.Ranked> rows = List.of(new DashboardStats.Ranked(LABEL_BILLING, 3L));

        LeaderboardCard card = new LeaderboardCard(TITLE_TOP_SERVICES, rows, false);

        assertThat(spansWithText(card, TITLE_TOP_SERVICES)).isNotEmpty();
    }

    @Test
    void constructor_emptyRows_titleSpanPresent() {
        LeaderboardCard card = new LeaderboardCard(TITLE_MOST_SCHEDULED, List.of(), false);

        assertThat(spansWithText(card, TITLE_MOST_SCHEDULED)).isNotEmpty();
    }

    // --- rows ---

    @Test
    void constructor_singleRow_rowLabelSpanPresent() {
        List<DashboardStats.Ranked> rows = List.of(new DashboardStats.Ranked(LABEL_BILLING, 5L));

        LeaderboardCard card = new LeaderboardCard(TITLE_TOP_SERVICES, rows, false);

        assertThat(spansWithText(card, LABEL_BILLING)).isNotEmpty();
    }

    @Test
    void constructor_multipleRows_allRowLabelsPresent() {
        List<DashboardStats.Ranked> rows = List.of(
                new DashboardStats.Ranked(LABEL_BILLING, 5L),
                new DashboardStats.Ranked(LABEL_EMAIL, 3L),
                new DashboardStats.Ranked(LABEL_PAYMENTS, 1L));

        LeaderboardCard card = new LeaderboardCard(TITLE_TOP_SERVICES, rows, false);

        assertThat(spansWithText(card, LABEL_BILLING)).isNotEmpty();
        assertThat(spansWithText(card, LABEL_EMAIL)).isNotEmpty();
        assertThat(spansWithText(card, LABEL_PAYMENTS)).isNotEmpty();
    }

    @Test
    void constructor_multipleRows_rowCountSpansPresent() {
        List<DashboardStats.Ranked> rows = List.of(
                new DashboardStats.Ranked(LABEL_BILLING, 5L),
                new DashboardStats.Ranked(LABEL_EMAIL, 3L));

        LeaderboardCard card = new LeaderboardCard(TITLE_TOP_SERVICES, rows, false);

        assertThat(spansWithText(card, "5")).isNotEmpty();
        assertThat(spansWithText(card, "3")).isNotEmpty();
    }

    // --- empty state ---

    @Test
    void constructor_emptyRows_emptyStateSpanPresent() {
        LeaderboardCard card = new LeaderboardCard(TITLE_TOP_SERVICES, List.of(), false);

        assertThat(spansWithText(card, DashboardText.LEADERBOARD_EMPTY)).isNotEmpty();
    }

    @Test
    void constructor_withRows_emptyStateSpanAbsent() {
        List<DashboardStats.Ranked> rows = List.of(new DashboardStats.Ranked(LABEL_BILLING, 2L));

        LeaderboardCard card = new LeaderboardCard(TITLE_TOP_SERVICES, rows, false);

        assertThat(spansWithText(card, DashboardText.LEADERBOARD_EMPTY)).isEmpty();
    }

    // --- preview badge ---

    @Test
    void constructor_previewTrue_previewBadgeSpanPresent() {
        LeaderboardCard card = new LeaderboardCard(TITLE_TOP_SERVICES, List.of(), true);

        assertThat(spansWithText(card, DashboardText.PREVIEW_BADGE)).isNotEmpty();
    }

    @Test
    void constructor_previewFalse_previewBadgeSpanAbsent() {
        LeaderboardCard card = new LeaderboardCard(TITLE_TOP_SERVICES, List.of(), false);

        assertThat(spansWithText(card, DashboardText.PREVIEW_BADGE)).isEmpty();
    }

    @Test
    void constructor_previewTrue_titleStillPresent() {
        LeaderboardCard card = new LeaderboardCard(TITLE_TOP_SERVICES, List.of(), true);

        assertThat(spansWithText(card, TITLE_TOP_SERVICES)).isNotEmpty();
    }

    // --- helpers ---

    private static Stream<Span> spansWithText(Component root, String text) {
        return allDescendants(root)
                .filter(c -> c instanceof Span)
                .map(c -> (Span) c)
                .filter(s -> text.equals(s.getText()));
    }

    private static Stream<Component> allDescendants(Component root) {
        return Stream.concat(
                Stream.of(root),
                root.getChildren().flatMap(LeaderboardCardTest::allDescendants));
    }
}
