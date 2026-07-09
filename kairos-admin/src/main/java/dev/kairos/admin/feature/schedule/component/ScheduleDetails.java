package dev.kairos.admin.feature.schedule.component;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.util.DateTimes;

/**
 * Read-only modal dialog that shows all fields of a single schedule. The "when"
 * value is resolved by {@link ScheduleWhen}; timestamps are formatted for
 * display via {@link dev.kairos.admin.shared.util.DateTimes}.
 */
public class ScheduleDetails extends Dialog {

    public ScheduleDetails(ScheduleResponse schedule) {
        setHeaderTitle(ScheduleText.DETAILS_TITLE);
        setWidth(Tokens.DIALOG_WIDTH_M);

        add(buildContent(schedule));
        getFooter().add(Buttons.tertiary(UiText.BTN_CLOSE, e -> close()));
    }

    private FormLayout buildContent(ScheduleResponse schedule) {
        FormLayout layout = new FormLayout();

        layout.addFormItem(text(String.valueOf(schedule.id())), ScheduleText.DETAIL_ID);
        layout.addFormItem(text(String.valueOf(schedule.taskId())), ScheduleText.DETAIL_TASK_ID);
        layout.addFormItem(text(schedule.type()), ScheduleText.COL_TYPE);
        layout.addFormItem(text(schedule.label()), ScheduleText.COL_LABEL);
        layout.addFormItem(text(ScheduleWhen.describe(schedule)), ScheduleText.COL_WHEN);
        layout.addFormItem(text(schedule.timezone()), ScheduleText.COL_TIMEZONE);
        layout.addFormItem(text(status(schedule)), ScheduleText.COL_ACTIVE);
        layout.addFormItem(text(DateTimes.forDisplay(schedule.createdAt())), ScheduleText.DETAIL_CREATED);
        layout.addFormItem(text(DateTimes.forDisplay(schedule.updatedAt())), ScheduleText.DETAIL_UPDATED);

        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", Tokens.FORM_COLUMNS));
        return layout;
    }

    private static Span text(String value) {
        return new Span(value == null ? "" : value);
    }

    private static String status(ScheduleResponse schedule) {
        return schedule.active() ? ScheduleText.STATUS_ACTIVE : ScheduleText.STATUS_PAUSED;
    }
}
