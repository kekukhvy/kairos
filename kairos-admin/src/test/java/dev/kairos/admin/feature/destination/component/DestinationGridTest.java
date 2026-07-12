package dev.kairos.admin.feature.destination.component;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.grid.ItemClickEvent;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link DestinationGrid}: a single click on a row opens that row's
 * details, matching {@code TaskGrid} and {@code ScheduleGrid} (which also use
 * {@code addItemClickListener}). This is the behaviour issue #33 unifies —
 * previously the grid required a double click.
 */
class DestinationGridTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void rowClick_invokesOnView() {
        DestinationGrid grid = new DestinationGrid(jsonMapper);
        List<DestinationDTO> viewed = new ArrayList<>();
        grid.setOnView(viewed::add);
        DestinationDTO destination = destination();
        grid.setRows(List.of(destination));

        fireRowClick(grid, destination);

        assertThat(viewed).containsExactly(destination);
    }

    // --- helpers ---

    private static DestinationDTO destination() {
        return new DestinationDTO("dest-kafka-1", "KAFKA", null, NOW);
    }

    private static void fireRowClick(DestinationGrid grid, DestinationDTO destination) {
        String itemKey = grid.getDataCommunicator().getKeyMapper().key(destination);
        ComponentUtil.fireEvent(grid,
                new ItemClickEvent<>(grid, true, itemKey, null, 0, 0, 0, 0, 1, 0, false, false, false, false));
    }
}
