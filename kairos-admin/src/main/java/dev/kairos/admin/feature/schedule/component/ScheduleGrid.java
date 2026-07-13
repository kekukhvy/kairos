package dev.kairos.admin.feature.schedule.component;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.ListDataProvider;
import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.admin.shared.ui.Dialogs;
import dev.kairos.admin.shared.ui.ToggleSwitches;
import dev.kairos.common.dto.schedule.ScheduleResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Grid that lists a task's schedules. Columns are defined explicitly and the
 * "when" column resolves the type-specific field via {@link ScheduleWhen}. A
 * single click on a row opens its details (which host edit/delete); the
 * Active column renders a confirm-before-toggle switch. Both are injected
 * via callbacks so the grid stays decoupled from service and navigation logic.
 */
public class ScheduleGrid extends Grid<ScheduleResponse> {

    private Consumer<ScheduleResponse> onView = schedule -> {
    };
    private Consumer<ScheduleResponse> onToggleActive = schedule -> {
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
        addComponentColumn(this::activeToggle).setHeader(ScheduleText.COL_ACTIVE).setAutoWidth(true)
                .setComparator(ScheduleResponse::active);

        addItemClickListener(event -> onView.accept(event.getItem()));

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

    private Checkbox activeToggle(ScheduleResponse schedule) {
        return ToggleSwitches.build(schedule.active(), this::confirmToggle,
                active -> onToggleActive.accept(schedule));
    }

    private void confirmToggle(boolean pendingValue, Runnable onConfirm, Runnable onCancel) {
        String title = pendingValue ? ScheduleText.CONFIRM_ENABLE_TITLE : ScheduleText.CONFIRM_DISABLE_TITLE;
        String text = pendingValue ? ScheduleText.CONFIRM_ENABLE_TEXT : ScheduleText.CONFIRM_DISABLE_TEXT;
        String confirmText = pendingValue ? ScheduleText.ACTION_RESUME : ScheduleText.ACTION_PAUSE;
        Dialogs.confirmToggle(title, text, confirmText, onConfirm, onCancel);
    }

    /** Sets the callback invoked when a row is clicked to open its details. */
    public void setOnView(Consumer<ScheduleResponse> onView) {
        this.onView = onView;
    }

    /**
     * Sets the callback invoked when the operator confirms an Active switch
     * toggle. The consumer receives the schedule in its <em>current</em>
     * (pre-toggle) state; the caller is responsible for deciding whether to
     * call pause or resume.
     */
    public void setOnToggleActive(Consumer<ScheduleResponse> onToggleActive) {
        this.onToggleActive = onToggleActive;
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
}
