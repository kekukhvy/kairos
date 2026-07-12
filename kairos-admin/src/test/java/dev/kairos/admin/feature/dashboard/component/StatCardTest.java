package dev.kairos.admin.feature.dashboard.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import dev.kairos.admin.feature.dashboard.DashboardText;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class StatCardTest {

    private static final VaadinIcon ICON = VaadinIcon.TASKS;
    private static final String ACCENT = "var(--lumo-primary-color)";
    private static final String VALUE_42 = "42";
    private static final String VALUE_99 = "99%";
    private static final String CAPTION_ACTIVE = "Active Tasks";
    private static final String CAPTION_TOTAL = "Total Tasks";

    // --- value Span ---

    @Test
    void constructor_nonPreview_valueSpanPresent() {
        StatCard card = new StatCard(ICON, ACCENT, VALUE_42, CAPTION_ACTIVE, false, null);

        assertThat(spansWithText(card, VALUE_42)).isNotEmpty();
    }

    @Test
    void constructor_nonPreview_captionSpanPresent() {
        StatCard card = new StatCard(ICON, ACCENT, VALUE_42, CAPTION_ACTIVE, false, null);

        assertThat(spansWithText(card, CAPTION_ACTIVE)).isNotEmpty();
    }

    @Test
    void constructor_differentValueAndCaption_bothSpansPresent() {
        StatCard card = new StatCard(ICON, ACCENT, VALUE_99, CAPTION_TOTAL, false, null);

        assertThat(spansWithText(card, VALUE_99)).isNotEmpty();
        assertThat(spansWithText(card, CAPTION_TOTAL)).isNotEmpty();
    }

    // --- preview badge ---

    @Test
    void constructor_previewTrue_previewBadgeSpanPresent() {
        StatCard card = new StatCard(ICON, ACCENT, VALUE_42, CAPTION_ACTIVE, true, null);

        assertThat(spansWithText(card, DashboardText.PREVIEW_BADGE)).isNotEmpty();
    }

    @Test
    void constructor_previewFalse_previewBadgeSpanAbsent() {
        StatCard card = new StatCard(ICON, ACCENT, VALUE_42, CAPTION_ACTIVE, false, null);

        assertThat(spansWithText(card, DashboardText.PREVIEW_BADGE)).isEmpty();
    }

    @Test
    void constructor_previewTrue_valueAndCaptionStillPresent() {
        StatCard card = new StatCard(ICON, ACCENT, VALUE_42, CAPTION_ACTIVE, true, null);

        assertThat(spansWithText(card, VALUE_42)).isNotEmpty();
        assertThat(spansWithText(card, CAPTION_ACTIVE)).isNotEmpty();
    }

    // --- onClick: null is accepted without error ---

    @Test
    void constructor_nullOnClick_doesNotThrow() {
        StatCard card = new StatCard(ICON, ACCENT, VALUE_42, CAPTION_ACTIVE, false, null);

        assertThat(card).isNotNull();
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
                root.getChildren().flatMap(StatCardTest::allDescendants));
    }
}
