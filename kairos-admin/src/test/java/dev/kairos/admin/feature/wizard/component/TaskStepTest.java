package dev.kairos.admin.feature.wizard.component;

import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.wizard.WizardDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

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

    private JsonMapper jsonMapper;
    private TaskStep step;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        step = new TaskStep(jsonMapper);
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

    private void fillRequiredFields() {
        step.service().setValue(SERVICE);
        step.name().setValue(NAME);
        step.eventName().setValue(EVENT_NAME);
    }
}
