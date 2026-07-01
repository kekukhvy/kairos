package dev.kairos.admin.feature.task.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.shared.ui.Badges;
import dev.kairos.admin.shared.ui.Buttons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TaskGrid extends Grid<TaskDto> {


    private Consumer<TaskDto> onView = task -> {
    };
    private Consumer<TaskDto> onEdit = task -> {
    };
    private Consumer<TaskDto> onToggleActive = task -> {
    };
    private Consumer<TaskDto> onDelete = task -> {
    };

    private final ListDataProvider<TaskDto> dataProvider = new ListDataProvider<>(new ArrayList<>());


    public TaskGrid() {
        super(TaskDto.class, false);

        addColumn(TaskDto::service).setHeader(TaskText.COL_SERVICE).setAutoWidth(true).setSortable(true);
        addColumn(TaskDto::name).setHeader(TaskText.COL_NAME).setAutoWidth(true).setSortable(true);
        addColumn(TaskDto::destinationId).setHeader(TaskText.COL_DESTINATION).setAutoWidth(true).setSortable(true);
        addColumn(TaskDto::eventName).setHeader(TaskText.COL_EVENT_NAME).setAutoWidth(true).setSortable(true);
        addComponentColumn(TaskGrid::statusBadge).setHeader(TaskText.COL_ACTIVE).setAutoWidth(true)
                .setComparator(TaskDto::active);
        addColumn(TaskDto::timeoutMs).setHeader(TaskText.COL_TIMEOUT).setAutoWidth(true).setSortable(true);

        addComponentColumn(this::actions)
                .setHeader(TaskText.COL_ACTIONS)
                .setAutoWidth(true)
                .setFlexGrow(0);

        setItems(dataProvider);
        setSizeFull();
    }

    /** Replaces the rows shown, preserving any active filter. */
    public void setRows(Collection<TaskDto> tasks) {
        dataProvider.getItems().clear();
        dataProvider.getItems().addAll(tasks);
        dataProvider.refreshAll();
    }

    /** Applies a client-side filter predicate; {@code null} shows all rows. */
    public void setFilter(Predicate<TaskDto> predicate) {
        dataProvider.setFilter(predicate == null ? null : predicate::test);
    }


    private HorizontalLayout actions(TaskDto task) {
        var view = Buttons.icon(VaadinIcon.EYE.create(), TaskText.ACTION_VIEW,
                _ -> onView.accept(task));

        var edit = Buttons.icon(VaadinIcon.EDIT.create(), TaskText.ACTION_EDIT,
                e -> onEdit.accept(task));

        boolean running = task.active();
        Icon toggleIcon = (running ? VaadinIcon.PAUSE : VaadinIcon.PLAY).create();
        String toggleTooltip = running ? TaskText.ACTION_STOP : TaskText.ACTION_START;
        var toggle = Buttons.icon(toggleIcon, toggleTooltip,
                e -> onToggleActive.accept(task));

        var delete = Buttons.iconDanger(VaadinIcon.TRASH.create(), TaskText.ACTION_DELETE,
                e -> onDelete.accept(task));

        HorizontalLayout layout = new HorizontalLayout(view, edit, toggle, delete);
        layout.setSpacing(false);
        return layout;
    }

    public void setOnView(Consumer<TaskDto> onView) {
        this.onView = onView;
    }

    public void setOnEdit(Consumer<TaskDto> onEdit) {
        this.onEdit = onEdit;
    }

    public void setOnToggleActive(Consumer<TaskDto> onToggleActive) {
        this.onToggleActive = onToggleActive;
    }

    public void setOnDelete(Consumer<TaskDto> onDelete) {
        this.onDelete = onDelete;
    }

    private static Span statusBadge(TaskDto task) {
        return task.active()
                ? Badges.success(TaskText.STATUS_ACTIVE)
                : Badges.neutral(TaskText.STATUS_INACTIVE);
    }
}