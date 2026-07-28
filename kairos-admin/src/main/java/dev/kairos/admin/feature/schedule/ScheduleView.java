package dev.kairos.admin.feature.schedule;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import dev.kairos.admin.feature.schedule.component.ScheduleDetails;
import dev.kairos.admin.feature.schedule.component.ScheduleForm;
import dev.kairos.admin.feature.schedule.component.ScheduleGrid;
import dev.kairos.common.dto.schedule.CreateScheduleRequest;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import dev.kairos.admin.feature.schedule.dto.ScheduleType;
import dev.kairos.common.dto.schedule.UpdateScheduleRequest;
import dev.kairos.admin.feature.task.TaskService;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.shared.layout.MainLayout;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Dialogs;
import dev.kairos.admin.shared.ui.Fields;
import dev.kairos.admin.shared.ui.FilterBar;
import dev.kairos.admin.shared.ui.Notifications;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.ui.ViewActions;
import dev.kairos.admin.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Schedules screen. Loads the schedules of every task at once (the list API is
 * task-scoped, so this fans out one call per task) into a single grid with a
 * Task column, filtered client-side by task, type, status and free text. The
 * owning task is chosen inside the create form, so "New schedule" is always
 * available. Implements {@link BeforeEnterObserver} so the Tasks grid's
 * schedule-count badge can deep-link here with a pre-selected task filter via
 * the {@code ?task=} query parameter, mirroring {@code TaskView}'s
 * {@code ?status=} handling.
 */
