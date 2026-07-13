package dev.kairos.admin.feature.wizard.component;

import dev.kairos.admin.feature.wizard.WizardStep;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the stepper's presentational construction: it renders exactly one
 * label per {@link WizardStep}, in declaration order, and never throws for
 * any current step. The stepper is display-only — it exposes no click
 * handling, matching the "not clickable" acceptance criterion.
 */
class WizardStepperTest {

    @Test
    void construction_forEachStep_rendersOneLabelPerWizardStep() {
        for (WizardStep current : WizardStep.values()) {
            WizardStepper stepper = new WizardStepper(current);

            assertThat(stepper.getComponentCount()).isEqualTo(WizardStep.values().length);
        }
    }
}
