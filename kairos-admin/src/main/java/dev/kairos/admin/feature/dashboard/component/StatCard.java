package dev.kairos.admin.feature.dashboard.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;

/**
 * A single dashboard summary card: an accent-coloured icon badge, a large value
 * and a caption. When an {@code onClick} action is supplied the whole card is
 * clickable and deep-links into the matching view. The {@code preview} flag adds
 * a badge marking a value that is a placeholder until executions are wired.
 */
public class StatCard extends DashboardCard {

    /**
     * Creates a stat card.
     *
     * @param icon    Vaadin icon rendered inside the accent badge
     * @param accent  CSS colour token applied to the icon (primary/success/warning)
     * @param value   pre-formatted metric string shown in large type
     * @param caption short label beneath the value
     * @param preview whether to show the "Preview" badge (placeholder value)
     * @param onClick action run when the card is clicked, or {@code null} for a non-clickable card
     */
    public StatCard(VaadinIcon icon, String accent, String value, String caption,
                    boolean preview, Runnable onClick) {
        super(preview);
        add(buildIconBadge(icon, accent), buildValue(value), buildCaption(caption));
        if (onClick != null) {
            makeClickable();
            addClickListener(e -> onClick.run());
        }
    }

    private Div buildIconBadge(VaadinIcon icon, String accent) {
        Icon glyph = icon.create();
        StyleConfig.create()
                .width(Tokens.ICON_SIZE_M)
                .height(Tokens.ICON_SIZE_M)
                .color(accent)
                .applyTo(glyph);

        Div badge = new Div(glyph);
        return StyleConfig.create()
                .display(Tokens.DISPLAY_GRID)
                .width(Tokens.ICON_BADGE_SIZE)
                .height(Tokens.ICON_BADGE_SIZE)
                .borderRadius(Tokens.RADIUS_M)
                .background(Tokens.COLOR_PRIMARY_10)
                .alignItems(Tokens.CENTER)
                .justifyContent(Tokens.CENTER)
                .applyTo(badge);
    }

    private Span buildValue(String value) {
        Span span = new Span(value);
        return StyleConfig.create()
                .fontSize(Tokens.FONT_DISPLAY)
                .fontWeight(Tokens.FONT_WEIGHT_BOLD)
                .color(Tokens.TEXT_BODY)
                .applyTo(span);
    }

    private Span buildCaption(String caption) {
        Span span = new Span(caption);
        return StyleConfig.create()
                .fontSize(Tokens.FONT_S)
                .color(Tokens.TEXT_SECONDARY)
                .applyTo(span);
    }
}
