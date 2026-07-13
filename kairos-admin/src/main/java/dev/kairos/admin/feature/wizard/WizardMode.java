package dev.kairos.admin.feature.wizard;

/**
 * Which of the two ways the Destination step can supply its entity: pick one
 * that already exists, or fill in the inline fields to create a new one.
 * Only the active mode's fields are validated. The Schedule step has no such
 * choice — it always creates a new schedule for the task being set up.
 */
public enum WizardMode {
    SELECT_EXISTING,
    CREATE_NEW
}
