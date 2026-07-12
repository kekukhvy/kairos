package dev.kairos.admin.feature.dashboard.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import dev.kairos.admin.feature.dashboard.CardId;
import dev.kairos.admin.feature.dashboard.DashboardCardMeta;
import dev.kairos.admin.feature.dashboard.DashboardPrefs;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Only covers dialog construction/rendering. Toggling a checkbox or clicking
 * "Reset to defaults" calls {@link DashboardPrefs#save()}, which requires a
 * live Vaadin {@code UI}/session (browser round-trip via {@code WebStorage});
 * that interaction is out of scope for a UI-less unit test.
 */
class DashboardSettingsDialogTest {

    private static final CardId TOGGLED_CARD = CardId.TOTAL_TASKS;

    // --- initial checkbox state ---

    @Test
    void constructor_oneCheckboxPerCardId() {
        DashboardSettingsDialog dialog = new DashboardSettingsDialog(DashboardPrefs.defaults(), () -> { });

        assertThat(checkboxes(dialog)).hasSize(CardId.values().length);
    }

    @Test
    void constructor_checkboxLabelsMatchCardTitles() {
        DashboardSettingsDialog dialog = new DashboardSettingsDialog(DashboardPrefs.defaults(), () -> { });

        List<String> labels = checkboxes(dialog).map(Checkbox::getLabel).toList();
        for (CardId id : CardId.values()) {
            assertThat(labels).contains(DashboardCardMeta.title(id));
        }
    }

    @Test
    void constructor_defaultsPrefs_allCheckboxesChecked() {
        DashboardSettingsDialog dialog = new DashboardSettingsDialog(DashboardPrefs.defaults(), () -> { });

        assertThat(checkboxes(dialog)).allMatch(Checkbox::getValue);
    }

    @Test
    void constructor_hiddenCard_correspondingCheckboxUnchecked() {
        DashboardPrefs prefs = DashboardPrefs.defaults();
        prefs.setVisible(TOGGLED_CARD, false);

        DashboardSettingsDialog dialog = new DashboardSettingsDialog(prefs, () -> { });

        Checkbox box = checkboxFor(dialog, TOGGLED_CARD);
        assertThat(box.getValue()).isFalse();
    }

    @Test
    void constructor_onlyOneCardHidden_otherCheckboxesStillChecked() {
        DashboardPrefs prefs = DashboardPrefs.defaults();
        prefs.setVisible(TOGGLED_CARD, false);

        DashboardSettingsDialog dialog = new DashboardSettingsDialog(prefs, () -> { });

        Checkbox other = checkboxFor(dialog, CardId.ACTIVE_TASKS);
        assertThat(other.getValue()).isTrue();
    }

    // --- helpers ---

    private static Checkbox checkboxFor(DashboardSettingsDialog dialog, CardId id) {
        String label = DashboardCardMeta.title(id);
        return checkboxes(dialog)
                .filter(box -> label.equals(box.getLabel()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No checkbox found for " + id));
    }

    private static Stream<Checkbox> checkboxes(Component root) {
        return allDescendants(root)
                .filter(c -> c instanceof Checkbox)
                .map(c -> (Checkbox) c);
    }

    private static Stream<Component> allDescendants(Component root) {
        return Stream.concat(
                Stream.of(root),
                root.getChildren().flatMap(DashboardSettingsDialogTest::allDescendants));
    }
}
