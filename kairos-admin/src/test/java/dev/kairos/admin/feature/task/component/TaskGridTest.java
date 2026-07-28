package dev.kairos.admin.feature.task.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.shared.style.Tokens;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link TaskGrid}: it no longer exposes a per-row Actions column
 * (view/edit/toggle/delete now live behind a single-click row-open and an
 * in-grid toggle switch), a single row click opens the row's details, and
 * the Active column renders a switch reflecting the task's current state.
 *
 * <p>The switch's confirm/cancel wiring itself is covered by
 * {@link dev.kairos.admin.shared.ui.ToggleSwitchesTest}, since
 * {@code TaskGrid} delegates to {@code ToggleSwitches.build}.
 */
class TaskGridTest {

    private static final UUID TASK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void columns_doNotIncludeActions() {
        TaskGrid grid = new TaskGrid();

        List<String> headers = grid.getColumns().stream()
                .map(Grid.Column::getHeaderText)
                .toList();

        assertThat(headers).containsExactly(
                TaskText.COL_SERVICE, TaskText.COL_NAME, TaskText.COL_DESTINATION,
                TaskText.COL_EVENT_NAME, TaskText.COL_ACTIVE, TaskText.COL_TIMEOUT);
    }

    @Test
    void activeColumn_activeTask_rendersCheckedSwitch() {
        TaskGrid grid = new TaskGrid();
        TaskDto task = activeTask(true);
        grid.setRows(List.of(task));

        Checkbox toggle = activeToggleFor(grid, task);

        assertThat(toggle.getValue()).isTrue();
    }

    @Test
    void activeColumn_inactiveTask_rendersUncheckedSwitch() {
        TaskGrid grid = new TaskGrid();
        TaskDto task = activeTask(false);
        grid.setRows(List.of(task));

        Checkbox toggle = activeToggleFor(grid, task);

        assertThat(toggle.getValue()).isFalse();
    }

    @Test
    void rowClick_invokesOnView() {
        TaskGrid grid = new TaskGrid();
        List<TaskDto> viewed = new ArrayList<>();
        grid.setOnView(viewed::add);
        TaskDto task = activeTask(true);
        grid.setRows(List.of(task));

        fireRowClick(grid, task);

        assertThat(viewed).containsExactly(task);
    }

    // ── name cell: badge visibility + zero-active styling ────────────────────

    @Test
    void nameCell_zeroActiveSchedules_isRenderedInErrorColor() {
        TaskGrid grid = new TaskGrid();
        TaskDto task = taskWithActiveScheduleCount(0);
        grid.setRows(List.of(task));

        Span name = nameSpanFor(grid, task);

        assertThat(name.getStyle().get("color")).isEqualTo(Tokens.COLOR_ERROR);
    }

    @Test
    void nameCell_zeroActiveSchedules_hasExplanatoryTooltip() {
        TaskGrid grid = new TaskGrid();
        TaskDto task = taskWithActiveScheduleCount(0);
        grid.setRows(List.of(task));

        Span name = nameSpanFor(grid, task);

        assertThat(Tooltip.forComponent(name).getText()).isEqualTo(TaskText.TOOLTIP_NO_ACTIVE_SCHEDULES);
    }

    @Test
    void nameCell_zeroActiveSchedules_showsNoBadge() {
        TaskGrid grid = new TaskGrid();
        TaskDto task = taskWithActiveScheduleCount(0);
        grid.setRows(List.of(task));

        Component nameCell = nameCellFor(grid, task);

        assertThat(badgeIn(nameCell)).isEmpty();
    }

    @Test
    void nameCell_oneActiveSchedule_isRenderedInDefaultColor() {
        TaskGrid grid = new TaskGrid();
        TaskDto task = taskWithActiveScheduleCount(1);
        grid.setRows(List.of(task));

        Span name = nameSpanFor(grid, task);

        assertThat(name.getStyle().get("color")).isNull();
    }