@Route(value = ScheduleRoutes.SCHEDULES, layout = MainLayout.class)
@PageTitle(ScheduleRoutes.PAGE_TITLE)
public class ScheduleView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleView.class);

    private final ScheduleService scheduleService;
    private final TaskService taskService;

    private final ScheduleGrid grid = new ScheduleGrid();
    private final Select<String> taskFilter;
    private final Select<String> typeFilter;
    private final Select<String> statusFilter;
    private final FilterBar filterBar;

    private List<TaskDto> tasks = List.of();
    private Map<UUID, String> taskLabels = Map.of();

    public ScheduleView(ScheduleService scheduleService, TaskService taskService) {
        this.scheduleService = scheduleService;
        this.taskService = taskService;
        this.taskFilter = buildTaskFilter();
        this.typeFilter = buildTypeFilter();
        this.statusFilter = buildStatusFilter();
        this.filterBar = buildFilterBar();

        setSizeFull();
        setSpacing(false);
        setPadding(false);
        StyleConfig.create()
                .padding(Tokens.SPACE_L)
                .gap(Tokens.SPACE_M)
                .applyTo(this);

        add(buildToolbar(), filterBar, grid);

        grid.setTaskLabel(this::labelFor);
        grid.setOnView(this::viewSchedule);
        grid.setOnToggleActive(this::toggleActive);

        refresh();
    }

    /**
     * Applies the optional {@code task} query parameter (e.g. from the Tasks
     * grid's schedule-count badge) to the task filter so the grid opens
     * pre-filtered. An unknown or malformed task id is ignored, leaving the
     * list unfiltered — the same fallback {@code TaskView} uses for an unknown
     * {@code ?status=} value.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getLocation().getQueryParameters().getSingleParameter(ScheduleRoutes.QUERY_TASK)
                .flatMap(taskId -> resolveTaskFilterLabel(taskId, tasks))
                .ifPresent(taskFilter::setValue);
    }

    /**
     * Resolves a {@code ?task=<taskId>} query value to the matching task's
     * filter label, or empty if the id is missing, malformed, or unknown.
     */
    static Optional<String> resolveTaskFilterLabel(String taskId, List<TaskDto> tasks) {
        if (Strings.isBlank(taskId)) {
            return Optional.empty();
        }
        UUID parsed;
        try {
            parsed = UUID.fromString(taskId);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return tasks.stream()
                .filter(task -> task.id().equals(parsed))
                .map(TaskDto::label)
                .findFirst();
    }

    private FilterBar buildFilterBar() {
        return FilterBar.create()
                .onChange(this::applyFilter)
                .withFilter(taskFilter)
                .withFilter(typeFilter)
                .withFilter(statusFilter)
                .build();
    }

    private Select<String> buildTaskFilter() {
        Select<String> select = new Select<>();
        select.setLabel(ScheduleText.FILTER_TASK);
        select.setEmptySelectionAllowed(true);
        select.setEmptySelectionCaption(UiText.FILTER_ALL);
        select.setPlaceholder(UiText.FILTER_ALL);
        return select;
    }

    private Select<String> buildTypeFilter() {
        Select<String> select = Fields.select(ScheduleText.FILTER_TYPE,
                ScheduleType.ONCE.name(), ScheduleType.CRON.name(), ScheduleType.FIXED.name());
        select.setEmptySelectionAllowed(true);
        select.setEmptySelectionCaption(UiText.FILTER_ALL);
        select.setPlaceholder(UiText.FILTER_ALL);
        return select;
    }

    private Select<String> buildStatusFilter() {
        Select<String> select = Fields.select(ScheduleText.FILTER_STATUS,
                ScheduleText.STATUS_ACTIVE, ScheduleText.STATUS_PAUSED);
        select.setEmptySelectionAllowed(true);
        select.setEmptySelectionCaption(UiText.FILTER_ALL);
        select.setPlaceholder(UiText.FILTER_ALL);
        return select;
    }

    private void applyFilter() {
        grid.setFilter(buildPredicate());
    }

    private Predicate<ScheduleResponse> buildPredicate() {
        String term = filterBar.searchTerm();
        String task = taskFilter.getValue();
        String type = typeFilter.getValue();
        String status = statusFilter.getValue();
        return schedule -> matchesTask(schedule, task)
                && matchesType(schedule, type)
                && matchesStatus(schedule, status)
                && matchesTerm(schedule, term);
    }

    private boolean matchesTask(ScheduleResponse schedule, String task) {
        return Strings.isBlank(task) || task.equals(labelFor(schedule));
    }

    private boolean matchesType(ScheduleResponse schedule, String type) {
        return Strings.isBlank(type) || type.equals(schedule.type());
    }

    private boolean matchesStatus(ScheduleResponse schedule, String status) {
        if (Strings.isBlank(status)) {
            return true;
        }
        boolean wantActive = ScheduleText.STATUS_ACTIVE.equals(status);
        return schedule.active() == wantActive;
    }

    private boolean matchesTerm(ScheduleResponse schedule, String term) {
        if (term.isEmpty()) {
            return true;
        }
        return Strings.containsIgnoreCase(labelFor(schedule), term)
                || Strings.containsIgnoreCase(schedule.type(), term)
                || Strings.containsIgnoreCase(schedule.label(), term)
                || Strings.containsIgnoreCase(schedule.cronExpression(), term)
                || Strings.containsIgnoreCase(schedule.timezone(), term);
    }

    private HorizontalLayout buildToolbar() {
        H2 title = createTitle();
        Button newSchedule = Buttons.primary(ScheduleText.NEW_SCHEDULE, e -> openForm());
        HorizontalLayout toolbar = new HorizontalLayout(title, newSchedule);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        return toolbar;
    }

    private H2 createTitle() {
        H2 title = new H2(ScheduleText.TITLE);
        return StyleConfig.create()
                .fontSize(Tokens.FONT_XL)
                .applyTo(title);
    }

    private void refresh() {
        loadTasks();
        try {
            grid.setRows(scheduleService.listForTasks(taskLabels.keySet()));
        } catch (RuntimeException ex) {
            logger.error(ScheduleText.NOTIFY_LOAD_FAILED, ex);
            Notifications.error(ScheduleText.NOTIFY_LOAD_FAILED);
            grid.setRows(List.of());
        }
        applyFilter();
    }

    private void loadTasks() {
        try {
            tasks = taskService.list();
        } catch (RuntimeException ex) {
            logger.warn("Failed to load tasks — schedule task labels will be missing", ex);
            tasks = List.of();
        }
        taskLabels = tasks.stream()
                .collect(Collectors.toMap(TaskDto::id, TaskDto::label, (a, b) -> a));
        taskFilter.setItems(tasks.stream().map(TaskDto::label).distinct().toList());
    }

    private String labelFor(ScheduleResponse schedule) {
        return taskLabels.getOrDefault(schedule.taskId(), String.valueOf(schedule.taskId()));
    }

    private void openForm() {
        ScheduleForm.forCreate(tasks, this::createSchedule).open();
    }

    private void viewSchedule(ScheduleResponse schedule) {
        ScheduleDetails.of(schedule, this::editSchedule, this::confirmDelete).open();
    }

    private void editSchedule(ScheduleResponse schedule) {
        ScheduleForm.forEdit(schedule, request -> updateSchedule(schedule, request)).open();
    }

    private void confirmDelete(ScheduleResponse schedule) {
        Dialogs.confirmDelete(ScheduleText.CONFIRM_DELETE_TITLE, ScheduleText.CONFIRM_DELETE_TEXT,
                UiText.ACTION_DELETE, () -> deleteSchedule(schedule));
    }

    private void createSchedule(UUID taskId, CreateScheduleRequest request) {
        execute(() -> scheduleService.create(taskId, request),
                ScheduleText.NOTIFY_CREATED, ScheduleText.NOTIFY_CREATE_FAILED);
    }

    private void updateSchedule(ScheduleResponse schedule, UpdateScheduleRequest request) {
        execute(() -> scheduleService.update(schedule.id(), request),
                ScheduleText.NOTIFY_UPDATED, ScheduleText.NOTIFY_UPDATE_FAILED);
    }

    private void deleteSchedule(ScheduleResponse schedule) {
        execute(() -> scheduleService.delete(schedule.id()),
                ScheduleText.NOTIFY_DELETED, ScheduleText.NOTIFY_DELETE_FAILED);
    }

    private void toggleActive(ScheduleResponse schedule) {
        if (schedule.active()) {
            toggle(() -> scheduleService.pause(schedule.id()),
                    ScheduleText.NOTIFY_PAUSED, ScheduleText.NOTIFY_UPDATE_FAILED);
        } else {
            toggle(() -> scheduleService.resume(schedule.id()),
                    ScheduleText.NOTIFY_RESUMED, ScheduleText.NOTIFY_UPDATE_FAILED);
        }
    }

    private void execute(Runnable action, String successMessage, String failureMessage) {
        ViewActions.execute(action, successMessage, failureMessage, this::refresh, logger);
    }

    /**
     * Runs an optimistic Active-switch toggle: refreshes the grid whether the
     * pause/resume call succeeds or fails, so a failed toggle re-renders the row
     * from server state instead of leaving the switch flipped.
     */
    private void toggle(Runnable action, String successMessage, String failureMessage) {
        ViewActions.executeAndRefresh(action, successMessage, failureMessage, this::refresh, logger);
    }
}
