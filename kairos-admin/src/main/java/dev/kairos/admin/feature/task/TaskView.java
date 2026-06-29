package dev.kairos.admin.feature.task;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import dev.kairos.admin.feature.task.component.TaskForm;
import dev.kairos.admin.feature.task.component.TaskGrid;
import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.shared.layout.MainLayout;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import tools.jackson.databind.json.JsonMapper;

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
        refresh();
    }


    private HorizontalLayout buildToolbar() {
        H2 title = createTitle();

        Button newTask = new Button(TaskText.NEW_TASK, e -> openForm());
        newTask.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(title, newTask);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        return toolbar;
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
            Notification.show(TaskText.NOTIFY_CREATED);
            refresh();
        } catch (RuntimeException ex) {
            Notification.show(TaskText.NOTIFY_CREATE_FAILED);
        }
    }
}