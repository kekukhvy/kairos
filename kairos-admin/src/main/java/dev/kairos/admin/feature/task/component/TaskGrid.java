package dev.kairos.admin.feature.task.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.data.provider.ListDataProvider;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Dialogs;
import dev.kairos.admin.shared.ui.ToggleSwitches;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Grid that lists task records in the admin UI. Columns are defined
 * explicitly (auto-detection disabled) and sized to their content. A single
 * click on a row opens its details (which host edit/delete); the Active
 * column renders a confirm-before-toggle switch. Both are injected via
 * callbacks so the grid stays decoupled from service and navigation logic.
 */
public class TaskGrid extends Grid<TaskDto> {

    /** {@code activeScheduleCount} threshold above which the name cell shows a badge. */
    private static final long BADGE_THRESHOLD = 1L;

    /** {@code activeScheduleCount} below which the task name is coloured as an error. */
    private static final long NO_ACTIVE_SCHEDULES = 0L;

    /** DOM event fired when the schedule-count badge is clicked. */
    private static final String EVENT_CLICK = "click";

    private Consumer<TaskDto> onView = task -> {
    };
    private Consumer<TaskDto> onToggleActive = task -> {
    };
    private Consumer<String> onOpenDestination = destinationId -> {
    };
    private Consumer<TaskDto> onOpenSchedules = task -> {
    };

    private final ListDataProvider<TaskDto> dataProvider = new ListDataProvider<>(new ArrayList<>());


    public TaskGrid() {
        super(TaskDto.class, false);

        addColumn(TaskDto::service).setHeader(TaskText.COL_SERVICE).setAutoWidth(true).setSortable(true);
        addComponentColumn(this::nameCell).setHeader(TaskText.COL_NAME).setAutoWidth(true)
                .setComparator(TaskDto::name);
        addComponentColumn(this::destinationLink).setHeader(TaskText.COL_DESTINATION).setAutoWidth(true)
                .setComparator(TaskDto::destinationId);
        addColumn(TaskDto::eventName).setHeader(TaskText.COL_EVENT_NAME).setAutoWidth(true).setSortable(true);
        addComponentColumn(this::activeToggle).setHeader(TaskText.COL_ACTIVE).setAutoWidth(true)
                .setComparator(TaskDto::active);
        addColumn(TaskDto::timeoutMs).setHeader(TaskText.COL_TIMEOUT).setAutoWidth(true).setSortable(true);

        addItemClickListener(event -> onView.accept(event.getItem()));

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


    /**
     * Builds the task-name cell: the name alone for 0/1 active schedules (0
     * additionally coloured as an error, with an explanatory tooltip), or the
     * name plus a clickable count badge for more than one.
     */
    private Component nameCell(TaskDto task) {
        Div cell = new Div(nameSpan(task));
        if (task.activeScheduleCount() > BADGE_THRESHOLD) {
            cell.add(scheduleBadge(task));
        }
        return cell;
    }

    private Span nameSpan(TaskDto task) {
        Span name = new Span(task.name());
        if (task.activeScheduleCount() == NO_ACTIVE_SCHEDULES) {
            StyleConfig.create().color(Tokens.COLOR_ERROR).applyTo(name);
            Tooltip.forComponent(name).withText(TaskText.TOOLTIP_NO_ACTIVE_SCHEDULES);
        }
        return name;
    }

    private Span scheduleBadge(TaskDto task) {
        Span badge = new Span(String.valueOf(task.activeScheduleCount()));
        badge.getElement().getThemeList().add(Tokens.THEME_BADGE_CONTRAST);
        badge.getElement().addEventListener(EVENT_CLICK, event -> onOpenSchedules.accept(task))
                .stopPropagation();
        StyleConfig.create()
                .cursor(Tokens.CURSOR_POINTER)
                .marginInlineStart(Tokens.SPACE_XS)
                .applyTo(badge);
        return badge;
    }

    private Component destinationLink(TaskDto task) {
        String destinationId = task.destinationId();
        if (destinationId == null || destinationId.isBlank()) {
            return new Span();
        }
        return Buttons.link(destinationId, e -> onOpenDestination.accept(destinationId));
    }

    private Checkbox activeToggle(TaskDto task) {
        return ToggleSwitches.build(task.active(), this::confirmToggle,
                active -> onToggleActive.accept(task));
    }

    private void confirmToggle(boolean pendingValue, Runnable onConfirm, Runnable onCancel) {
        String title = pendingValue ? TaskText.CONFIRM_ENABLE_TITLE : TaskText.CONFIRM_DISABLE_TITLE;
        String text = pendingValue ? TaskText.CONFIRM_ENABLE_TEXT : TaskText.CONFIRM_DISABLE_TEXT;
        String confirmText = pendingValue ? TaskText.ACTION_START : TaskText.ACTION_STOP;
        Dialogs.confirmToggle(title, text, confirmText, onConfirm, onCancel);
    }

    /** Sets the callback invoked when a row is clicked to open its details. */
    public void setOnView(Consumer<TaskDto> onView) {
        this.onView = onView;
    }

    /**
     * Sets the callback invoked when the operator confirms an Active switch
     * toggle. The consumer receives the task in its <em>current</em> (pre-toggle)
     * state; the caller is responsible for deciding whether to call start or stop.
     */
    public void setOnToggleActive(Consumer<TaskDto> onToggleActive) {
        this.onToggleActive = onToggleActive;
    }

    /** Sets the callback invoked with the destination id when a destination link is clicked. */
    public void setOnOpenDestination(Consumer<String> onOpenDestination) {
        this.onOpenDestination = onOpenDestination;
    }

    /** Sets the callback invoked with the task when its schedule-count badge is clicked. */
    public void setOnOpenSchedules(Consumer<TaskDto> onOpenSchedules) {
        this.onOpenSchedules = onOpenSchedules;
    }
}