    @Test
    void nameCell_oneActiveSchedule_showsNoBadge() {
        TaskGrid grid = new TaskGrid();
        TaskDto task = taskWithActiveScheduleCount(1);
        grid.setRows(List.of(task));

        Component nameCell = nameCellFor(grid, task);

        assertThat(badgeIn(nameCell)).isEmpty();
    }

    @Test
    void nameCell_multipleActiveSchedules_showsBadgeWithCount() {
        TaskGrid grid = new TaskGrid();
        TaskDto task = taskWithActiveScheduleCount(3);
        grid.setRows(List.of(task));

        Component nameCell = nameCellFor(grid, task);

        assertThat(badgeIn(nameCell)).map(Span::getText).contains("3");
    }

    @Test
    void nameCell_multipleActiveSchedules_isRenderedInDefaultColor() {
        TaskGrid grid = new TaskGrid();
        TaskDto task = taskWithActiveScheduleCount(3);
        grid.setRows(List.of(task));

        Span name = nameSpanFor(grid, task);

        assertThat(name.getStyle().get("color")).isNull();
    }

    @Test
    void nameCell_badgeClick_doesNotInvokeOnView() {
        TaskGrid grid = new TaskGrid();
        List<TaskDto> viewed = new ArrayList<>();
        grid.setOnView(viewed::add);
        TaskDto task = taskWithActiveScheduleCount(3);
        grid.setRows(List.of(task));

        Component nameCell = nameCellFor(grid, task);
        Span badge = badgeIn(nameCell).orElseThrow(() -> new AssertionError("No badge found"));
        badge.getElement().executeJs("this.click()");

        assertThat(viewed).isEmpty();
    }

    // --- helpers ---

    private static TaskDto activeTask(boolean active) {
        return new TaskDto(TASK_ID, "billing", "monthly-invoice", null, active,
                "dest-1", "InvoiceReady", null, 5000, false, NOW, NOW, 1);
    }

    private static TaskDto taskWithActiveScheduleCount(long activeScheduleCount) {
        return new TaskDto(TASK_ID, "billing", "monthly-invoice", null, true,
                "dest-1", "InvoiceReady", null, 5000, false, NOW, NOW, activeScheduleCount);
    }

    private static Component nameCellFor(TaskGrid grid, TaskDto task) {
        Grid.Column<TaskDto> nameColumn = grid.getColumns().stream()
                .filter(c -> TaskText.COL_NAME.equals(c.getHeaderText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Name column found"));

        @SuppressWarnings("unchecked")
        ComponentRenderer<Component, TaskDto> renderer =
                (ComponentRenderer<Component, TaskDto>) nameColumn.getRenderer();
        return renderer.createComponent(task);
    }

    private static Span nameSpanFor(TaskGrid grid, TaskDto task) {
        Component nameCell = nameCellFor(grid, task);
        return nameCell.getChildren()
                .filter(Span.class::isInstance)
                .map(Span.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No name Span found in cell"));
    }

    private static java.util.Optional<Span> badgeIn(Component nameCell) {
        return nameCell.getChildren()
                .filter(Span.class::isInstance)
                .map(Span.class::cast)
                .filter(span -> span.getElement().getThemeList().contains(Tokens.THEME_BADGE))
                .findFirst();
    }

    private static Checkbox activeToggleFor(TaskGrid grid, TaskDto task) {
        Grid.Column<TaskDto> activeColumn = grid.getColumns().stream()
                .filter(c -> TaskText.COL_ACTIVE.equals(c.getHeaderText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Active column found"));

        @SuppressWarnings("unchecked")
        ComponentRenderer<Checkbox, TaskDto> renderer =
                (ComponentRenderer<Checkbox, TaskDto>) activeColumn.getRenderer();
        return renderer.createComponent(task);
    }

    private static void fireRowClick(TaskGrid grid, TaskDto task) {
        String itemKey = grid.getDataCommunicator().getKeyMapper().key(task);
        ComponentUtil.fireEvent(grid,
                new ItemClickEvent<>(grid, true, itemKey, null, 0, 0, 0, 0, 1, 0, false, false, false, false));
    }
}
