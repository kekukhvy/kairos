package dev.kairos.admin.feature.wizard;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import dev.kairos.admin.feature.destination.DestinationService;
import dev.kairos.admin.feature.schedule.ScheduleService;
import dev.kairos.admin.feature.task.TaskService;
import dev.kairos.admin.feature.wizard.component.DestinationStep;
import dev.kairos.admin.feature.wizard.component.ScheduleStep;
import dev.kairos.admin.feature.wizard.component.TaskStep;
import dev.kairos.admin.feature.wizard.component.WizardStepper;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Notifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Modal dialog that guides an operator through creating a Task, its
 * Destination and its Schedule in one flow. Displays the three steps in
 * {@link WizardStep} order (Task → Destination → Schedule) but commits them
 * at Finish in dependency order (destination → task → schedule) via
 * {@link WizardCommit}. Nothing is persisted before Finish; Back/Next only
 * move between steps. Thin by design — navigation lives in
 * {@link WizardStepState}, commit sequencing in {@link WizardCommit}, both
 * UI-free and unit-tested on their own.
 */
public class SetupWizard extends Dialog {

    private static final Logger logger = LoggerFactory.getLogger(SetupWizard.class);

    private final TaskStep taskStep;
    private final DestinationStep destinationStep;
    private final ScheduleStep scheduleStep;
    private final WizardDraft draft = new WizardDraft();
    private final WizardCommit commit;
    private final Runnable onSuccess;

    private final Button backButton;
    private final Button nextButton;

    private WizardStepState state = WizardStepState.initial();
    private Component stepperSlot;
    private Component bodySlot;

    public SetupWizard(JsonMapper jsonMapper, TaskService taskService,
                       DestinationService destinationService, ScheduleService scheduleService,
                       Runnable onSuccess) {
        this.onSuccess = onSuccess;
        this.taskStep = new TaskStep(jsonMapper);
        this.destinationStep = new DestinationStep(jsonMapper, destinationService.list());
        this.scheduleStep = new ScheduleStep();
        this.commit = new WizardCommit(destinationService::create, taskService::create, scheduleService::create);
        this.backButton = Buttons.secondary(WizardText.BTN_BACK, e -> goBack());
        this.nextButton = Buttons.primary(state.nextButtonLabel(), e -> goNext());

        setHeaderTitle(WizardText.DIALOG_TITLE);
        setWidth(Tokens.DIALOG_WIDTH_L);
        renderBody();
        getFooter().add(buildCancel(), backButton, nextButton);
        refreshFooter();
    }

    private Component stepForCurrentStep() {
        return switch (state.currentStep()) {
            case TASK -> taskStep;
            case DESTINATION -> destinationStep;
            case SCHEDULE -> scheduleStep;
        };
    }

    private boolean validateCurrentStep() {
        return switch (state.currentStep()) {
            case TASK -> taskStep.validate();
            case DESTINATION -> destinationStep.validate();
            case SCHEDULE -> scheduleStep.validate();
        };
    }

    private void readCurrentStepInto(WizardDraft target) {
        switch (state.currentStep()) {
            case TASK -> taskStep.readInto(target);
            case DESTINATION -> destinationStep.readInto(target);
            case SCHEDULE -> scheduleStep.readInto(target);
        }
    }

    private Button buildCancel() {
        return Buttons.tertiary(WizardText.BTN_CANCEL, e -> close());
    }

    private void goBack() {
        state = state.back();
        renderBody();
        refreshFooter();
    }

    private void goNext() {
        if (!validateCurrentStep()) {
            Notifications.error(WizardText.NOTIFY_INVALID_STEP);
            return;
        }
        readCurrentStepInto(draft);
        if (state.isLastStep()) {
            finish();
            return;
        }
        state = state.next();
        renderBody();
        refreshFooter();
    }

    private void finish() {
        try {
            commit.execute(draft);
        } catch (RuntimeException ex) {
            logger.error("Wizard commit failed", ex);
            Notifications.error(WizardText.NOTIFY_FAILED);
            return;
        }
        Notifications.success(WizardText.NOTIFY_SUCCESS);
        onSuccess.run();
        close();
    }

    /** (Re)renders the stepper and the current step's body, replacing whatever was shown before. */
    private void renderBody() {
        if (stepperSlot != null) {
            remove(stepperSlot, bodySlot);
        }
        stepperSlot = new WizardStepper(state.currentStep());
        bodySlot = stepForCurrentStep();
        add(stepperSlot, bodySlot);
    }

    private void refreshFooter() {
        backButton.setEnabled(state.canGoBack());
        nextButton.setText(state.nextButtonLabel());
    }
}
