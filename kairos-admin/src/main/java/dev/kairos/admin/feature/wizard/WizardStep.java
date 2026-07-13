package dev.kairos.admin.feature.wizard;

/**
 * The three ordered steps of the {@code SetupWizard}, in the order they are
 * shown to the operator: Task, then Destination, then Schedule. This display
 * order is intentionally different from the commit order at Finish (which
 * resolves the dependency chain Destination → Task → Schedule).
 */
public enum WizardStep {
    TASK,
    DESTINATION,
    SCHEDULE
}
