package dev.kairos.admin.feature.task;

/**
 * Route and query-parameter constants for the Task feature.
 * Centralised here so the dashboard deep-link and the view share the same strings.
 */
public final class TaskRoutes {

    private TaskRoutes() {
    }

    /** Vaadin route segment for the task list view. */
    public static final String TASKS = "tasks";

    /** Browser tab title for the task list page. */
    public static final String PAGE_TITLE = "Tasks · Kairos";

    /** Query parameter that pre-selects the status filter, e.g. {@code tasks?status=Active}. */
    public static final String QUERY_STATUS = "status";


}
