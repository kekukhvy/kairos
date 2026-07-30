package dev.kairos.admin.feature.wizard.component;

import com.vaadin.flow.component.BlurNotifier;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.textfield.TextField;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.feature.wizard.WizardDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@code TaskStep}'s validation and draft-population logic. The step
 * collects every {@code CreateTaskRequest} field except {@code destinationId}
 * — that is resolved later, from the Destination step, at commit time.
 */
class TaskStepTest {

    private static final String SERVICE = "billing";
    private static final String NAME = "invoice-sync";
    private static final String EVENT_NAME = "invoice.created";
    private static final String RESOLVED_DESTINATION_ID = "kafka-dest-1";
    private static final UUID EXISTING_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private JsonMapper jsonMapper;
    private TaskStep step;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        step = new TaskStep(jsonMapper, List.of());
    }

    @Test
    void validate_allRequiredFieldsBlank_returnsFalse() {
        assertThat(step.validate()).isFalse();
    }

    @Test
    void validate_requiredFieldsFilled_returnsTrue() {
        fillRequiredFields();

        assertThat(step.validate()).isTrue();
    }

    @Test
    void validate_missingService_returnsFalse() {
        fillRequiredFields();
        step.service().clear();

        assertThat(step.validate()).isFalse();
    }

    @Test
    void validate_missingName_returnsFalse() {
        fillRequiredFields();
        step.name().clear();

        assertThat(step.validate()).isFalse();
    }

    @Test
    void validate_missingEventName_returnsFalse() {
        fillRequiredFields();
        step.eventName().clear();

        assertThat(step.validate()).isFalse();
    }

    @Test
    void readInto_setsFactoryThatBuildsRequestWithResolvedDestinationId() {
        fillRequiredFields();
        WizardDraft draft = new WizardDraft();

        step.readInto(draft);

        CreateTaskRequest request = draft.taskRequestFactory().apply(RESOLVED_DESTINATION_ID);
        assertThat(request.destinationId()).isEqualTo(RESOLVED_DESTINATION_ID);
        assertThat(request.service()).isEqualTo(SERVICE);
        assertThat(request.name()).isEqualTo(NAME);
        assertThat(request.eventName()).isEqualTo(EVENT_NAME);
    }

    @Test
    void blurOnDuplicatePair_marksNameFieldInvalidWithDuplicateMessage() {
        TaskStep dupStep = new TaskStep(jsonMapper, List.of(existingTask()));
        fillRequiredFields(dupStep);

        blur(dupStep.name());

        assertThat(dupStep.name().isInvalid()).isTrue();
        assertThat(dupStep.name().getErrorMessage()).isEqualTo(TaskText.VALIDATION_DUPLICATE_SERVICE_NAME);
    }

    @Test
    void blurOnUniquePair_leavesNameFieldValid() {
        TaskStep dupStep = new TaskStep(jsonMapper, List.of(existingTask()));
        dupStep.service().setValue("shipping");
        dupStep.name().setValue("label-print");
        dupStep.eventName().setValue(EVENT_NAME);

        blur(dupStep.name());

        assertThat(dupStep.name().isInvalid()).isFalse();
    }

    @Test
    void validate_duplicatePair_returnsFalse() {
        TaskStep dupStep = new TaskStep(jsonMapper, List.of(existingTask()));
        fillRequiredFields(dupStep);

        assertThat(dupStep.validate()).isFalse();
    }

    private static void blur(TextField field) {
        ComponentUtil.fireEvent(field, new BlurNotifier.BlurEvent<>(field, false));
    }

    private static TaskDto existingTask() {
        return new TaskDto(EXISTING_ID, SERVICE, NAME, null, true,
                "dest-1", "InvoiceReady", null, 5000, false, NOW, NOW, 0);
    }

    private void fillRequiredFields() {
        fillRequiredFields(step);
    }

    private void fillRequiredFields(TaskStep target) {
        target.service().setValue(SERVICE);
        target.name().setValue(NAME);
        target.eventName().setValue(EVENT_NAME);
    }
}
