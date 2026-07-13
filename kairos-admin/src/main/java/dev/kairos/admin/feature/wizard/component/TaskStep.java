package dev.kairos.admin.feature.wizard.component;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.wizard.WizardDraft;
import dev.kairos.admin.feature.wizard.WizardText;
import dev.kairos.admin.shared.form.FieldValidation;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Fields;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.util.Strings;
import tools.jackson.databind.json.JsonMapper;

/**
 * Step 1 body: collects every {@link CreateTaskRequest} field except
 * {@code destinationId}, which the wizard resolves later from the
 * Destination step's outcome. Mirrors {@code TaskForm}'s field set and
 * validation rules; this step's task is always a "create" (the wizard never
 * edits an existing task).
 */
public class TaskStep extends VerticalLayout {

    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    private final JsonMapper jsonMapper;

    private final TextField service = Fields.text(TaskText.COL_SERVICE);
    private final TextField name = Fields.text(TaskText.COL_NAME);
    private final TextField description = Fields.text(TaskText.FIELD_DESCRIPTION);
    private final TextField eventName = Fields.text(TaskText.COL_EVENT_NAME);
    private final IntegerField timeoutMs = Fields.integer(TaskText.COL_TIMEOUT);
    private final Checkbox active = Fields.checkbox(TaskText.COL_ACTIVE, true);
    private final Checkbox supportsRetry = Fields.checkbox(TaskText.FIELD_SUPPORTS_RETRY, false);
    private final TextArea payload = Fields.textArea(TaskText.FIELD_PAYLOAD);

    public TaskStep(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        timeoutMs.setValue(DEFAULT_TIMEOUT_MS);

        setPadding(false);
        StyleConfig.create().gap(Tokens.SPACE_S).applyTo(this);
        add(buildHelp(), buildForm());
    }

    private FormLayout buildForm() {
        FormLayout layout = new FormLayout(
                service, name, description, eventName, timeoutMs, payload, active, supportsRetry);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep(Tokens.FORM_BREAKPOINT_ZERO, Tokens.FORM_COLUMNS));
        layout.setColspan(description, Tokens.FORM_COLSPAN_FULL);
        layout.setColspan(payload, Tokens.FORM_COLSPAN_FULL);
        return layout;
    }

    private Span buildHelp() {
        return StyleConfig.create()
                .color(Tokens.TEXT_SECONDARY)
                .fontSize(Tokens.FONT_S)
                .applyTo(new Span(WizardText.HELP_TASK));
    }

    /** Validates the required fields and the payload JSON, mirroring {@code TaskForm.validate()}. */
    public boolean validate() {
        boolean ok = FieldValidation.require(service, UiText.VALIDATION_REQUIRED);
        ok &= FieldValidation.require(name, UiText.VALIDATION_REQUIRED);
        ok &= FieldValidation.require(eventName, UiText.VALIDATION_REQUIRED);
        ok &= FieldValidation.requirePresent(timeoutMs, timeoutMs, UiText.VALIDATION_REQUIRED);
        FieldValidation.JsonResult result = FieldValidation.parseJson(payload, jsonMapper, UiText.VALIDATION_INVALID_JSON);
        return ok & result.valid();
    }

    /**
     * Installs a request factory on {@code draft} that builds a
     * {@link CreateTaskRequest} once the destination id is resolved.
     */
    public void readInto(WizardDraft draft) {
        String serviceValue = Strings.trimToNull(service.getValue());
        String nameValue = Strings.trimToNull(name.getValue());
        String descriptionValue = Strings.trimToNull(description.getValue());
        String eventNameValue = Strings.trimToNull(eventName.getValue());
        Boolean activeValue = active.getValue();
        Boolean supportsRetryValue = supportsRetry.getValue();
        int timeoutValue = timeoutMs.getValue();
        Object payloadValue = FieldValidation.parseJson(payload, jsonMapper, UiText.VALIDATION_INVALID_JSON).value();

        draft.setTaskRequestFactory(destinationId -> new CreateTaskRequest(
                serviceValue, nameValue, descriptionValue, activeValue, destinationId,
                eventNameValue, payloadValue, timeoutValue, supportsRetryValue));
    }

    TextField service() {
        return service;
    }

    TextField name() {
        return name;
    }

    TextField eventName() {
        return eventName;
    }
}
