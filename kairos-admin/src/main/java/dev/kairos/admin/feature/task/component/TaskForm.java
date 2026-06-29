package dev.kairos.admin.feature.task.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.Consumer;

public class TaskForm extends Dialog {

    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    private final JsonMapper jsonMapper;
    private final Consumer<CreateTaskRequest> onSave;

    private final TextField service = new TextField(TaskText.COL_SERVICE);
    private final TextField name = new TextField(TaskText.COL_NAME);
    private final TextField description = new TextField(TaskText.FIELD_DESCRIPTION);
    private final TextField destinationId = new TextField(TaskText.COL_DESTINATION);
    private final TextField messageType = new TextField(TaskText.COL_MESSAGE_TYPE);
    private final IntegerField timeoutMs = new IntegerField(TaskText.COL_TIMEOUT);
    private final Checkbox active = new Checkbox(TaskText.COL_ACTIVE, true);
    private final Checkbox supportsRetry = new Checkbox(TaskText.FIELD_SUPPORTS_RETRY, false);
    private final TextArea payload = new TextArea(TaskText.FIELD_PAYLOAD);

    public TaskForm(JsonMapper jsonMapper, Consumer<CreateTaskRequest> onSave) {
        this.jsonMapper = jsonMapper;
        this.onSave = onSave;

        setHeaderTitle(TaskText.NEW_TASK);
        setWidth("480px");
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
        Button save = new Button(TaskText.BTN_SAVE, e -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return save;
    }

    private Button buildCancel() {
        return new Button(TaskText.BTN_CANCEL, e -> close());
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
                nullIfBlank(service.getValue()),
                nullIfBlank(name.getValue()),
                nullIfBlank(description.getValue()),
                active.getValue(),
                nullIfBlank(destinationId.getValue()),
                nullIfBlank(messageType.getValue()),
                parsedPayload,
                timeoutMs.getValue(),
                supportsRetry.getValue()
        ));
        close();
    }

    private boolean validate() {
        boolean ok = requireText(service);
        ok &= requireText(name);
        ok &= requireText(destinationId);
        ok &= requireText(messageType);
        if (timeoutMs.isEmpty()) {
            timeoutMs.setInvalid(true);
            timeoutMs.setErrorMessage(TaskText.VALIDATION_REQUIRED);
            ok = false;
        } else {
            timeoutMs.setInvalid(false);
        }
        return ok;
    }

    private Object readPayload() {
        String raw = payload.getValue();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return jsonMapper.readValue(raw, Object.class);
    }

    private boolean requireText(TextField field) {
        if (field.getValue() == null || field.getValue().isBlank()) {
            field.setInvalid(true);
            field.setErrorMessage(TaskText.VALIDATION_REQUIRED);
            return false;
        }
        field.setInvalid(false);
        return true;
    }

    private static String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}