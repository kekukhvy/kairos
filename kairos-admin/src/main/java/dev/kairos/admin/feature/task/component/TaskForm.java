package dev.kairos.admin.feature.task.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.shared.form.FieldValidation;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Fields;
import dev.kairos.admin.shared.util.Strings;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.Consumer;

public class TaskForm extends Dialog {

    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    private final JsonMapper jsonMapper;
    private final Consumer<CreateTaskRequest> onSave;

    private final TextField service = Fields.text(TaskText.COL_SERVICE);
    private final TextField name = Fields.text(TaskText.COL_NAME);
    private final TextField description = Fields.text(TaskText.FIELD_DESCRIPTION);
    private final TextField destinationId = Fields.text(TaskText.COL_DESTINATION);
    private final TextField messageType = Fields.text(TaskText.COL_MESSAGE_TYPE);
    private final IntegerField timeoutMs = Fields.integer(TaskText.COL_TIMEOUT);
    private final Checkbox active = Fields.checkbox(TaskText.COL_ACTIVE, true);
    private final Checkbox supportsRetry = Fields.checkbox(TaskText.FIELD_SUPPORTS_RETRY, false);
    private final TextArea payload = Fields.textArea(TaskText.FIELD_PAYLOAD);

    public TaskForm(JsonMapper jsonMapper, Consumer<CreateTaskRequest> onSave) {
        this.jsonMapper = jsonMapper;
        this.onSave = onSave;

        setHeaderTitle(TaskText.NEW_TASK);
        setWidth(Tokens.DIALOG_WIDTH_M);
        timeoutMs.setValue(DEFAULT_TIMEOUT_MS);

        add(buildForm());
        getFooter().add(buildCancel(), buildSave());
    }

    private FormLayout buildForm() {
        FormLayout layout = new FormLayout(
                service, name, description, destinationId,
                messageType, timeoutMs, active, supportsRetry, payload
        );
        layout.setColspan(description, 2);
        layout.setColspan(payload, 2);
        return layout;
    }

    private Button buildSave() {
        return Buttons.primary(TaskText.BTN_SAVE, e -> save());
    }

    private Button buildCancel() {
        return Buttons.tertiary(TaskText.BTN_CANCEL, e -> close());
    }

    private void save() {
        if (!validate()) {
            return;
        }

        Object parsedPayload;
        try {
            parsedPayload = readPayload();
            payload.setInvalid(false);
        } catch (JacksonException ex) {
            payload.setInvalid(true);
            payload.setErrorMessage(TaskText.VALIDATION_INVALID_JSON);
            return;
        }

        onSave.accept(new CreateTaskRequest(
                Strings.trimToNull(service.getValue()),
                Strings.trimToNull(name.getValue()),
                Strings.trimToNull(description.getValue()),
                active.getValue(),
                Strings.trimToNull(destinationId.getValue()),
                Strings.trimToNull(messageType.getValue()),
                parsedPayload,
                timeoutMs.getValue(),
                supportsRetry.getValue()
        ));
        close();
    }

    private boolean validate() {
        boolean ok = FieldValidation.require(service, TaskText.VALIDATION_REQUIRED);
        ok &= FieldValidation.require(name, TaskText.VALIDATION_REQUIRED);
        ok &= FieldValidation.require(destinationId, TaskText.VALIDATION_REQUIRED);
        ok &= FieldValidation.require(messageType, TaskText.VALIDATION_REQUIRED);
        ok &= FieldValidation.requirePresent(timeoutMs, timeoutMs, TaskText.VALIDATION_REQUIRED);
        return ok;
    }

    private Object readPayload() {
        String raw = payload.getValue();
        if (Strings.isBlank(raw)) {
            return null;
        }
        return jsonMapper.readValue(raw, Object.class);
    }
}
