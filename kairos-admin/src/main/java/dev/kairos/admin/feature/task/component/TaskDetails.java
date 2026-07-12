package dev.kairos.admin.feature.task.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Dialogs;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.util.DateTimes;
import dev.kairos.admin.shared.util.JsonText;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.Consumer;

import static dev.kairos.admin.shared.style.Tokens.FORM_COLSPAN_FULL;

/**
 * Read-only modal dialog that shows all fields of a single task, with Edit
 * and Delete actions in its footer alongside Close. The payload JSON is
 * rendered in a full-width text area for readability. Timestamps are
 * formatted for display via {@link dev.kairos.admin.shared.util.DateTimes}.
 */
public class TaskDetails extends Dialog {

    private TaskDetails(JsonMapper jsonMapper, TaskDto task, Consumer<TaskDto> onEdit, Consumer<TaskDto> onDelete) {
        setHeaderTitle(TaskText.DETAILS_TITLE);
        setWidth(Tokens.DIALOG_WIDTH_L);

        add(buildContent(jsonMapper, task));
        getFooter().add(
                buildDeleteButton(task, onDelete),
                Buttons.tertiary(UiText.BTN_CLOSE, e -> close()),
                Buttons.secondary(TaskText.ACTION_EDIT, e -> edit(task, onEdit)));
    }

    private Button buildDeleteButton(TaskDto task, Consumer<TaskDto> onDelete) {
        Button button = Buttons.danger(UiText.ACTION_DELETE, e -> confirmDelete(task, onDelete));
        return StyleConfig.create().marginInlineEnd(Tokens.AUTO).applyTo(button);
    }

    /**
     * Creates a details dialog for the given task.
     *
     * @param jsonMapper used to pretty-print the payload JSON
     * @param task       the task to display
     * @param onEdit     called with the task when the operator clicks Edit
     * @param onDelete   called with the task when the operator confirms deletion
     * @return a new dialog instance, ready to be {@link #open() opened}
     */
    public static TaskDetails of(JsonMapper jsonMapper, TaskDto task,
                                 Consumer<TaskDto> onEdit, Consumer<TaskDto> onDelete) {
        return new TaskDetails(jsonMapper, task, onEdit, onDelete);
    }

    private void edit(TaskDto task, Consumer<TaskDto> onEdit) {
        onEdit.accept(task);
        close();
    }

    private void confirmDelete(TaskDto task, Consumer<TaskDto> onDelete) {
        Dialogs.confirmDelete(TaskText.CONFIRM_DELETE_TITLE, TaskText.CONFIRM_DELETE_TEXT,
                UiText.ACTION_DELETE, () -> delete(task, onDelete));
    }

    private void delete(TaskDto task, Consumer<TaskDto> onDelete) {
        onDelete.accept(task);
        close();
    }

    private FormLayout buildContent(JsonMapper jsonMapper, TaskDto task) {
        FormLayout layout = new FormLayout();

        layout.addFormItem(text(String.valueOf(task.id())), TaskText.DETAIL_ID);
        layout.addFormItem(text(task.service()), TaskText.COL_SERVICE);
        layout.addFormItem(text(task.name()), TaskText.COL_NAME);
        layout.addFormItem(text(task.description()), TaskText.FIELD_DESCRIPTION);
        layout.addFormItem(text(task.destinationId()), TaskText.COL_DESTINATION);
        layout.addFormItem(text(task.eventName()), TaskText.COL_EVENT_NAME);
        layout.addFormItem(text(bool(task.active())), TaskText.COL_ACTIVE);
        layout.addFormItem(text(bool(task.supportsRetry())), TaskText.FIELD_SUPPORTS_RETRY);
        layout.addFormItem(text(String.valueOf(task.timeoutMs())), TaskText.COL_TIMEOUT);
        layout.addFormItem(text(DateTimes.forDisplay(task.createdAt())), TaskText.DETAIL_CREATED);
        layout.addFormItem(text(DateTimes.forDisplay(task.updatedAt())), TaskText.DETAIL_UPDATED);

        TextArea payload = new TextArea();
        payload.setReadOnly(true);
        payload.setWidthFull();
        payload.setValue(JsonText.forDisplay(jsonMapper, task.payload()));

        layout.setResponsiveSteps(new FormLayout.ResponsiveStep(Tokens.FORM_BREAKPOINT_ZERO, Tokens.FORM_COLUMNS));

        FormLayout.FormItem payloadItem = layout.addFormItem(payload, TaskText.FIELD_PAYLOAD);
        layout.setColspan(payloadItem, FORM_COLSPAN_FULL);

        return layout;
    }

    private static Span text(String value) {
        return new Span(value == null ? "" : value);
    }

    private static String bool(boolean value) {
        return value ? TaskText.BOOL_YES : TaskText.BOOL_NO;
    }
}