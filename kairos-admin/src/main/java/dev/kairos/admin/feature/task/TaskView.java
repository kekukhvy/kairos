package dev.kairos.admin.feature.task;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import dev.kairos.admin.feature.destination.DestinationService;
import dev.kairos.admin.feature.destination.DestinationText;
import dev.kairos.admin.feature.destination.component.DestinationDetails;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.feature.destination.dto.UpdateDestinationRequest;
import dev.kairos.admin.feature.schedule.ScheduleRoutes;
import dev.kairos.admin.feature.schedule.ScheduleService;
import dev.kairos.admin.feature.schedule.ScheduleView;
import dev.kairos.admin.feature.task.component.TaskDetails;
import dev.kairos.admin.feature.task.component.TaskForm;
import dev.kairos.admin.feature.task.component.TaskGrid;
import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.feature.task.dto.UpdateTaskRequest;
import dev.kairos.admin.feature.wizard.SetupWizard;
import dev.kairos.admin.feature.wizard.WizardText;
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
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.function.Predicate;

/**
 * Task management view: lists all tasks with status/destination filters, supports
 * create, edit, delete, activate/deactivate, and inline destination inspection.
 * Implements {@link BeforeEnterObserver} so the dashboard can deep-link to this
 * view with a pre-selected status filter via the {@code ?status=} query parameter.
 */
