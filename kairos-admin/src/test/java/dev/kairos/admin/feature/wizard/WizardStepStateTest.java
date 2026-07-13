package dev.kairos.admin.feature.wizard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the pure navigation state that backs {@code SetupWizard}: current
 * step index, back/next gating, and the Next/Finish button label — all
 * exercised without any Vaadin UI.
 */
class WizardStepStateTest {

    @Test
    void initialState_startsOnFirstStep() {
        WizardStepState state = WizardStepState.initial();

        assertThat(state.currentStep()).isEqualTo(WizardStep.TASK);
    }

    @Test
    void initialState_backIsDisabled() {
        WizardStepState state = WizardStepState.initial();

        assertThat(state.canGoBack()).isFalse();
    }

    @Test
    void initialState_isNotLastStep() {
        WizardStepState state = WizardStepState.initial();

        assertThat(state.isLastStep()).isFalse();
    }

    @Test
    void next_fromFirstStep_advancesToSecondStep() {
        WizardStepState state = WizardStepState.initial();

        WizardStepState advanced = state.next();

        assertThat(advanced.currentStep()).isEqualTo(WizardStep.DESTINATION);
    }

    @Test
    void next_fromSecondStep_advancesToThirdStep() {
        WizardStepState state = WizardStepState.initial().next();

        WizardStepState advanced = state.next();

        assertThat(advanced.currentStep()).isEqualTo(WizardStep.SCHEDULE);
    }

    @Test
    void next_onLastStep_doesNotAdvanceFurther() {
        WizardStepState state = WizardStepState.initial().next().next();

        WizardStepState advanced = state.next();

        assertThat(advanced.currentStep()).isEqualTo(WizardStep.SCHEDULE);
    }

    @Test
    void secondStep_backIsEnabled() {
        WizardStepState state = WizardStepState.initial().next();

        assertThat(state.canGoBack()).isTrue();
    }

    @Test
    void thirdStep_isLastStep() {
        WizardStepState state = WizardStepState.initial().next().next();

        assertThat(state.isLastStep()).isTrue();
    }

    @Test
    void back_fromSecondStep_returnsToFirstStep() {
        WizardStepState state = WizardStepState.initial().next();

        WizardStepState back = state.back();

        assertThat(back.currentStep()).isEqualTo(WizardStep.TASK);
    }

    @Test
    void back_fromFirstStep_staysOnFirstStep() {
        WizardStepState state = WizardStepState.initial();

        WizardStepState back = state.back();

        assertThat(back.currentStep()).isEqualTo(WizardStep.TASK);
    }

    @Test
    void nextButtonLabel_onNonLastStep_isNext() {
        WizardStepState state = WizardStepState.initial();

        assertThat(state.nextButtonLabel()).isEqualTo(WizardText.BTN_NEXT);
    }

    @Test
    void nextButtonLabel_onLastStep_isFinish() {
        WizardStepState state = WizardStepState.initial().next().next();

        assertThat(state.nextButtonLabel()).isEqualTo(WizardText.BTN_FINISH);
    }
}
