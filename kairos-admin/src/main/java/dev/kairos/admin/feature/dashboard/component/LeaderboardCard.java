package dev.kairos.admin.feature.dashboard.component;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import dev.kairos.admin.feature.dashboard.DashboardStats;
import dev.kairos.admin.feature.dashboard.DashboardText;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;

import java.util.List;

/**
 * A dashboard leaderboard card: a title and a ranked list of label/count rows.
 * Renders an empty-state message when there are no rows. The {@code preview} flag
 * marks a leaderboard whose data is a placeholder until executions are wired.
 */
public class LeaderboardCard extends DashboardCard {

    /**
     * Creates a leaderboard card.
     *
     * @param title   heading describing what is ranked
     * @param rows    ranked entries (already sorted, highest first); may be empty
     * @param preview whether to show the "Preview" badge
     */
    public LeaderboardCard(String title, List<DashboardStats.Ranked> rows, boolean preview) {
        super(preview);
        add(buildTitle(title));
        if (rows.isEmpty()) {
            add(buildEmpty());
            return;
        }
        rows.forEach(row -> add(buildRow(row)));
    }

    private Span buildTitle(String title) {
        Span span = new Span(title);
        return StyleConfig.create()
                .fontSize(Tokens.FONT_L)
                .fontWeight(Tokens.FONT_WEIGHT_SEMIBOLD)
                .color(Tokens.TEXT_BODY)
                .applyTo(span);
    }

    private Span buildEmpty() {
        Span span = new Span(DashboardText.LEADERBOARD_EMPTY);
        return StyleConfig.create()
                .fontSize(Tokens.FONT_S)
                .color(Tokens.TEXT_SECONDARY)
                .applyTo(span);
    }

    private HorizontalLayout buildRow(DashboardStats.Ranked row) {
        Span label = StyleConfig.create()
                .fontSize(Tokens.FONT_S)
                .color(Tokens.TEXT_BODY)
                .applyTo(new Span(row.label()));
        Span count = StyleConfig.create()
                .fontSize(Tokens.FONT_S)
                .fontWeight(Tokens.FONT_WEIGHT_SEMIBOLD)
                .color(Tokens.TEXT_SECONDARY)
                .marginInlineStart(Tokens.AUTO)
                .applyTo(new Span(String.valueOf(row.count())));

        HorizontalLayout line = new HorizontalLayout(label, count);
        line.setWidthFull();
        line.setPadding(false);
        line.setSpacing(false);
        return line;
    }
}
