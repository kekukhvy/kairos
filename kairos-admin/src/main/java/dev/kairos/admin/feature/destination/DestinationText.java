package dev.kairos.admin.feature.destination;

/**
 * UI string constants for the Destinations feature — column headers and
 * other display labels. Centralised here so that literal strings do not
 * scatter across view and grid classes.
 */
public final class DestinationText {

    private DestinationText() {
    }

    public static final String COL_DESTINATION_ID = "Id";

    public static final String COL_DESTINATION_TYPE = "Type";
    public static final String COL_DESTINATION_CONFIG = "Config";
    public static final String COL_CREATED_AT = "CreatedAt";
    public static final String TITLE = "Destinations";
    public static final String NEW_DESTINATION = "Create Destination";

    public static final String FIELD_ID = "Id";
    public static final String FIELD_TYPE = "Type";
    public static final String FIELD_CONFIG = "Config (JSON)";

    public static final String NEW_DESTINATION_TITLE = "New destination";

    public static final String BTN_SAVE = "Save";
    public static final String BTN_CANCEL = "Cancel";

    public static final String VALIDATION_REQUIRED = "Required";
    public static final String VALIDATION_INVALID_JSON = "Invalid JSON";

    public static final String NOTIFY_CREATED = "Destination created";
    public static final String NOTIFY_CREATE_FAILED = "Failed to create destination";

    public static final String TYPE_KAFKA = "KAFKA";
    public static final String TYPE_SQS = "SQS";
    public static final String TYPE_WEBHOOK = "WEBHOOK";
    public static final String TYPE_RABBITMQ = "RABBITMQ";

    public static final String FILTER_TYPE = "Type";
}
