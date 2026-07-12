package dev.kairos.admin.feature.dashboard;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardPrefsTest {

    // --- defaults() ---

    @Test
    void defaults_orderContainsAllCardIds() {
        DashboardPrefs prefs = DashboardPrefs.defaults();

        assertThat(prefs.order()).containsExactlyElementsOf(Arrays.asList(CardId.values()));
    }

    @Test
    void defaults_allCardsAreVisible() {
        DashboardPrefs prefs = DashboardPrefs.defaults();

        for (CardId id : CardId.values()) {
            assertThat(prefs.isVisible(id)).as("card %s should be visible", id).isTrue();
        }
    }

    @Test
    void defaults_orderMatchesEnumDeclarationOrder() {
        DashboardPrefs prefs = DashboardPrefs.defaults();

        assertThat(prefs.order()).containsExactly(CardId.values());
    }

    // --- isVisible / setVisible ---

    @Test
    void setVisible_false_hidesCard() {
        DashboardPrefs prefs = DashboardPrefs.defaults();

        prefs.setVisible(CardId.TOTAL_TASKS, false);

        assertThat(prefs.isVisible(CardId.TOTAL_TASKS)).isFalse();
    }

    @Test
    void setVisible_trueAfterFalse_restoresVisibility() {
        DashboardPrefs prefs = DashboardPrefs.defaults();
        prefs.setVisible(CardId.ACTIVE_TASKS, false);

        prefs.setVisible(CardId.ACTIVE_TASKS, true);

        assertThat(prefs.isVisible(CardId.ACTIVE_TASKS)).isTrue();
    }

    @Test
    void setVisible_falseMultipleTimes_remainsHidden() {
        DashboardPrefs prefs = DashboardPrefs.defaults();

        prefs.setVisible(CardId.SUCCESS_RATE, false);
        prefs.setVisible(CardId.SUCCESS_RATE, false);

        assertThat(prefs.isVisible(CardId.SUCCESS_RATE)).isFalse();
    }

    @Test
    void setVisible_hidingOneCard_doesNotAffectOthers() {
        DashboardPrefs prefs = DashboardPrefs.defaults();

        prefs.setVisible(CardId.NEXT_RUN, false);

        assertThat(prefs.isVisible(CardId.ACTIVE_TASKS)).isTrue();
        assertThat(prefs.isVisible(CardId.TOTAL_SCHEDULES)).isTrue();
    }

    @Test
    void setVisible_hidesMultipleCards_eachReportsHidden() {
        DashboardPrefs prefs = DashboardPrefs.defaults();

        prefs.setVisible(CardId.NEXT_RUN, false);
        prefs.setVisible(CardId.COMPLETED_TODAY, false);

        assertThat(prefs.isVisible(CardId.NEXT_RUN)).isFalse();
        assertThat(prefs.isVisible(CardId.COMPLETED_TODAY)).isFalse();
    }

    // --- reorder ---

    @Test
    void reorder_movesCardToTargetIndex_movedAppearsImmediatelyBeforeTarget() {
        DashboardPrefs prefs = DashboardPrefs.defaults();
        List<CardId> allCards = Arrays.asList(CardId.values());
        CardId moved = allCards.get(0);
        CardId target = allCards.get(2);

        prefs.reorder(moved, target);

        int movedIndex = prefs.order().indexOf(moved);
        int targetIndex = prefs.order().indexOf(target);
        assertThat(movedIndex).isEqualTo(targetIndex - 1);
    }

    @Test
    void reorder_sameCard_orderIsUnchanged() {
        DashboardPrefs prefs = DashboardPrefs.defaults();
        List<CardId> originalOrder = List.copyOf(prefs.order());

        prefs.reorder(CardId.ACTIVE_TASKS, CardId.ACTIVE_TASKS);

        assertThat(prefs.order()).isEqualTo(originalOrder);
    }

    @Test
    void reorder_preservesAllCards() {
        DashboardPrefs prefs = DashboardPrefs.defaults();

        prefs.reorder(CardId.FAILURE_LEADERBOARD, CardId.ACTIVE_TASKS);

        assertThat(prefs.order()).containsExactlyInAnyOrderElementsOf(Arrays.asList(CardId.values()));
    }

    @Test
    void reorder_movesLastCardToFirst_firstPositionIsMovedCard() {
        DashboardPrefs prefs = DashboardPrefs.defaults();
        CardId[] all = CardId.values();
        CardId last = all[all.length - 1];
        CardId first = all[0];

        prefs.reorder(last, first);

        assertThat(prefs.order().getFirst()).isEqualTo(last);
    }

    @Test
    void reorder_movesFirstCardToLast_movedAppearsImmediatelyBeforeTarget() {
        DashboardPrefs prefs = DashboardPrefs.defaults();
        CardId[] all = CardId.values();
        CardId first = all[0];
        CardId last = all[all.length - 1];

        prefs.reorder(first, last);

        int movedIndex = prefs.order().indexOf(first);
        int targetIndex = prefs.order().indexOf(last);
        assertThat(movedIndex).isEqualTo(targetIndex - 1);
    }

    // --- resetToDefaults ---

    @Test
    void resetToDefaults_afterHidingCards_allCardsVisibleAgain() {
        DashboardPrefs prefs = DashboardPrefs.defaults();
        prefs.setVisible(CardId.NEXT_RUN, false);
        prefs.setVisible(CardId.COMPLETED_TODAY, false);

        prefs.resetToDefaults();

        for (CardId id : CardId.values()) {
            assertThat(prefs.isVisible(id)).as("card %s should be visible after reset", id).isTrue();
        }
    }

    @Test
    void resetToDefaults_afterReordering_orderMatchesEnumDeclarationOrder() {
        DashboardPrefs prefs = DashboardPrefs.defaults();
        prefs.reorder(CardId.FAILURE_LEADERBOARD, CardId.ACTIVE_TASKS);

        prefs.resetToDefaults();

        assertThat(prefs.order()).containsExactly(CardId.values());
    }

    @Test
    void resetToDefaults_restoresAllCardIds() {
        DashboardPrefs prefs = DashboardPrefs.defaults();
        prefs.setVisible(CardId.SUCCESS_RATE, false);
        prefs.reorder(CardId.TOTAL_TASKS, CardId.ACTIVE_SCHEDULES);

        prefs.resetToDefaults();

        assertThat(prefs.order()).containsExactlyElementsOf(Arrays.asList(CardId.values()));
    }
}
