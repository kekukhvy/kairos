package dev.kairos.admin.feature.task.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.feature.task.dto.UpdateTaskRequest;
import dev.kairos.admin.shared.form.FieldValidation;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Fields;
import dev.kairos.admin.shared.util.JsonText;
import dev.kairos.admin.shared.util.Strings;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.function.Consumer;

public class TaskForm extends Dialog {

    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    private final JsonMapper jsonMapper;

    private final TaskDto editing;                       // null → create mode
    private final Consumer<CreateTaskRequest> onCreate;  // set in create mode
    private final Consumer<UpdateTaskRequest> onEdit;    // set in edit mode


    private final TextField service = Fields.text(TaskText.COL_SERVICE);
    private final TextField name = Fields.text(TaskText.COL_NAME);
    private final TextField description = Fields.text(TaskText.FIELD_DESCRIPTION);
    private final ComboBox<String> destinationId;
    private final TextField messageType = Fields.text(TaskText.COL_MESSAGE_TYPE);
    private final IntegerField timeoutMs = Fields.integer(TaskText.COL_TIMEOUT);
    private final Checkbox active = Fields.checkbox(TaskText.COL_ACTIVE, true);
    private final Checkbox supportsRetry = Fields.checkbox(TaskText.FIELD_SUPPORTS_RETRY, false);
    private final TextArea payload = Fields.textArea(TaskText.FIELD_PAYLOAD);

    private TaskForm(JsonMapper jsonMapper,
                     List<String> destinationIds,
                     TaskDto editing,
                     Consumer<CreateTaskRequest> onCreate,
                     Consumer<UpdateTaskRequest> onEdit) {
        this.jsonMapper = jsonMapper;
        this.editing = editing;
        this.onCreate = onCreate;
        this.onEdit = onEdit;
        this.destinationId = Fields.combo(TaskText.COL_DESTINATION, destinationIds);

        setHeaderTitle(editing == null ? TaskText.NEW_TASK : TaskText.EDIT_TASK);
        setWidth(Tokens.DIALOG_WIDTH_L);

        if (editing == null) {
            timeoutMs.setValue(DEFAULT_TIMEOUT_MS);
        } else {
            prefill(editing);
            service.setReadOnly(true); // service is immutable
        }

        add(buildForm());
        getFooter().add(buildCancel(), buildSave());
    }

    public static TaskForm forCreate(JsonMapper jsonMapper,
                                     List<String> destinationIds,
                                     Consumer<CreateTaskRequest> onCreate) {
        return new TaskForm(jsonMapper, destinationIds, null, onCreate, null);
    }

    public static TaskForm forEdit(JsonMapper jsonMapper,
                                   List<String> destinationIds,
                                   TaskDto task,
                                   Consumer<UpdateTaskRequest> onEdit) {
        return new TaskForm(jsonMapper, destinationIds, task, null, onEdit);
    }

    private FormLayout buildForm() {
        FormLayout layout = new FormLayout(
                service, name, description, destinationId, messageType,
                timeoutMs, payload, active, supportsRetry
        );

        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", Tokens.FORM_COLUMNS));
        layout.setColspan(description, Tokens.FORM_COLSPAN_FULL);
        layout.setColspan(payload, Tokens.FORM_COLSPAN_FULL);
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

        if (editing == null) {
            onCreate.accept(new CreateTaskRequest(
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
        } else {
            onEdit.accept(new UpdateTaskRequest(
                    Strings.trimToNull(name.getValue()),
                    Strings.trimToNull(description.getValue()),
                    active.getValue(),
                    Strings.trimToNull(destinationId.getValue()),
                    Strings.trimToNull(messageType.getValue()),
                    parsedPayload,
                    timeoutMs.getValue(),
                    supportsRetry.getValue()
            ));
        }
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

    private void prefill(TaskDto task) {
        service.setValue(safe(task.service()));
        name.setValue(safe(task.name()));
        description.setValue(safe(task.description()));
        destinationId.setValue(task.destinationId());
        messageType.setValue(safe(task.messageType()));
        timeoutMs.setValue(task.timeoutMs());
        active.setValue(task.active());
        supportsRetry.setValue(task.supportsRetry());
        payload.setValue(JsonText.forDisplay(jsonMapper, task.payload()));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
