package dev.kairos.admin.feature.schedule.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Dialogs;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.util.DateTimes;
import dev.kairos.common.dto.schedule.ScheduleResponse;

import java.util.function.Consumer;

/**
 * Read-only modal dialog that shows all fields of a single schedule, with
 * Edit and Delete actions in its footer alongside Close. The "when" value is
 * resolved by {@link ScheduleWhen}; timestamps are formatted for display via
 * {@link dev.kairos.admin.shared.util.DateTimes}.
 */
public class ScheduleDetails extends Dialog {

    private ScheduleDetails(ScheduleResponse schedule, Consumer<ScheduleResponse> onEdit,
                            Consumer<ScheduleResponse> onDelete) {
        setHeaderTitle(ScheduleText.DETAILS_TITLE);
        setWidth(Tokens.DIALOG_WIDTH_L);

        add(buildContent(schedule));
        getFooter().add(
                buildDeleteButton(schedule, onDelete),
                Buttons.tertiary(UiText.BTN_CLOSE, e -> close()),
                Buttons.secondary(ScheduleText.ACTION_EDIT, e -> edit(schedule, onEdit)));
    }

    /**
     * Creates a details dialog for the given schedule.
     *
     * @param schedule the schedule to display
     * @param onEdit   called with the schedule when the operator clicks Edit
     * @param onDelete called with the schedule when the operator confirms deletion
     * @return a new dialog instance, ready to be {@link #open() opened}
     */
    public static ScheduleDetails of(ScheduleResponse schedule, Consumer<ScheduleResponse> onEdit,
                                     Consumer<ScheduleResponse> onDelete) {
        return new ScheduleDetails(schedule, onEdit, onDelete);
    }

    private Button buildDeleteButton(ScheduleResponse schedule, Consumer<ScheduleResponse> onDelete) {
        Button button = Buttons.danger(UiText.ACTION_DELETE, e -> confirmDelete(schedule, onDelete));
        return StyleConfig.create().marginInlineEnd(Tokens.AUTO).applyTo(button);
    }

    private void edit(ScheduleResponse schedule, Consumer<ScheduleResponse> onEdit) {
        onEdit.accept(schedule);
        close();
    }

    private void confirmDelete(ScheduleResponse schedule, Consumer<ScheduleResponse> onDelete) {
        Dialogs.confirmDelete(ScheduleText.CONFIRM_DELETE_TITLE, ScheduleText.CONFIRM_DELETE_TEXT,
                UiText.ACTION_DELETE, () -> delete(schedule, onDelete));
    }

    private void delete(ScheduleResponse schedule, Consumer<ScheduleResponse> onDelete) {
        onDelete.accept(schedule);
        close();
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

        layout.setResponsiveSteps(new FormLayout.ResponsiveStep(Tokens.FORM_BREAKPOINT_ZERO, Tokens.FORM_COLUMNS));
        return layout;
    }

    private static Span text(String value) {
        return new Span(value == null ? "" : value);
    }

    private static String status(ScheduleResponse schedule) {
        return schedule.active() ? ScheduleText.STATUS_ACTIVE : ScheduleText.STATUS_PAUSED;
    }
}
