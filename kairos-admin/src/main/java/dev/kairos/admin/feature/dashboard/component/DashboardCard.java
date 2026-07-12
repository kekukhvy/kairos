package dev.kairos.admin.feature.dashboard.component;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import dev.kairos.admin.feature.dashboard.DashboardText;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;

/**
 * Shared surface for every dashboard card: the glass card styling, an optional
 * "Preview" badge for cards not yet backed by real data, and an optional
 * clickable affordance. Concrete cards ({@link StatCard}, {@link LeaderboardCard})
 * add their own content on top.
 */
public abstract class DashboardCard extends VerticalLayout {

    /**
     * Applies the shared glass-card surface and, when {@code preview} is
     * {@code true}, prepends the "Preview" badge to signal placeholder data.
     *
     * @param preview {@code true} to show the preview badge
     */
    protected DashboardCard(boolean preview) {
        setPadding(false);
        setSpacing(false);
        applyCardStyle();
        if (preview) {
            add(buildPreviewBadge());
        }
    }

    private void applyCardStyle() {
        StyleConfig.create()
                .background(Tokens.SURFACE_TRANSLUCENT)
                .border(Tokens.BORDER_LIGHT)
                .borderRadius(Tokens.RADIUS_L)
                .boxShadow(Tokens.SHADOW_S)
                .padding(Tokens.SPACE_L)
                .gap(Tokens.SPACE_S)
                .transition(Tokens.TRANSITION)
                .applyTo(this);
    }

    private Span buildPreviewBadge() {
        Span badge = new Span(DashboardText.PREVIEW_BADGE);
        badge.getElement().getThemeList().add(Tokens.THEME_BADGE_CONTRAST);
        return StyleConfig.create()
                .marginInlineStart(Tokens.AUTO)
                .applyTo(badge);
    }

    /** Marks the card as interactive: pointer cursor and a subtle lift affordance. */
    protected void makeClickable() {
        StyleConfig.create()
                .cursor(Tokens.CURSOR_POINTER)
                .applyTo(this);
    }
}
