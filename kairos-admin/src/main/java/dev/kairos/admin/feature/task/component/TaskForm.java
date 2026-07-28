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
import dev.kairos.admin.feature.task.TaskUniqueness;
import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.feature.task.dto.UpdateTaskRequest;
import dev.kairos.admin.shared.form.FieldValidation;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Fields;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.util.JsonText;
import dev.kairos.admin.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class TaskForm extends Dialog {

    private static final Logger logger = LoggerFactory.getLogger(TaskForm.class);

    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    private final JsonMapper jsonMapper;

    private final TaskDto editing;                       // null → create mode
    private final Consumer<CreateTaskRequest> onCreate;  // set in create mode
    private final Consumer<UpdateTaskRequest> onEdit;    // set in edit mode
    private final Set<String> takenServiceNameKeys;

    private final TextField service = Fields.text(TaskText.COL_SERVICE);
    private final TextField name = Fields.text(TaskText.COL_NAME);
    private final TextField description = Fields.text(TaskText.FIELD_DESCRIPTION);
    private final ComboBox<String> destinationId;
    private final TextField eventName = Fields.text(TaskText.COL_EVENT_NAME);
    private final IntegerField timeoutMs = Fields.integer(TaskText.COL_TIMEOUT);
    private final Checkbox active = Fields.checkbox(TaskText.COL_ACTIVE, true);
    private final Checkbox supportsRetry = Fields.checkbox(TaskText.FIELD_SUPPORTS_RETRY, false);
    private final TextArea payload = Fields.textArea(TaskText.FIELD_PAYLOAD);

    private TaskForm(JsonMapper jsonMapper,
                     List<String> destinationIds,
                     List<TaskDto> existingTasks,
                     TaskDto editing,
                     Consumer<CreateTaskRequest> onCreate,
                     Consumer<UpdateTaskRequest> onEdit) {
        this.jsonMapper = jsonMapper;
        this.editing = editing;
        this.onCreate = onCreate;
        this.onEdit = onEdit;
        this.destinationId = Fields.combo(TaskText.COL_DESTINATION, destinationIds);
        UUID excludeId = editing == null ? null : editing.id();
        this.takenServiceNameKeys = TaskUniqueness.keysExcluding(existingTasks, excludeId);

        setHeaderTitle(editing == null ? TaskText.NEW_TASK : TaskText.EDIT_TASK);
        setWidth(Tokens.DIALOG_WIDTH_L);

        if (editing == null) {
            timeoutMs.setValue(DEFAULT_TIMEOUT_MS);
        } else {
            prefill(editing);
            service.setReadOnly(true); // service is immutable
        }

        name.addBlurListener(e -> checkUnique());
        service.addBlurListener(e -> checkUnique());

        add(buildForm());
        getFooter().add(buildCancel(), buildSave());
    }

    public static TaskForm forCreate(JsonMapper jsonMapper,
                                     List<String> destinationIds,
                                     List<TaskDto> existingTasks,
                                     Consumer<CreateTaskRequest> onCreate) {
        return new TaskForm(jsonMapper, destinationIds, existingTasks, null, onCreate, null);
    }

    public static TaskForm forEdit(JsonMapper jsonMapper,
                                   List<String> destinationIds,
                                   List<TaskDto> existingTasks,
                                   TaskDto task,
                                   Consumer<UpdateTaskRequest> onEdit) {
        return new TaskForm(jsonMapper, destinationIds, existingTasks, task, null, onEdit);
    }

    private FormLayout buildForm() {
        FormLayout layout = new FormLayout(
                service, name, description, destinationId, eventName,
                timeoutMs, payload, active, supportsRetry
        );

        layout.setResponsiveSteps(new FormLayout.ResponsiveStep(Tokens.FORM_BREAKPOINT_ZERO, Tokens.FORM_COLUMNS));
        layout.setColspan(description, Tokens.FORM_COLSPAN_FULL);
        layout.setColspan(payload, Tokens.FORM_COLSPAN_FULL);
        return layout;
    }

    private Button buildSave() {
        return Buttons.primary(UiText.BTN_SAVE, e -> save());
    }

    private Button buildCancel() {
        return Buttons.tertiary(UiText.BTN_CANCEL, e -> close());
    }

    private void save() {
        if (!validate()) {
            return;
        }

        FieldValidation.JsonResult result = FieldValidation.parseJson(
                payload, jsonMapper, UiText.VALIDATION_INVALID_JSON);
        if (!result.valid()) {
            return;
        }

        if (editing == null) {
            onCreate.accept(new CreateTaskRequest(
                    Strings.trimToNull(service.getValue()),
                    Strings.trimToNull(name.getValue()),
                    Strings.trimToNull(description.getValue()),
                    active.getValue(),
                    Strings.trimToNull(destinationId.getValue()),
                    Strings.trimToNull(eventName.getValue()),
                    result.value(),
                    timeoutMs.getValue(),
                    supportsRetry.getValue()
            ));
        } else {
            onEdit.accept(new UpdateTaskRequest(
                    Strings.trimToNull(name.getValue()),
                    Strings.trimToNull(description.getValue()),
                    active.getValue(),
                    Strings.trimToNull(destinationId.getValue()),
                    Strings.trimToNull(eventName.getValue()),
                    result.value(),
                    timeoutMs.getValue(),
                    supportsRetry.getValue()
            ));
        }
        close();
    }

    boolean validate() {
        boolean ok = FieldValidation.require(service, UiText.VALIDATION_REQUIRED);
        ok &= FieldValidation.require(name, UiText.VALIDATION_REQUIRED);
        ok &= FieldValidation.require(destinationId, UiText.VALIDATION_REQUIRED);
        ok &= FieldValidation.require(eventName, UiText.VALIDATION_REQUIRED);
        ok &= FieldValidation.requirePresent(timeoutMs, timeoutMs, UiText.VALIDATION_REQUIRED);
        ok &= checkUnique();
        return ok;
    }

    private boolean checkUnique() {
        boolean unique = FieldValidation.uniqueServiceName(
                service, name, takenServiceNameKeys, TaskText.VALIDATION_DUPLICATE_SERVICE_NAME);
        if (!unique) {
            logger.debug("Duplicate (service, name) flagged in the task form: service='{}', name='{}'",
                    service.getValue(), name.getValue());
        }
        return unique;
    }

    private void prefill(TaskDto task) {
        service.setValue(Strings.nullToEmpty(task.service()));
        name.setValue(Strings.nullToEmpty(task.name()));
        description.setValue(Strings.nullToEmpty(task.description()));
        destinationId.setValue(task.destinationId());
        eventName.setValue(Strings.nullToEmpty(task.eventName()));
        timeoutMs.setValue(task.timeoutMs());
        active.setValue(task.active());
        supportsRetry.setValue(task.supportsRetry());
        payload.setValue(JsonText.forDisplay(jsonMapper, task.payload()));
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

    ComboBox<String> destinationId() {
        return destinationId;
    }

    IntegerField timeoutMs() {
        return timeoutMs;
    }

}
