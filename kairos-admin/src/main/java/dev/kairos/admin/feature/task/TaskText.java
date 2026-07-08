package dev.kairos.admin.feature.task;

/**
 * UI string constants for the Tasks feature — column headers, field labels,
 * action labels, and notification messages. Centralised here so that literal
 * strings do not scatter across view and grid classes.
 */
public final class TaskText {


    private TaskText() {
    }

    public static final String TITLE = "Tasks";
    public static final String NEW_TASK = "New task";

    public static final String COL_SERVICE = "Service";
    public static final String COL_NAME = "Name";
    public static final String COL_DESTINATION = "Destination";
    public static final String COL_EVENT_NAME = "Event name";
    public static final String COL_ACTIVE = "Active";
    public static final String COL_TIMEOUT = "Timeout (ms)";

    public static final String FIELD_DESCRIPTION = "Description";
    public static final String FIELD_PAYLOAD = "Payload (JSON)";
    public static final String FIELD_SUPPORTS_RETRY = "Supports retry";

    public static final String NOTIFY_CREATED = "Task created";
    public static final String NOTIFY_CREATE_FAILED = "Failed to create task";

    public static final String COL_ACTIONS = "Actions";

    public static final String ACTION_VIEW = "View details";
    public static final String ACTION_EDIT = "Edit";
    public static final String ACTION_START = "Start";
    public static final String ACTION_STOP = "Stop";


    public static final String NOTIFY_STARTED = "Task started";
    public static final String NOTIFY_STOPPED = "Task stopped";
    public static final String NOTIFY_UPDATE_FAILED = "Failed to update task";

    public static final String CONFIRM_DELETE_TITLE = "Delete task";
    public static final String CONFIRM_DELETE_TEXT = "This task will be removed. You can't undo this.";

    public static final String NOTIFY_DELETED = "Task deleted";
    public static final String NOTIFY_DELETE_FAILED = "Failed to delete task";

    public static final String DETAILS_TITLE = "Task details";

    public static final String DETAIL_ID = "ID";
    public static final String DETAIL_CREATED = "Created";
    public static final String DETAIL_UPDATED = "Updated";

    public static final String BOOL_YES = "Yes";
    public static final String BOOL_NO = "No";

    public static final String EDIT_TASK = "Edit task";
    public static final String NOTIFY_UPDATED = "Task updated";

    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_INACTIVE = "Inactive";

    public static final String FILTER_STATUS = "Status";
    public static final String FILTER_DESTINATION = "Destination";
}