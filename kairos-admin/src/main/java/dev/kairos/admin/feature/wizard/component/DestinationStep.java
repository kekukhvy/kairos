package dev.kairos.admin.feature.wizard.component;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import dev.kairos.admin.feature.destination.ConfigTemplatePrefill;
import dev.kairos.admin.feature.destination.DestinationText;
import dev.kairos.admin.feature.destination.dto.CreateDestinationRequest;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.feature.wizard.WizardDraft;
import dev.kairos.admin.feature.wizard.WizardMode;
import dev.kairos.admin.feature.wizard.WizardText;
import dev.kairos.admin.shared.form.FieldValidation;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Fields;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.util.Strings;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Step 2 body: lets the operator pick an existing destination or fill in the
 * inline fields to create a new one. A {@link RadioButtonGroup} toggles
 * between the two; only the active mode is validated and read into the
 * draft, mirroring {@code DestinationForm}'s field set for the create side.
 */
public class DestinationStep extends VerticalLayout {

    private final JsonMapper jsonMapper;

    private final RadioButtonGroup<WizardMode> mode = buildModeToggle();
    private final ComboBox<DestinationDTO> existingDestination;
    private final TextField newDestinationId = Fields.text(DestinationText.FIELD_ID);
    private final Select<String> newDestinationType = Fields.select(
            DestinationText.FIELD_TYPE,
            DestinationText.TYPE_KAFKA, DestinationText.TYPE_SQS,
            DestinationText.TYPE_WEBHOOK, DestinationText.TYPE_RABBITMQ);
    private final TextArea newDestinationConfig = Fields.textArea(DestinationText.FIELD_CONFIG);
    private final FormLayout createForm;
    private final ConfigTemplatePrefill configTemplatePrefill = new ConfigTemplatePrefill(newDestinationConfig);

    public DestinationStep(JsonMapper jsonMapper, List<DestinationDTO> existing) {
        this.jsonMapper = jsonMapper;
        this.existingDestination = Fields.combo(WizardText.FIELD_EXISTING_DESTINATION, existing);
        this.existingDestination.setItemLabelGenerator(DestinationDTO::destinationId);
        this.createForm = buildCreateForm();

        setPadding(false);
        StyleConfig.create().gap(Tokens.SPACE_S).applyTo(this);
        mode.addValueChangeListener(e -> showFieldsForMode(e.getValue()));
        newDestinationType.addValueChangeListener(e -> configTemplatePrefill.onTypeSelected(e.getValue()));
        add(buildHelp(), mode, existingDestination, createForm);
        showFieldsForMode(WizardMode.SELECT_EXISTING);
    }

    private static RadioButtonGroup<WizardMode> buildModeToggle() {
        RadioButtonGroup<WizardMode> group = new RadioButtonGroup<>();
        group.setItems(WizardMode.SELECT_EXISTING, WizardMode.CREATE_NEW);
        group.setItemLabelGenerator(m -> m == WizardMode.SELECT_EXISTING
                ? WizardText.MODE_USE_EXISTING : WizardText.MODE_CREATE_NEW);
        group.setValue(WizardMode.SELECT_EXISTING);
        return group;
    }

    private FormLayout buildCreateForm() {
        FormLayout layout = new FormLayout(newDestinationId, newDestinationType, newDestinationConfig);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep(Tokens.FORM_BREAKPOINT_ZERO, Tokens.FORM_COLUMNS));
        layout.setColspan(newDestinationConfig, Tokens.FORM_COLSPAN_FULL);
        return layout;
    }

    private Span buildHelp() {
        return StyleConfig.create()
                .color(Tokens.TEXT_SECONDARY)
                .fontSize(Tokens.FONT_S)
                .applyTo(new Span(WizardText.HELP_DESTINATION));
    }

    private void showFieldsForMode(WizardMode selected) {
        boolean creating = selected == WizardMode.CREATE_NEW;
        existingDestination.setVisible(!creating);
        createForm.setVisible(creating);
    }

    /** Switches to "use existing" mode; only the picker is validated afterwards. */
    public void useExisting() {
        mode.setValue(WizardMode.SELECT_EXISTING);
    }

    /** Switches to "create new" mode; only the inline fields are validated afterwards. */
    public void createNew() {
        mode.setValue(WizardMode.CREATE_NEW);
    }

    /** Validates the active mode only: the picker in select mode, the fields in create mode. */
    public boolean validate() {
        return mode.getValue() == WizardMode.SELECT_EXISTING ? validateExisting() : validateNew();
    }

    private boolean validateExisting() {
        boolean selected = !existingDestination.isEmpty();
        existingDestination.setErrorMessage(UiText.VALIDATION_REQUIRED);
        existingDestination.setInvalid(!selected);
        return selected;
    }

    private boolean validateNew() {
        boolean ok = FieldValidation.require(newDestinationId, UiText.VALIDATION_REQUIRED);
        ok &= FieldValidation.requirePresent(newDestinationType, newDestinationType, UiText.VALIDATION_REQUIRED);
        FieldValidation.JsonResult result = FieldValidation.requireJson(
                newDestinationConfig, jsonMapper, UiText.VALIDATION_REQUIRED, UiText.VALIDATION_INVALID_JSON);
        return ok & result.valid();
    }

    /** Records the active mode's outcome onto {@code draft}. */
    public void readInto(WizardDraft draft) {
        if (mode.getValue() == WizardMode.SELECT_EXISTING) {
            draft.selectExistingDestination(existingDestination.getValue().destinationId());
            return;
        }
        Object config = FieldValidation.parseJson(newDestinationConfig, jsonMapper, UiText.VALIDATION_INVALID_JSON).value();
        draft.createNewDestination(new CreateDestinationRequest(
                Strings.trimToNull(newDestinationId.getValue()), newDestinationType.getValue(), config));
    }

    ComboBox<DestinationDTO> existingDestination() {
        return existingDestination;
    }

    TextField newDestinationId() {
        return newDestinationId;
    }

    Select<String> newDestinationType() {
        return newDestinationType;
    }

    TextArea newDestinationConfig() {
        return newDestinationConfig;
    }
}
