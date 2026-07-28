package dev.kairos.admin.feature.schedule;

import dev.kairos.admin.feature.task.dto.TaskDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ScheduleView#resolveTaskFilterLabel}: the pure lookup behind
 * the {@code ?task=<taskId>} deep-link (mirroring how {@code TaskView} handles
 * {@code ?status=}). A known task id resolves to that task's filter label; an
 * unknown or malformed id resolves to empty, leaving the list unfiltered.
 */
class ScheduleViewTest {

    private static final UUID KNOWN_TASK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_TASK_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String MALFORMED_TASK_ID = "not-a-uuid";

    private final List<TaskDto> tasks = List.of(
            task(KNOWN_TASK_ID, "billing", "monthly-invoice"),
            task(OTHER_TASK_ID, "shipping", "dispatch-notice"));

    @Test
    void resolveTaskFilterLabel_knownTaskId_returnsItsLabel() {
        Optional<String> label = ScheduleView.resolveTaskFilterLabel(KNOWN_TASK_ID.toString(), tasks);

        assertThat(label).contains("billing / monthly-invoice");
    }

    @Test
    void resolveTaskFilterLabel_unknownTaskId_returnsEmpty() {
        UUID unknownId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        Optional<String> label = ScheduleView.resolveTaskFilterLabel(unknownId.toString(), tasks);

        assertThat(label).isEmpty();
    }

    @Test
    void resolveTaskFilterLabel_malformedTaskId_returnsEmpty() {
        Optional<String> label = ScheduleView.resolveTaskFilterLabel(MALFORMED_TASK_ID, tasks);

        assertThat(label).isEmpty();
    }

    @Test
    void resolveTaskFilterLabel_nullTaskId_returnsEmpty() {
        Optional<String> label = ScheduleView.resolveTaskFilterLabel(null, tasks);

        assertThat(label).isEmpty();
    }

    private static TaskDto task(UUID id, String service, String name) {
        return new TaskDto(id, service, name, null, true,
                "dest-1", "Event", null, 5000, false, NOW, NOW, 0);
    }
}
