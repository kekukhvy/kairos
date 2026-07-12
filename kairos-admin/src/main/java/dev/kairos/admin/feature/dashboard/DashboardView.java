package dev.kairos.admin.feature.dashboard;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import dev.kairos.admin.feature.dashboard.component.DashboardCard;
import dev.kairos.admin.feature.dashboard.component.DashboardSettingsDialog;
import dev.kairos.admin.feature.dashboard.component.LeaderboardCard;
import dev.kairos.admin.feature.dashboard.component.StatCard;
import dev.kairos.admin.feature.schedule.ScheduleView;
import dev.kairos.admin.feature.task.TaskRoutes;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.TaskView;
import dev.kairos.admin.shared.layout.MainLayout;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Landing dashboard. Renders metric cards driven by real task/schedule data,
 * plus a few preview cards whose data (executions) does not exist yet. The user
 * chooses which cards to show and can drag them to reorder; those choices persist
 * to browser storage via {@link DashboardPrefs}.
 */
@Route(value = DashboardRoutes.HOME, layout = MainLayout.class)
@PageTitle(DashboardRoutes.PAGE_TITLE)
public class DashboardView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(DashboardView.class);

    private final DashboardService dashboardService;
    private final Div grid = new Div();

    private DashboardPrefs prefs = DashboardPrefs.defaults();
    private DashboardStats stats = DashboardStats.empty();

    /**
     * Constructs the dashboard view: builds the static header, starts loading
     * stats synchronously, then asynchronously loads the stored user prefs from
     * browser storage and triggers the first render once both are ready.
     *
     * @param dashboardService service that computes the dashboard metrics
     */
    public DashboardView(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
        setSizeFull();
        setSpacing(false);
        setPadding(false);
        StyleConfig.create().padding(Tokens.SPACE_L).gap(Tokens.SPACE_L).applyTo(this);

        applyGridStyle();
        add(buildHeader(), grid);

        stats = dashboardService.load();
        DashboardPrefs.load(loaded -> {
            prefs = loaded;
            render();
        });
    }

    private void applyGridStyle() {
        StyleConfig.create()
                .display(Tokens.DISPLAY_GRID)
                .gridTemplateColumns(Tokens.GRID_CARDS_STATS)
                .gap(Tokens.SPACE_M)
                .fullWidth()
                .applyTo(grid);
    }

    private Component buildHeader() {
        H2 title = StyleConfig.create().fontSize(Tokens.FONT_XL).applyTo(new H2(DashboardText.TITLE));
        Span subtitle = StyleConfig.create()
                .color(Tokens.TEXT_SECONDARY).fontSize(Tokens.FONT_M)
                .applyTo(new Span(DashboardText.SUBTITLE));
        VerticalLayout titleBlock = new VerticalLayout(title, subtitle);
        titleBlock.setPadding(false);
        titleBlock.setSpacing(false);

        Button customize = Buttons.secondary(DashboardText.SETTINGS_OPEN, e -> openSettings());
        customize.setIcon(VaadinIcon.COG.create());

        HorizontalLayout header = new HorizontalLayout(titleBlock, customize);
        header.setWidthFull();
        header.setAlignItems(HorizontalLayout.Alignment.CENTER);
        header.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        return header;
    }

    private void openSettings() {
        new DashboardSettingsDialog(prefs, this::render).open();
    }

    private void render() {
        grid.removeAll();
        prefs.order().stream()
                .filter(prefs::isVisible)
                .forEach(id -> grid.add(wireDrag(id, buildCard(id))));
    }

    private DashboardCard buildCard(CardId id) {
        return switch (id) {
            case ACTIVE_TASKS -> stat(id, VaadinIcon.TASKS, Tokens.COLOR_PRIMARY,
                    String.valueOf(stats.activeTasks()), () -> openTasks(TaskText.STATUS_ACTIVE));
            case TOTAL_TASKS -> stat(id, VaadinIcon.RECORDS, Tokens.COLOR_PRIMARY,
                    String.valueOf(stats.totalTasks()), () -> openTasks(null));
            case ACTIVE_SCHEDULES -> stat(id, VaadinIcon.CALENDAR_CLOCK, Tokens.COLOR_SUCCESS,
                    String.valueOf(stats.activeSchedules()), this::openSchedules);
            case TOTAL_SCHEDULES -> stat(id, VaadinIcon.CALENDAR, Tokens.COLOR_SUCCESS,
                    String.valueOf(stats.totalSchedules()), this::openSchedules);
            case SERVICE_LEADERBOARD -> new LeaderboardCard(DashboardText.SERVICE_LEADERBOARD,
                    stats.serviceRanking(), false);
            case SCHEDULE_LEADERBOARD -> new LeaderboardCard(DashboardText.SCHEDULE_LEADERBOARD,
                    stats.scheduleRanking(), false);
            case NEXT_RUN -> previewStat(VaadinIcon.CLOCK, DashboardText.PREVIEW_NEXT_RUN,
                    DashboardText.NEXT_RUN);
            case COMPLETED_TODAY -> previewStat(VaadinIcon.CHECK_CIRCLE,
                    DashboardText.PREVIEW_COMPLETED_TODAY, DashboardText.COMPLETED_TODAY);
            case SUCCESS_RATE -> previewStat(VaadinIcon.TRENDING_UP,
                    DashboardText.PREVIEW_SUCCESS_RATE, DashboardText.SUCCESS_RATE);
            case FAILURE_LEADERBOARD -> new LeaderboardCard(DashboardText.FAILURE_LEADERBOARD,
                    previewFailures(), true);
        };
    }

    private StatCard stat(CardId id, VaadinIcon icon, String accent, String value, Runnable onClick) {
        return new StatCard(icon, accent, value, DashboardCardMeta.title(id), false, onClick);
    }

    private StatCard previewStat(VaadinIcon icon, String value, String caption) {
        return new StatCard(icon, Tokens.COLOR_WARNING, value, caption, true, null);
    }

    private List<DashboardStats.Ranked> previewFailures() {
        return List.of(
                new DashboardStats.Ranked(DashboardText.PREVIEW_FAILURE_1, DashboardText.PREVIEW_FAILURE_COUNT_1),
                new DashboardStats.Ranked(DashboardText.PREVIEW_FAILURE_2, DashboardText.PREVIEW_FAILURE_COUNT_2),
                new DashboardStats.Ranked(DashboardText.PREVIEW_FAILURE_3, DashboardText.PREVIEW_FAILURE_COUNT_3));
    }

    private void openTasks(String status) {
        QueryParameters query = status == null
                ? QueryParameters.empty()
                : QueryParameters.of(TaskRoutes.QUERY_STATUS, status);
        UI.getCurrent().navigate(TaskView.class, query);
    }

    private void openSchedules() {
        UI.getCurrent().navigate(ScheduleView.class);
    }

    // --- drag to reorder ---

    /**
     * Makes {@code card} both a drag source (carrying its {@link CardId}) and a
     * drop target. Dropping one card onto another moves the dragged card to the
     * target's position and persists the new order.
     */
    private DashboardCard wireDrag(CardId id, DashboardCard card) {
        DragSource<DashboardCard> source = DragSource.configure(card, true);
        source.setDragData(id);

        DropTarget<DashboardCard> target = DropTarget.configure(card, true);
        target.addDropListener(event -> event.getDragData()
                .filter(data -> data instanceof CardId)
                .map(CardId.class::cast)
                .ifPresent(moved -> onDrop(moved, id)));
        return card;
    }

    private void onDrop(CardId moved, CardId target) {
        logger.debug("Dashboard card reordered: moved={} to position of target={}", moved, target);
        prefs.reorder(moved, target);
        prefs.save();
        render();
    }
}
