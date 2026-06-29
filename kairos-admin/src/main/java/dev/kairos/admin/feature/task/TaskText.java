package dev.kairos.admin.feature.task;

import com.vaadin.flow.component.Component;

public final class TaskText {


    private TaskText() {
    }

    public static final String TITLE = "Tasks";
    public static final String NEW_TASK = "New task";

    public static final String COL_SERVICE = "Service";
    public static final String COL_NAME = "Name";
    public static final String COL_DESTINATION = "Destination";
    public static final String COL_MESSAGE_TYPE = "Message type";
    public static final String COL_ACTIVE = "Active";
    public static final String COL_TIMEOUT = "Timeout (ms)";

    public static final String FIELD_DESCRIPTION = "Description";
    public static final String FIELD_PAYLOAD = "Payload (JSON)";
    public static final String FIELD_SUPPORTS_RETRY = "Supports retry";

    public static final String BTN_SAVE = "Save";
    public static final String BTN_CANCEL = "Cancel";

    public static final String VALIDATION_REQUIRED = "Required";
    public static final String VALIDATION_INVALID_JSON = "Invalid JSON";

    public static final String NOTIFY_CREATED = "Task created";
    public static final String NOTIFY_CREATE_FAILED = "Failed to create task";
}