@Route(value = TaskRoutes.TASKS, layout = MainLayout.class)
@PageTitle(TaskRoutes.PAGE_TITLE)
public class TaskView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger logger = LoggerFactory.getLogger(TaskView.class);

    private final JsonMapper jsonMapper;
    private final TaskService taskService;
    private final DestinationService destinationService;
    private final ScheduleService scheduleService;
    private final TaskGrid grid = new TaskGrid();
    private final Select<String> statusFilter;
    private final ComboBox<String> destinationFilter;
    private final FilterBar filterBar;

    /**
     * Constructs the task view: wires filters, builds the toolbar and grid, and
     * performs the initial data load.
     *
     * @param jsonMapper           used by detail and form dialogs for JSON pretty-printing
     * @param taskService          data access for task CRUD and lifecycle operations
     * @param destinationService   data access used to populate the destination filter and inline dialogs
     * @param scheduleService      data access the guided setup wizard lists/commits schedules through
     */
    public TaskView(JsonMapper jsonMapper, TaskService taskService, DestinationService destinationService,
                    ScheduleService scheduleService) {
        this.jsonMapper = jsonMapper;
        this.taskService = taskService;
        this.destinationService = destinationService;
        this.scheduleService = scheduleService;
        this.statusFilter = buildStatusFilter();
        this.destinationFilter = buildDestinationFilter();
        this.filterBar = buildFilterBar();

        setSizeFull();
        setSpacing(false);
        setPadding(false);


        StyleConfig.create()
                .padding(Tokens.SPACE_L)
                .gap(Tokens.SPACE_M)
                .applyTo(this);

        add(buildToolbar(), filterBar, grid);

        grid.setOnToggleActive(this::toggleActive);
        grid.setOnView(this::viewTask);
        grid.setOnOpenDestination(this::openDestination);
        grid.setOnOpenSchedules(this::openSchedules);
        refresh();
    }

    /**
     * Applies the optional {@code status} query parameter (e.g. from a dashboard
     * deep-link) to the status filter so the grid opens pre-filtered.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getLocation().getQueryParameters().getSingleParameter(TaskRoutes.QUERY_STATUS)
                .filter(status -> TaskText.STATUS_ACTIVE.equals(status)
                        || TaskText.STATUS_INACTIVE.equals(status))
                .ifPresent(statusFilter::setValue);
    }

    /** Navigates to Schedules with the task filter pre-selected via {@code ?task=<taskId>}. */
    private void openSchedules(TaskDto task) {
        QueryParameters query = QueryParameters.of(ScheduleRoutes.QUERY_TASK, task.id().toString());
        UI.getCurrent().navigate(ScheduleView.class, query);
    }

    private void openDestination(String destinationId) {
        DestinationDTO destination;
        try {
            destination = destinationService.getById(destinationId);
        } catch (RuntimeException ex) {
            logger.error("Failed to load destination {}", destinationId, ex);
            Notifications.error(DestinationText.NOTIFY_LOAD_FAILED);
            return;
        }
        DestinationDetails.of(jsonMapper, destination,
                request -> updateDestination(destinationId, request),
                d -> deleteDestination(destinationId)).open();
    }

    private void updateDestination(String destinationId, UpdateDestinationRequest request) {
        execute(() -> destinationService.update(destinationId, request),
                DestinationText.NOTIFY_UPDATED, DestinationText.NOTIFY_UPDATE_FAILED);
    }

    private void deleteDestination(String destinationId) {
        execute(() -> destinationService.delete(destinationId),
                DestinationText.NOTIFY_DELETED, DestinationText.NOTIFY_DELETE_FAILED);
    }

    private FilterBar buildFilterBar() {
        return FilterBar.create()
                .onChange(this::applyFilter)
                .withFilter(statusFilter)
                .withFilter(destinationFilter)
                .build();
    }

    private Select<String> buildStatusFilter() {
        Select<String> select = Fields.select(TaskText.FILTER_STATUS,
                TaskText.STATUS_ACTIVE, TaskText.STATUS_INACTIVE);
        select.setEmptySelectionAllowed(true);
        select.setEmptySelectionCaption(UiText.FILTER_ALL);
        select.setPlaceholder(UiText.FILTER_ALL);
        return select;
    }

    private ComboBox<String> buildDestinationFilter() {
        ComboBox<String> combo = Fields.combo(TaskText.FILTER_DESTINATION, List.of());
        combo.setPlaceholder(UiText.FILTER_ALL);
        return combo;
    }

    private void applyFilter() {
        grid.setFilter(buildPredicate());
    }

    private Predicate<TaskDto> buildPredicate() {
        String term = filterBar.searchTerm();
        String status = statusFilter.getValue();
        String destination = destinationFilter.getValue();
        return task -> matchesStatus(task, status)
                && matchesDestination(task, destination)
                && matchesTerm(task, term);
    }

    private boolean matchesStatus(TaskDto task, String status) {
        if (Strings.isBlank(status)) {
            return true;
        }
        boolean wantActive = TaskText.STATUS_ACTIVE.equals(status);
        return task.active() == wantActive;
    }

    private boolean matchesDestination(TaskDto task, String destination) {
        return Strings.isBlank(destination) || destination.equals(task.destinationId());
    }

    private boolean matchesTerm(TaskDto task, String term) {
        if (term.isEmpty()) {
            return true;
        }
        return Strings.containsIgnoreCase(task.service(), term)
                || Strings.containsIgnoreCase(task.name(), term)
                || Strings.containsIgnoreCase(task.destinationId(), term)
                || Strings.containsIgnoreCase(task.eventName(), term);
    }

    private void confirmDelete(TaskDto task) {
        Dialogs.confirmDelete(TaskText.CONFIRM_DELETE_TITLE, TaskText.CONFIRM_DELETE_TEXT,
                UiText.ACTION_DELETE, () -> deleteTask(task));
    }


    private HorizontalLayout buildToolbar() {
        H2 title = createTitle();

        Button newTask = Buttons.primary(TaskText.NEW_TASK, e -> openForm());
        Button guidedSetup = Buttons.secondary(TaskText.GUIDED_SETUP, e -> openWizard());

        HorizontalLayout actions = new HorizontalLayout(newTask, guidedSetup);
        actions.setAlignItems(HorizontalLayout.Alignment.CENTER);

        HorizontalLayout toolbar = new HorizontalLayout(title, actions);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        return toolbar;
    }

    private void openWizard() {
        try {
            new SetupWizard(jsonMapper, taskService, destinationService, scheduleService, this::refresh).open();
        } catch (RuntimeException ex) {
            logger.error("Failed to open setup wizard", ex);
            Notifications.error(WizardText.NOTIFY_OPEN_FAILED);
        }
    }

    private void viewTask(TaskDto task) {
        TaskDetails.of(jsonMapper, task, this::editTask, this::confirmDelete).open();
    }

    private H2 createTitle() {
        H2 title = new H2(TaskText.TITLE);
        return StyleConfig.create()
                .fontSize(Tokens.FONT_XL)
                .applyTo(title);
    }

    private void refresh() {
        grid.setRows(taskService.list());
        destinationFilter.setItems(destinationIds());
        applyFilter();
    }

    private void openForm() {
        TaskForm.forCreate(jsonMapper, destinationIds(), taskService.list(), this::createTask).open();
    }

    private void editTask(TaskDto task) {
        TaskForm.forEdit(jsonMapper, destinationIds(), taskService.list(), task, request -> updateTask(task, request))
                .open();
    }

    private List<String> destinationIds() {
        return destinationService.list().stream()
                .map(DestinationDTO::destinationId)
                .toList();
    }

    private void updateTask(TaskDto task, UpdateTaskRequest request) {
        execute(() -> taskService.update(task.id(), request), TaskText.NOTIFY_UPDATED, TaskText.NOTIFY_UPDATE_FAILED);
    }


    private void createTask(CreateTaskRequest request) {
        execute(() -> taskService.create(request), TaskText.NOTIFY_CREATED, TaskText.NOTIFY_CREATE_FAILED);
    }

    private void deleteTask(TaskDto task) {
        execute(() -> taskService.delete(task.id()), TaskText.NOTIFY_DELETED, TaskText.NOTIFY_DELETE_FAILED);
    }

    private void toggleActive(TaskDto task) {
        if (task.active()) {
            toggle(() -> taskService.stop(task.id()), TaskText.NOTIFY_STOPPED, TaskText.NOTIFY_UPDATE_FAILED);
        } else {
            toggle(() -> taskService.start(task.id()), TaskText.NOTIFY_STARTED, TaskText.NOTIFY_UPDATE_FAILED);
        }
    }


    private void execute(Runnable action, String successMessage, String failureMessage) {
        ViewActions.execute(action, successMessage, failureMessage, this::refresh, logger);
    }

    /**
     * Runs an optimistic Active-switch toggle: refreshes the grid whether the
     * start/stop call succeeds or fails, so a failed toggle re-renders the row
     * from server state instead of leaving the switch flipped.
     */
    private void toggle(Runnable action, String successMessage, String failureMessage) {
        ViewActions.executeAndRefresh(action, successMessage, failureMessage, this::refresh, logger);
    }

}