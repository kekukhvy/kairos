package dev.kairos.admin.feature.schedule.component;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ScheduleGrid}: it no longer exposes a per-row Actions column
 * (view/edit/toggle/delete now live behind a single-click row-open and an
 * in-grid toggle switch), a single row click opens the row's details, and
 * the Active column renders a switch reflecting the schedule's current state.
 *
 * <p>The switch's confirm/cancel wiring itself is covered by
 * {@link dev.kairos.admin.shared.ui.ToggleSwitchesTest}, since
 * {@code ScheduleGrid} delegates to {@code ToggleSwitches.build}.
 */
class ScheduleGridTest {

    private static final UUID SCHEDULE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TASK_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void columns_doNotIncludeActions() {
        ScheduleGrid grid = new ScheduleGrid();

        List<String> headers = grid.getColumns().stream()
                .map(Grid.Column::getHeaderText)
                .toList();

        assertThat(headers).containsExactly(
                ScheduleText.COL_TASK, ScheduleText.COL_TYPE, ScheduleText.COL_LABEL,
                ScheduleText.COL_WHEN, ScheduleText.COL_TIMEZONE, ScheduleText.COL_ACTIVE);
    }

    @Test
    void activeColumn_activeSchedule_rendersCheckedSwitch() {
        ScheduleGrid grid = new ScheduleGrid();
        ScheduleResponse schedule = schedule(true);
        grid.setRows(List.of(schedule));

        Checkbox toggle = activeToggleFor(grid, schedule);

        assertThat(toggle.getValue()).isTrue();
    }

    @Test
    void activeColumn_pausedSchedule_rendersUncheckedSwitch() {
        ScheduleGrid grid = new ScheduleGrid();
        ScheduleResponse schedule = schedule(false);
        grid.setRows(List.of(schedule));

        Checkbox toggle = activeToggleFor(grid, schedule);

        assertThat(toggle.getValue()).isFalse();
    }

    @Test
    void rowClick_invokesOnView() {
        ScheduleGrid grid = new ScheduleGrid();
        List<ScheduleResponse> viewed = new ArrayList<>();
        grid.setOnView(viewed::add);
        ScheduleResponse schedule = schedule(true);
        grid.setRows(List.of(schedule));

        fireRowClick(grid, schedule);

        assertThat(viewed).containsExactly(schedule);
    }

    // --- helpers ---

    private static ScheduleResponse schedule(boolean active) {
        return new ScheduleResponse(SCHEDULE_ID, TASK_ID, "ONCE", "label",
                NOW, null, null, "UTC", active, NOW, NOW);
    }

    private static Checkbox activeToggleFor(ScheduleGrid grid, ScheduleResponse schedule) {
        Grid.Column<ScheduleResponse> activeColumn = grid.getColumns().stream()
                .filter(c -> ScheduleText.COL_ACTIVE.equals(c.getHeaderText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Active column found"));

        @SuppressWarnings("unchecked")
        ComponentRenderer<Checkbox, ScheduleResponse> renderer =
                (ComponentRenderer<Checkbox, ScheduleResponse>) activeColumn.getRenderer();
        return renderer.createComponent(schedule);
    }

    private static void fireRowClick(ScheduleGrid grid, ScheduleResponse schedule) {
        String itemKey = grid.getDataCommunicator().getKeyMapper().key(schedule);
        ComponentUtil.fireEvent(grid,
                new ItemClickEvent<>(grid, true, itemKey, null, 0, 0, 0, 0, 1, 0, false, false, false, false));
    }
}
