package dev.kairos.admin.feature.schedule.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import dev.kairos.admin.shared.ui.Badges;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.UiText;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Grid that lists a task's schedules. Columns are defined explicitly and the
 * "when" column resolves the type-specific field via {@link ScheduleWhen}.
 * Row actions (view, edit, pause/resume toggle, delete) are injected via
 * callbacks so the grid stays decoupled from service and navigation logic.
 */
public class ScheduleGrid extends Grid<ScheduleResponse> {

    private Consumer<ScheduleResponse> onView = schedule -> {
    };
    private Consumer<ScheduleResponse> onEdit = schedule -> {
    };
    private Consumer<ScheduleResponse> onToggleActive = schedule -> {
    };
    private Consumer<ScheduleResponse> onDelete = schedule -> {
    };

    private Function<ScheduleResponse, String> taskLabel = schedule -> String.valueOf(schedule.taskId());

    private final ListDataProvider<ScheduleResponse> dataProvider = new ListDataProvider<>(new ArrayList<>());

    public ScheduleGrid() {
        super(ScheduleResponse.class, false);

        addColumn(this::resolveTaskLabel).setHeader(ScheduleText.COL_TASK).setAutoWidth(true).setSortable(true);
        addColumn(ScheduleResponse::type).setHeader(ScheduleText.COL_TYPE).setAutoWidth(true).setSortable(true);
        addColumn(ScheduleResponse::label).setHeader(ScheduleText.COL_LABEL).setAutoWidth(true).setSortable(true);
        addColumn(ScheduleWhen::describe).setHeader(ScheduleText.COL_WHEN).setAutoWidth(true);
        addColumn(ScheduleResponse::timezone).setHeader(ScheduleText.COL_TIMEZONE).setAutoWidth(true).setSortable(true);
        addComponentColumn(ScheduleGrid::statusBadge).setHeader(ScheduleText.COL_ACTIVE).setAutoWidth(true)
                .setComparator(ScheduleResponse::active);

        addComponentColumn(this::actions)
                .setHeader(ScheduleText.COL_ACTIONS)
                .setAutoWidth(true)
                .setFlexGrow(0);

        setItems(dataProvider);
        setSizeFull();
    }

    /** Replaces the rows shown, preserving any active filter. */
    public void setRows(Collection<ScheduleResponse> schedules) {
        dataProvider.getItems().clear();
        dataProvider.getItems().addAll(schedules);
        dataProvider.refreshAll();
    }

    /** Applies a client-side filter predicate; {@code null} shows all rows. */
    public void setFilter(Predicate<ScheduleResponse> predicate) {
        dataProvider.setFilter(predicate == null ? null : predicate::test);
    }

    private HorizontalLayout actions(ScheduleResponse schedule) {
        var view = Buttons.icon(VaadinIcon.EYE.create(), ScheduleText.ACTION_VIEW,
                e -> onView.accept(schedule));

        var edit = Buttons.icon(VaadinIcon.EDIT.create(), ScheduleText.ACTION_EDIT,
                e -> onEdit.accept(schedule));

        boolean active = schedule.active();
        Icon toggleIcon = (active ? VaadinIcon.PAUSE : VaadinIcon.PLAY).create();
        String toggleTooltip = active ? ScheduleText.ACTION_PAUSE : ScheduleText.ACTION_RESUME;
        var toggle = Buttons.icon(toggleIcon, toggleTooltip,
                e -> onToggleActive.accept(schedule));

        var delete = Buttons.iconDanger(VaadinIcon.TRASH.create(), UiText.ACTION_DELETE,
                e -> onDelete.accept(schedule));

        HorizontalLayout layout = new HorizontalLayout(view, edit, toggle, delete);
        layout.setSpacing(false);
        return layout;
    }

    /** Sets the callback invoked when the view-details action is triggered for a row. */
    public void setOnView(Consumer<ScheduleResponse> onView) {
        this.onView = onView;
    }

    /** Sets the callback invoked when the edit action is triggered for a row. */
    public void setOnEdit(Consumer<ScheduleResponse> onEdit) {
        this.onEdit = onEdit;
    }

    /**
     * Sets the callback invoked when the pause/resume toggle is clicked.
     * The consumer receives the schedule in its <em>current</em> state; the
     * caller is responsible for deciding whether to call pause or resume.
     */
    public void setOnToggleActive(Consumer<ScheduleResponse> onToggleActive) {
        this.onToggleActive = onToggleActive;
    }

    /** Sets the callback invoked when the delete action is triggered for a row. */
    public void setOnDelete(Consumer<ScheduleResponse> onDelete) {
        this.onDelete = onDelete;
    }

    /**
     * Sets the resolver that turns a schedule into its owning task's display
     * label for the Task column; defaults to the raw task id.
     */
    public void setTaskLabel(Function<ScheduleResponse, String> taskLabel) {
        this.taskLabel = taskLabel;
    }

    private String resolveTaskLabel(ScheduleResponse schedule) {
        return taskLabel.apply(schedule);
    }

    private static Span statusBadge(ScheduleResponse schedule) {
        return schedule.active()
                ? Badges.success(ScheduleText.STATUS_ACTIVE)
                : Badges.neutral(ScheduleText.STATUS_PAUSED);
    }
}
