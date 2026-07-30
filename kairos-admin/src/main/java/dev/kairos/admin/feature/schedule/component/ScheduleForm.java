package dev.kairos.admin.feature.schedule.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.common.dto.schedule.CreateScheduleRequest;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import dev.kairos.admin.feature.schedule.dto.ScheduleType;
import dev.kairos.common.dto.schedule.UpdateScheduleRequest;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.shared.form.FieldValidation;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Fields;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.util.Strings;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Create/edit dialog for a schedule. In create mode a {@link ComboBox} selects
 * the owning task (schedules are task-scoped); on edit the task is fixed and the
 * picker is hidden. A {@link Select} of {@link ScheduleType} drives which "when"
 * field is visible: {@code ONCE} → {@link DateTimePicker}, {@code CRON} → the
 * cron row from {@link ScheduleWhenFields#cronRow} (field + visual builder),
 * {@code FIXED} → interval field. On edit the type is locked
 * (changing type means delete + recreate) and the form is prefilled. The API
 * remains the source of truth; client-side checks only improve UX.
 */
public class ScheduleForm extends Dialog {

    private final ScheduleResponse editing;                            // null → create mode
    private final BiConsumer<UUID, CreateScheduleRequest> onCreate; // set in create mode
    private final Consumer<UpdateScheduleRequest> onEdit;         // set in edit mode

    private final ComboBox<TaskDto> task;
    private final Select<ScheduleType> type = Fields.select(ScheduleText.COL_TYPE, ScheduleType.values());
    private final TextField label = Fields.text(ScheduleText.COL_LABEL);
    private final DateTimePicker runAt = new DateTimePicker(ScheduleText.FIELD_RUN_AT);
    private final TextField cronExpression = Fields.text(ScheduleText.FIELD_CRON);
    private final HorizontalLayout cronRow = ScheduleWhenFields.cronRow(cronExpression);
    private final IntegerField intervalSeconds = Fields.integer(ScheduleText.FIELD_INTERVAL);
    private final ComboBox<String> timezone =
            Fields.comboCustom(ScheduleText.COL_TIMEZONE, ScheduleText.HELPER_TIMEZONE, ScheduleText.TIMEZONE_OPTIONS);

    private ScheduleForm(List<TaskDto> tasks,
                         ScheduleResponse editing,
                         BiConsumer<UUID, CreateScheduleRequest> onCreate,
                         Consumer<UpdateScheduleRequest> onEdit) {
        this.editing = editing;
        this.onCreate = onCreate;
        this.onEdit = onEdit;
        this.task = Fields.combo(ScheduleText.PICK_TASK, tasks);
        this.task.setItemLabelGenerator(TaskDto::label);

        setHeaderTitle(editing == null ? ScheduleText.NEW_SCHEDULE : ScheduleText.EDIT_SCHEDULE);
        setWidth(Tokens.DIALOG_WIDTH_L);

        type.addValueChangeListener(e -> showFieldsForType(e.getValue()));

        if (editing == null) {
            type.setValue(ScheduleType.ONCE);
            timezone.setValue(ScheduleText.DEFAULT_TIMEZONE);
        } else {
            task.setVisible(false); // task is fixed once the schedule exists
            prefill(editing);
            type.setReadOnly(true); // type is immutable
        }

        add(buildForm());
        getFooter().add(buildCancel(), buildSave());
    }

    /**
     * Opens the form in create mode. The user picks the owning task from
     * {@code tasks}, the type selector is active, and timezone defaults to UTC.
     * The {@code onCreate} consumer is called with the selected task id and the
     * assembled request when the user saves.
     *
     * @param tasks    the tasks a schedule can be attached to
     * @param onCreate called with the task id and {@link CreateScheduleRequest} on save
     * @return a new form instance ready to be opened
     */
    public static ScheduleForm forCreate(List<TaskDto> tasks,
                                         BiConsumer<UUID, CreateScheduleRequest> onCreate) {
        return new ScheduleForm(tasks, null, onCreate, null);
    }

    /**
     * Opens the form in edit mode prefilled from {@code schedule}. The task
     * picker is hidden (task is fixed) and the type selector is locked — changing
     * type is not supported by the update endpoint and requires delete +
     * recreate. The {@code onEdit} consumer is called with the assembled request
     * when the user saves.
     *
     * @param schedule the schedule to edit; its fields are used to prefill the form
     * @param onEdit   called with the assembled {@link UpdateScheduleRequest} on save
     * @return a new form instance ready to be opened
     */
    public static ScheduleForm forEdit(ScheduleResponse schedule, Consumer<UpdateScheduleRequest> onEdit) {
        return new ScheduleForm(List.of(), schedule, null, onEdit);
    }

    private FormLayout buildForm() {
        FormLayout layout = new FormLayout(task, type, label, runAt, cronRow, intervalSeconds, timezone);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep(Tokens.FORM_BREAKPOINT_ZERO, Tokens.FORM_COLUMNS));
        layout.setColspan(task, Tokens.FORM_COLSPAN_FULL);
        layout.setColspan(runAt, Tokens.FORM_COLSPAN_FULL);
        layout.setColspan(cronRow, Tokens.FORM_COLSPAN_FULL);
        return layout;
    }

    private void showFieldsForType(ScheduleType selected) {
        runAt.setVisible(selected == ScheduleType.ONCE);
        cronRow.setVisible(selected == ScheduleType.CRON);
        intervalSeconds.setVisible(selected == ScheduleType.FIXED);
        // timezone governs the ONCE wall-clock → instant conversion and the CRON
        // expression; it is meaningless for FIXED (a plain interval).
        timezone.setVisible(selected != ScheduleType.FIXED);
    }

    private Button buildSave() {
        return Buttons.primary(UiText.BTN_SAVE, e -> save());
    }

    private Button buildCancel() {
        return Buttons.tertiary(UiText.BTN_CANCEL, e -> close());
    }

    void save() {
        if (!validate()) {
            return;
        }
        if (editing == null) {
            onCreate.accept(task.getValue().id(), new CreateScheduleRequest(
                    type.getValue().name(),
                    Strings.trimToNull(label.getValue()),
                    runAtInstant(),
                    Strings.trimToNull(cronExpression.getValue()),
                    intervalSeconds.getValue(),
                    Strings.trimToNull(timezone.getValue())));
        } else {
            onEdit.accept(new UpdateScheduleRequest(
                    Strings.trimToNull(label.getValue()),
                    runAtInstant(),
                    Strings.trimToNull(cronExpression.getValue()),
                    intervalSeconds.getValue(),
                    Strings.trimToNull(timezone.getValue())));
        }
        close();
    }

    boolean validate() {
        boolean whenValid = switch (type.getValue()) {
            case ONCE -> ScheduleWhenFields.validateRunAt(runAt, timezone);
            case CRON -> FieldValidation.require(cronExpression, UiText.VALIDATION_REQUIRED);
            case FIXED -> ScheduleWhenFields.validateInterval(intervalSeconds);
        };
        boolean timezoneValid = ScheduleWhenFields.validateTimezoneIfApplicable(timezone, type.getValue());
        return validateTask() & whenValid & timezoneValid;
    }

    private boolean validateTask() {
        if (editing != null) {
            return true; // task is fixed in edit mode
        }
        boolean selected = !task.isEmpty();
        task.setErrorMessage(UiText.VALIDATION_REQUIRED);
        task.setInvalid(!selected);
        return selected;
    }

    private Instant runAtInstant() {
        return ScheduleWhenFields.runAtInstant(runAt, timezone);
    }

    private ZoneId selectedZone() {
        return ScheduleWhenFields.selectedZone(timezone);
    }

    private void prefill(ScheduleResponse schedule) {
        type.setValue(ScheduleType.valueOf(schedule.type()));
        label.setValue(Strings.nullToEmpty(schedule.label()));
        cronExpression.setValue(Strings.nullToEmpty(schedule.cronExpression()));
        timezone.setValue(Strings.nullToEmpty(schedule.timezone()));
        if (schedule.intervalSeconds() != null) {
            intervalSeconds.setValue(schedule.intervalSeconds());
        }
        if (schedule.runAt() != null) {
            runAt.setValue(LocalDateTime.ofInstant(schedule.runAt(), selectedZone()));
        }
    }

    ComboBox<TaskDto> task() {
        return task;
    }

    Select<ScheduleType> type() {
        return type;
    }

    DateTimePicker runAt() {
        return runAt;
    }

    IntegerField intervalSeconds() {
        return intervalSeconds;
    }

    ComboBox<String> timezone() {
        return timezone;
    }
}
