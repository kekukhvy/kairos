package dev.kairos.admin.feature.schedule.component;

import com.vaadin.flow.component.button.Button;
import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ScheduleDetails#of}: the details dialog's footer offers Edit,
 * Delete and Close, and Edit delegates directly to {@code onEdit} — mirroring
 * {@code TaskDetails} and {@code DestinationDetails}.
 *
 * <p>{@code Dialogs.confirmDelete} opens a {@code ConfirmDialog}, which
 * requires a live Vaadin session, so the delete path is only verified up to
 * that point (the button is present and wired), consistent with how
 * {@code TaskDetailsTest} and {@code DestinationDetails} are tested.
 */
class ScheduleDetailsTest {

    private static final UUID SCHEDULE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID TASK_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void of_footerContainsEditDeleteAndClose() {
        ScheduleDetails details = ScheduleDetails.of(schedule(), s -> { }, s -> { });

        List<String> labels = footerButtons(details).stream().map(Button::getText).toList();

        assertThat(labels).containsExactlyInAnyOrder(ScheduleText.ACTION_EDIT, UiText.ACTION_DELETE, UiText.BTN_CLOSE);
    }

    @Test
    void of_clickEdit_invokesOnEditWithTheSchedule() {
        List<ScheduleResponse> edited = new ArrayList<>();
        ScheduleResponse schedule = schedule();
        ScheduleDetails details = ScheduleDetails.of(schedule, edited::add, s -> { });

        footerButton(details, ScheduleText.ACTION_EDIT).click();

        assertThat(edited).containsExactly(schedule);
    }

    // --- helpers ---

    private static ScheduleResponse schedule() {
        return new ScheduleResponse(SCHEDULE_ID, TASK_ID, "ONCE", "label",
                NOW, null, null, "UTC", true, NOW, NOW);
    }

    private static List<Button> footerButtons(ScheduleDetails details) {
        return details.getFooter().getElement().getChildren()
                .map(el -> el.getComponent().orElse(null))
                .filter(c -> c instanceof Button)
                .map(c -> (Button) c)
                .toList();
    }

    private static Button footerButton(ScheduleDetails details, String label) {
        return footerButtons(details).stream()
                .filter(b -> label.equals(b.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No footer button labeled " + label));
    }
}
