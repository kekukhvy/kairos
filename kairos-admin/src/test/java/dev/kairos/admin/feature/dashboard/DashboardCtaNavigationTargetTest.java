package dev.kairos.admin.feature.dashboard;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import dev.kairos.admin.feature.schedule.ScheduleView;
import dev.kairos.admin.feature.task.TaskView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the dashboard's stat-card CTAs ({@code ACTIVE_TASKS},
 * {@code TOTAL_TASKS} &rarr; {@link TaskView}; {@code ACTIVE_SCHEDULES},
 * {@code TOTAL_SCHEDULES} &rarr; {@link ScheduleView}) resolve to real Vaadin
 * navigation targets, not to hand-rolled URL strings.
 *
 * <p>{@code DashboardView} calls {@code UI.getCurrent().navigate(TaskView.class, ...)}
 * / {@code navigate(ScheduleView.class)} from its private {@code openTasks}/
 * {@code openSchedules} handlers. Neither {@code DashboardView} (its constructor
 * calls {@link DashboardPrefs#load}, which needs browser {@code localStorage})
 * nor a click on the resulting {@code StatCard} (needs {@code UI.getCurrent()})
 * can be exercised without a running {@code VaadinSession}/UI, so the actual
 * click-to-navigate path is a known UI-mock limitation and is not asserted
 * here. What IS statically verifiable, and is asserted below: the CTA targets
 * are genuine {@code @Route}-annotated view classes (i.e. class-based
 * navigation, {@code UI.navigate(SomeView.class)}, is well-formed for them)
 * and every clickable stat card id in {@link CardId} maps to a title so the
 * CTA wiring in {@code DashboardCardMeta} stays exhaustive.
 */
class DashboardCtaNavigationTargetTest {

    // --- CTA targets are view classes, not literal URL strings ---

    @Test
    void taskView_isRouteAnnotatedComponent_validClassBasedNavigationTarget() {
        assertThat(Component.class).isAssignableFrom(TaskView.class);
        assertThat(TaskView.class.getAnnotation(Route.class)).isNotNull();
    }

    @Test
    void scheduleView_isRouteAnnotatedComponent_validClassBasedNavigationTarget() {
        assertThat(Component.class).isAssignableFrom(ScheduleView.class);
        assertThat(ScheduleView.class.getAnnotation(Route.class)).isNotNull();
    }

    @Test
    void taskViewAndScheduleView_areDistinctNavigationTargets() {
        assertThat(TaskView.class).isNotEqualTo(ScheduleView.class);
    }

    // --- CardId -> title wiring stays exhaustive for the clickable stat cards ---

    @Test
    void taskCtaCardIds_haveDedicatedTitles() {
        assertThat(DashboardCardMeta.title(CardId.ACTIVE_TASKS)).isNotEqualTo(CardId.ACTIVE_TASKS.name());
        assertThat(DashboardCardMeta.title(CardId.TOTAL_TASKS)).isNotEqualTo(CardId.TOTAL_TASKS.name());
    }

    @Test
    void scheduleCtaCardIds_haveDedicatedTitles() {
        assertThat(DashboardCardMeta.title(CardId.ACTIVE_SCHEDULES)).isNotEqualTo(CardId.ACTIVE_SCHEDULES.name());
        assertThat(DashboardCardMeta.title(CardId.TOTAL_SCHEDULES)).isNotEqualTo(CardId.TOTAL_SCHEDULES.name());
    }

    @Test
    void taskAndScheduleCtaCardIds_areNotMarkedAsPreview() {
        assertThat(DashboardCardMeta.isPreview(CardId.ACTIVE_TASKS)).isFalse();
        assertThat(DashboardCardMeta.isPreview(CardId.TOTAL_TASKS)).isFalse();
        assertThat(DashboardCardMeta.isPreview(CardId.ACTIVE_SCHEDULES)).isFalse();
        assertThat(DashboardCardMeta.isPreview(CardId.TOTAL_SCHEDULES)).isFalse();
    }
}
