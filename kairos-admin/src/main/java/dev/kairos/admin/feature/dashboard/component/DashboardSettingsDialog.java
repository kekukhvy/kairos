package dev.kairos.admin.feature.dashboard.component;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import dev.kairos.admin.feature.dashboard.CardId;
import dev.kairos.admin.feature.dashboard.DashboardCardMeta;
import dev.kairos.admin.feature.dashboard.DashboardPrefs;
import dev.kairos.admin.feature.dashboard.DashboardText;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * "Customize dashboard" dialog: a checkbox per card to toggle its visibility.
 * Toggling a card persists the change and calls {@code onChange} so the
 * dashboard re-renders live. Reset restores the default layout.
 */
public class DashboardSettingsDialog extends Dialog {

    private static final Logger logger = LoggerFactory.getLogger(DashboardSettingsDialog.class);

    private final DashboardPrefs prefs;
    private final Runnable onChange;

    /**
     * Creates and fully initialises the settings dialog.
     *
     * @param prefs    the current dashboard preferences, mutated in place as the
     *                 user toggles cards or resets to defaults
     * @param onChange callback invoked after every visibility change or reset so
     *                 the dashboard re-renders without a page reload
     */
    public DashboardSettingsDialog(DashboardPrefs prefs, Runnable onChange) {
        this.prefs = prefs;
        this.onChange = onChange;
        setHeaderTitle(DashboardText.SETTINGS_TITLE);
        add(buildBody());
        getFooter().add(
                Buttons.tertiary(DashboardText.SETTINGS_RESET, e -> reset()),
                Buttons.primary(DashboardText.SETTINGS_DONE, e -> close()));
        setWidth(Tokens.DIALOG_WIDTH_M);
    }

    private VerticalLayout buildBody() {
        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        StyleConfig.create().gap(Tokens.SPACE_S).applyTo(body);

        body.add(hint());
        for (CardId id : CardId.values()) {
            body.add(toggle(id));
        }
        return body;
    }

    private Span hint() {
        Span span = new Span(DashboardText.SETTINGS_HINT);
        return StyleConfig.create()
                .fontSize(Tokens.FONT_S)
                .color(Tokens.TEXT_SECONDARY)
                .applyTo(span);
    }

    private Checkbox toggle(CardId id) {
        Checkbox box = new Checkbox(DashboardCardMeta.title(id));
        box.setValue(prefs.isVisible(id));
        box.addValueChangeListener(e -> apply(id, e.getValue()));
        return box;
    }

    private void apply(CardId id, boolean visible) {
        logger.debug("Dashboard card {} set visible={}", id, visible);
        prefs.setVisible(id, visible);
        prefs.save();
        onChange.run();
    }

    private void reset() {
        logger.debug("Dashboard layout reset to defaults");
        prefs.resetToDefaults();
        prefs.save();
        onChange.run();
        close();
    }
}
