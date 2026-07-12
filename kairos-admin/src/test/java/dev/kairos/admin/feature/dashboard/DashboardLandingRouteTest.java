package dev.kairos.admin.feature.dashboard;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import dev.kairos.admin.feature.task.TaskRoutes;
import dev.kairos.admin.feature.task.TaskView;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the dashboard owns the console's landing page (the empty route)
 * and that {@link TaskView} no longer claims it, so the two views cannot
 * clash on startup.
 *
 * <p>These checks read the {@link Route}/{@link RouteAlias} annotations via
 * reflection rather than constructing the views: {@link DashboardView}'s
 * constructor calls {@link DashboardPrefs#load} (browser {@code localStorage}
 * via {@code WebStorage}) and {@link TaskView} requires application services,
 * both of which need a running {@code VaadinSession}/UI that is not available
 * in a plain unit test. Verifying route ownership at the annotation level is
 * sufficient to close this gap without a live UI.
 */
class DashboardLandingRouteTest {

    // --- DashboardView owns the empty route ---

    @Test
    void dashboardView_routeAnnotation_valueIsHomeConstant() {
        Route route = DashboardView.class.getAnnotation(Route.class);

        assertThat(route.value()).isEqualTo(DashboardRoutes.HOME);
    }

    @Test
    void dashboardRoutes_home_isEmptyRoute() {
        assertThat(DashboardRoutes.HOME).isEmpty();
    }

    @Test
    void dashboardRoutes_pageTitle_isSet() {
        assertThat(DashboardRoutes.PAGE_TITLE).isNotBlank();
    }

    // --- TaskView no longer owns the empty route ---

    @Test
    void taskView_routeAnnotation_valueIsTasksConstant_notEmpty() {
        Route route = TaskView.class.getAnnotation(Route.class);

        assertThat(route.value()).isEqualTo(TaskRoutes.TASKS);
        assertThat(route.value()).isNotEqualTo(DashboardRoutes.HOME);
    }

    @Test
    void taskView_hasNoRouteAliasForHome() {
        RouteAlias alias = TaskView.class.getAnnotation(RouteAlias.class);

        assertThat(alias).isNull();
    }

    // --- no landing-page clash between the two views ---

    @Test
    void dashboardAndTaskView_routeValues_doNotClash() {
        String dashboardRoute = DashboardView.class.getAnnotation(Route.class).value();
        String taskRoute = TaskView.class.getAnnotation(Route.class).value();

        assertThat(dashboardRoute).isNotEqualTo(taskRoute);
    }
}
