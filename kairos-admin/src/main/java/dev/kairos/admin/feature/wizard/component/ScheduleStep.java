package dev.kairos.admin.feature.wizard.component;

import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.admin.feature.schedule.component.ScheduleWhenFields;
import dev.kairos.admin.feature.schedule.dto.ScheduleType;
import dev.kairos.admin.feature.wizard.WizardDraft;
import dev.kairos.admin.feature.wizard.WizardText;
import dev.kairos.admin.shared.form.FieldValidation;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Fields;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.util.Strings;
import dev.kairos.common.dto.schedule.CreateScheduleRequest;

import java.time.Instant;

/**
 * Step 3 body: always creates a new schedule for the task being set up — the
 * wizard creates a new task, so its schedule must be newly created for that
 * task too; there is no "select an existing schedule" mode. Mirrors
 * {@code ScheduleForm}'s type-driven when-field validation.
 */
public class ScheduleStep extends VerticalLayout {

    private final Select<ScheduleType> type = Fields.select(ScheduleText.COL_TYPE, ScheduleType.values());
    private final TextField label = Fields.text(ScheduleText.COL_LABEL);
    private final DateTimePicker runAt = new DateTimePicker(ScheduleText.FIELD_RUN_AT);
    private final TextField cronExpression = Fields.text(ScheduleText.FIELD_CRON);
    private final IntegerField intervalSeconds = Fields.integer(ScheduleText.FIELD_INTERVAL);
    private final TextField timezone = Fields.text(ScheduleText.COL_TIMEZONE);
    private final FormLayout createForm;

    public ScheduleStep() {
        this.createForm = buildCreateForm();

        type.setValue(ScheduleType.ONCE);
        timezone.setValue(ScheduleText.DEFAULT_TIMEZONE);
        type.addValueChangeListener(e -> showFieldsForType(e.getValue()));

        setPadding(false);
        StyleConfig.create().gap(Tokens.SPACE_S).applyTo(this);
        add(buildHelp(), createForm);
        showFieldsForType(ScheduleType.ONCE);
    }

    private FormLayout buildCreateForm() {
        FormLayout layout = new FormLayout(type, label, runAt, cronExpression, intervalSeconds, timezone);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep(Tokens.FORM_BREAKPOINT_ZERO, Tokens.FORM_COLUMNS));
        layout.setColspan(runAt, Tokens.FORM_COLSPAN_FULL);
        return layout;
    }

    private Span buildHelp() {
        return StyleConfig.create()
                .color(Tokens.TEXT_SECONDARY)
                .fontSize(Tokens.FONT_S)
                .applyTo(new Span(WizardText.HELP_SCHEDULE));
    }

    private void showFieldsForType(ScheduleType selected) {
        runAt.setVisible(selected == ScheduleType.ONCE);
        cronExpression.setVisible(selected == ScheduleType.CRON);
        intervalSeconds.setVisible(selected == ScheduleType.FIXED);
        timezone.setVisible(selected != ScheduleType.FIXED);
    }

    /** Validates the type-driven "when" fields for the schedule about to be created. */
    public boolean validate() {
        return switch (type.getValue()) {
            case ONCE -> ScheduleWhenFields.validateRunAt(runAt, timezone);
            case CRON -> FieldValidation.require(cronExpression, UiText.VALIDATION_REQUIRED);
            case FIXED -> ScheduleWhenFields.validateInterval(intervalSeconds);
        };
    }

    /** Installs a request factory on {@code draft} that builds the new schedule's {@link CreateScheduleRequest}. */
    public void readInto(WizardDraft draft) {
        String typeName = type.getValue().name();
        String labelValue = Strings.trimToNull(label.getValue());
        Instant runAtValue = ScheduleWhenFields.runAtInstant(runAt, timezone);
        String cronValue = Strings.trimToNull(cronExpression.getValue());
        Integer intervalValue = intervalSeconds.getValue();
        String timezoneValue = Strings.trimToNull(timezone.getValue());

        draft.createNewSchedule(taskId -> new CreateScheduleRequest(
                typeName, labelValue, runAtValue, cronValue, intervalValue, timezoneValue));
    }

    Select<ScheduleType> type() {
        return type;
    }

    DateTimePicker runAt() {
        return runAt;
    }

    TextField cronExpression() {
        return cronExpression;
    }
}
