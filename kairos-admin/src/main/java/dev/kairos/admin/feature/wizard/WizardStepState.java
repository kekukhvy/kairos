package dev.kairos.admin.feature.wizard;

/**
 * Pure, UI-free navigation state for the {@code SetupWizard}. Immutable: each
 * navigation method returns a new state rather than mutating in place, so the
 * dialog can simply reassign its held reference. Deliberately has no
 * knowledge of validation or persistence — {@code SetupWizard} validates the
 * current step before calling {@link #next()}.
 */
public final class WizardStepState {

    private static final WizardStep[] STEPS = WizardStep.values();

    private final int index;

    private WizardStepState(int index) {
        this.index = index;
    }

    /** Returns a fresh state positioned on the first step ({@link WizardStep#TASK}). */
    public static WizardStepState initial() {
        return new WizardStepState(0);
    }

    public WizardStep currentStep() {
        return STEPS[index];
    }

    /** {@code true} once past the first step — the Back button is enabled. */
    public boolean canGoBack() {
        return index > 0;
    }

    /** {@code true} on the last step — the Next button reads "Finish". */
    public boolean isLastStep() {
        return index == STEPS.length - 1;
    }

    /** Advances to the next step, or stays put if already on the last step. */
    public WizardStepState next() {
        if (isLastStep()) {
            return this;
        }
        return new WizardStepState(index + 1);
    }

    /** Returns to the previous step, or stays put if already on the first step. */
    public WizardStepState back() {
        if (!canGoBack()) {
            return this;
        }
        return new WizardStepState(index - 1);
    }

    /** Label the footer's advance button should show for the current step. */
    public String nextButtonLabel() {
        return isLastStep() ? WizardText.BTN_FINISH : WizardText.BTN_NEXT;
    }
}
