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

        refresh();
    }

    private void toggleActive(TaskDto task) {
        try {
            if (task.active()) {
                taskService.stop(task.id());
                Notifications.success(TaskText.NOTIFY_STOPPED);
            } else {
                taskService.start(task.id());
                Notifications.success(TaskText.NOTIFY_STARTED);
            }
            refresh();
        } catch (RuntimeException ex) {
            Notifications.error(TaskText.NOTIFY_UPDATE_FAILED);
        }
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

    private void deleteTask(TaskDto task) {
        try {
            taskService.delete(task.id());
            Notifications.success(TaskText.NOTIFY_DELETED);
            refresh();
        } catch (RuntimeException ex) {
            Notifications.error(TaskText.NOTIFY_DELETE_FAILED);
        }
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
        new TaskForm(jsonMapper, this::createTask).open();
    }

    private void createTask(CreateTaskRequest request) {
        try {
            taskService.create(request);
            Notifications.success(TaskText.NOTIFY_CREATED);
            refresh();
        } catch (RuntimeException ex) {
            Notifications.error(TaskText.NOTIFY_CREATE_FAILED);
        }
    }


}