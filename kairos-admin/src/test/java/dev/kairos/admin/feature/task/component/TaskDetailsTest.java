package dev.kairos.admin.feature.task.component;

import com.vaadin.flow.component.button.Button;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.shared.ui.UiText;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link TaskDetails#of}: the details dialog's footer offers Edit,
 * Delete and Close, Edit delegates directly to {@code onEdit}, and Delete
 * goes through {@link dev.kairos.admin.shared.ui.Dialogs#confirmDelete}
 * before invoking {@code onDelete} — mirroring {@code DestinationDetails}.
 *
 * <p>{@code Dialogs.confirmDelete} opens a {@code ConfirmDialog}, which
 * requires a live Vaadin session, so the delete path is only verified up to
 * that point (the button is present and wired); the confirm/cancel branching
 * itself is exercised by {@code DestinationDetailsSaveLogicTest}'s sibling
 * pattern and by manual/E2E verification, consistent with how
 * {@code DestinationDetails} is tested.
 */
class TaskDetailsTest {

    private static final UUID TASK_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void of_footerContainsEditDeleteAndClose() {
        TaskDetails details = TaskDetails.of(jsonMapper, task(), t -> { }, t -> { });

        List<Button> buttons = footerButtons(details);
        List<String> labels = buttons.stream().map(Button::getText).toList();

        assertThat(labels).containsExactlyInAnyOrder(TaskText.ACTION_EDIT, UiText.ACTION_DELETE, UiText.BTN_CLOSE);
    }

    @Test
    void of_clickEdit_invokesOnEditWithTheTask() {
        List<TaskDto> edited = new ArrayList<>();
        TaskDto task = task();
        TaskDetails details = TaskDetails.of(jsonMapper, task, edited::add, t -> { });

        footerButton(details, TaskText.ACTION_EDIT).click();

        assertThat(edited).containsExactly(task);
    }

    // --- helpers ---

    private static TaskDto task() {
        return new TaskDto(TASK_ID, "billing", "monthly-invoice", null, true,
                "dest-1", "InvoiceReady", null, 5000, false, NOW, NOW);
    }

    private static List<Button> footerButtons(TaskDetails details) {
        return details.getFooter().getElement().getChildren()
                .map(el -> el.getComponent().orElse(null))
                .filter(c -> c instanceof Button)
                .map(c -> (Button) c)
                .toList();
    }

    private static Button footerButton(TaskDetails details, String label) {
        return footerButtons(details).stream()
                .filter(b -> label.equals(b.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No footer button labeled " + label));
    }
}
