package dev.kairos.admin.feature.wizard.component;

import dev.kairos.admin.feature.destination.dto.CreateDestinationRequest;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.feature.wizard.WizardDraft;
import dev.kairos.admin.feature.wizard.WizardMode;
import dev.kairos.admin.shared.ui.UiText;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@code DestinationStep}'s two modes: selecting an existing
 * destination (a {@code ComboBox} of loaded items) vs. creating a new one
 * inline. Only the active mode is validated / read into the draft.
 */
class DestinationStepTest {

    private static final String EXISTING_ID = "kafka-dest-existing";
    private static final String NEW_ID = "kafka-dest-new";
    private static final String NEW_TYPE = "KAFKA";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private JsonMapper jsonMapper;
    private DestinationStep step;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        List<DestinationDTO> existing = List.of(new DestinationDTO(EXISTING_ID, "KAFKA", null, CREATED_AT));
        step = new DestinationStep(jsonMapper, existing);
    }

    @Test
    void defaultMode_isSelectExisting() {
        assertThat(step.validate()).isFalse(); // nothing selected yet, but mode is select-existing
    }

    @Test
    void selectExistingMode_noSelection_validateFails() {
        step.useExisting();

        assertThat(step.validate()).isFalse();
    }

    @Test
    void selectExistingMode_selectionMade_validateSucceeds() {
        step.useExisting();
        step.existingDestination().setValue(existingDto());

        assertThat(step.validate()).isTrue();
    }

    @Test
    void selectExistingMode_readInto_selectsExistingDestinationId() {
        step.useExisting();
        step.existingDestination().setValue(existingDto());
        WizardDraft draft = new WizardDraft();

        step.readInto(draft);

        assertThat(draft.destinationMode()).isEqualTo(WizardMode.SELECT_EXISTING);
        assertThat(draft.selectedDestinationId()).isEqualTo(EXISTING_ID);
    }

    @Test
    void createNewMode_blankFields_validateFails() {
        step.createNew();

        assertThat(step.validate()).isFalse();
    }

    @Test
    void createNewMode_fieldsFilled_validateSucceeds() {
        step.createNew();
        fillNewDestinationFields();

        assertThat(step.validate()).isTrue();
    }

    @Test
    void createNewMode_readInto_capturesNewDestinationRequest() {
        step.createNew();
        fillNewDestinationFields();
        WizardDraft draft = new WizardDraft();

        step.readInto(draft);

        assertThat(draft.destinationMode()).isEqualTo(WizardMode.CREATE_NEW);
        CreateDestinationRequest request = draft.newDestination();
        assertThat(request.destinationId()).isEqualTo(NEW_ID);
        assertThat(request.destinationType()).isEqualTo(NEW_TYPE);
    }

    @Test
    void switchingBackToSelectExisting_doesNotValidateCreateFields() {
        step.createNew();
        step.useExisting();
        step.existingDestination().setValue(existingDto());

        assertThat(step.validate()).isTrue();
    }

    /**
     * Regression: a blank config used to be marked "Required" and then
     * immediately cleared by the JSON parse (blank parses as valid), so Next
     * refused to advance while the field showed no error at all.
     */
    @Test
    void createNewMode_blankConfig_marksConfigFieldInvalid() {
        step.createNew();
        step.newDestinationId().setValue(NEW_ID);
        step.newDestinationType().setValue(NEW_TYPE);

        assertThat(step.validate()).isFalse();
        assertThat(step.newDestinationConfig().isInvalid()).isTrue();
        assertThat(step.newDestinationConfig().getErrorMessage()).isEqualTo(UiText.VALIDATION_REQUIRED);
    }

    @Test
    void createNewMode_malformedConfig_marksConfigFieldInvalid() {
        step.createNew();
        step.newDestinationId().setValue(NEW_ID);
        step.newDestinationType().setValue(NEW_TYPE);
        step.newDestinationConfig().setValue("{not json");

        assertThat(step.validate()).isFalse();
        assertThat(step.newDestinationConfig().isInvalid()).isTrue();
        assertThat(step.newDestinationConfig().getErrorMessage()).isEqualTo(UiText.VALIDATION_INVALID_JSON);
    }

    @Test
    void createNewMode_validConfig_clearsConfigFieldError() {
        step.createNew();
        fillNewDestinationFields();

        assertThat(step.validate()).isTrue();
        assertThat(step.newDestinationConfig().isInvalid()).isFalse();
    }

    private DestinationDTO existingDto() {
        return new DestinationDTO(EXISTING_ID, "KAFKA", null, CREATED_AT);
    }

    private void fillNewDestinationFields() {
        step.newDestinationId().setValue(NEW_ID);
        step.newDestinationType().setValue(NEW_TYPE);
        step.newDestinationConfig().setValue("{\"topic\":\"t\"}");
    }
}
