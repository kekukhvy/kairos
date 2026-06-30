package dev.kairos.admin.feature.task;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import dev.kairos.admin.feature.task.component.TaskDetails;
import dev.kairos.admin.feature.task.component.TaskForm;
import dev.kairos.admin.feature.task.component.TaskGrid;
import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.feature.task.dto.UpdateTaskRequest;
import dev.kairos.admin.shared.layout.MainLayout;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Notifications;
import tools.jackson.databind.json.JsonMapper;

import static dev.kairos.admin.shared.style.Tokens.THEME_DANGER_CONFIRM;

@Route(value = TaskRoutes.TASKS, layout = MainLayout.class)
@RouteAlias(value = TaskRoutes.ROOT, layout = MainLayout.class)
@PageTitle(TaskRoutes.PAGE_TITLE)
public class TaskView extends VerticalLayout {

    private final JsonMapper jsonMapper;
    private final TaskService taskService;
    private final TaskGrid grid = new TaskGrid();

    public TaskView(JsonMapper jsonMapper, TaskService taskService) {
        this.jsonMapper = jsonMapper;
        this.taskService = taskService;

        setSizeFull();
        setSpacing(false);
        setPadding(false);


        StyleConfig.create()
                .padding(Tokens.SPACE_L)
                .gap(Tokens.SPACE_M)
                .applyTo(this);

        add(buildToolbar(), grid);

        grid.setOnToggleActive(this::toggleActive);
        grid.setOnDelete(this::confirmDelete);
        grid.setOnView(this::viewTask);
        grid.setOnEdit(this::editTask);
        refresh();
    }

    private void confirmDelete(TaskDto task) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(TaskText.CONFIRM_DELETE_TITLE);
        dialog.setText(TaskText.CONFIRM_DELETE_TEXT);
        dialog.setCancelable(true);
        dialog.setConfirmText(TaskText.ACTION_DELETE);
        dialog.setConfirmButtonTheme(THEME_DANGER_CONFIRM);
        dialog.addConfirmListener(e -> deleteTask(task));
        dialog.open();
    }


    private HorizontalLayout buildToolbar() {
        H2 title = createTitle();

        Button newTask = Buttons.primary(TaskText.NEW_TASK, e -> openForm());

        HorizontalLayout toolbar = new HorizontalLayout(title, newTask);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        return toolbar;
    }

    private void viewTask(TaskDto task) {
        new TaskDetails(jsonMapper, task).open();
    }

    private H2 createTitle() {
        H2 title = new H2(TaskText.TITLE);
        return StyleConfig.create()
                .fontSize(Tokens.FONT_XL)
                .applyTo(title);
    }

    private void refresh() {
        grid.setItems(taskService.list());
    }

    private void openForm() {
        TaskForm.forCreate(jsonMapper, this::createTask).open();
    }

    private void editTask(TaskDto task) {
        TaskForm.forEdit(jsonMapper, task, request -> updateTask(task, request)).open();
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
            execute(() -> taskService.stop(task.id()), TaskText.NOTIFY_STOPPED, TaskText.NOTIFY_UPDATE_FAILED);
        } else {
            execute(() -> taskService.start(task.id()), TaskText.NOTIFY_STARTED, TaskText.NOTIFY_UPDATE_FAILED);
        }
    }


    private void execute(Runnable action, String successMessage, String failureMessage) {
        try {
            action.run();
            Notifications.success(successMessage);
            refresh();
        } catch (RuntimeException ex) {
            Notifications.error(failureMessage);
        }
    }

}