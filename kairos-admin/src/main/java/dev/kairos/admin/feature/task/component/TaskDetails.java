package dev.kairos.admin.feature.task.component;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.util.DateTimes;
import dev.kairos.admin.shared.util.JsonText;
import tools.jackson.databind.json.JsonMapper;

import static dev.kairos.admin.shared.style.Tokens.FORM_COLSPAN_FULL;

public class TaskDetails extends Dialog {

    public TaskDetails(JsonMapper jsonMapper, TaskDto task) {
        setHeaderTitle(TaskText.DETAILS_TITLE);
        setWidth(Tokens.DIALOG_WIDTH_L);

        add(buildContent(jsonMapper, task));
        getFooter().add(Buttons.tertiary(TaskText.BTN_CLOSE, e -> close()));
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

        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", Tokens.FORM_COLUMNS));

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