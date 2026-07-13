package dev.kairos.admin.feature.wizard;

/**
 * UI string constants for the Guided Setup Wizard feature — dialog title,
 * step titles/helpers, footer button labels, mode-toggle labels, field labels
 * and notification messages. Centralised here so no literal strings scatter
 * across the wizard's components.
 */
public final class WizardText {

    private WizardText() {
    }

    // --- dialog ---
    public static final String DIALOG_TITLE = "Guided setup";

    // --- footer buttons ---
    public static final String BTN_BACK = "Back";
    public static final String BTN_NEXT = "Next";
    public static final String BTN_FINISH = "Finish";
    public static final String BTN_CANCEL = "Cancel";

    // --- stepper labels ---
    public static final String STEP_TASK = "Task";
    public static final String STEP_DESTINATION = "Destination";
    public static final String STEP_SCHEDULE = "Schedule";

    // --- step helper text ---
    public static final String HELP_TASK = "Define the new task's fields.";
    public static final String HELP_DESTINATION =
            "Pick an existing destination, or create a new one for this task.";
    public static final String HELP_SCHEDULE = "Define when this task should run.";

    // --- mode toggle ---
    public static final String MODE_USE_EXISTING = "Use existing";
    public static final String MODE_CREATE_NEW = "Create new";

    // --- destination picker (existing mode) ---
    public static final String FIELD_EXISTING_DESTINATION = "Destination";

    // --- schedule picker (existing mode) ---
    public static final String FIELD_EXISTING_SCHEDULE = "Schedule";

    // --- notifications ---
    public static final String NOTIFY_SUCCESS = "Task set up successfully";
    public static final String NOTIFY_FAILED = "Setup failed";
    public static final String NOTIFY_OPEN_FAILED = "Couldn't open guided setup";
    public static final String NOTIFY_INVALID_STEP = "Fix the highlighted fields to continue";
}
