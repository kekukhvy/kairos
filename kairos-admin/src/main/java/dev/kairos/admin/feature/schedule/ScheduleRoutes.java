package dev.kairos.admin.feature.schedule;

/** Route constants for the Schedules feature. */
public final class ScheduleRoutes {

    private ScheduleRoutes() {
    }

    public static final String SCHEDULES = "schedules";
    public static final String PAGE_TITLE = "Schedules · Kairos";

    /** Query parameter that pre-selects the task filter, e.g. {@code schedules?task=<taskId>}. */
    public static final String QUERY_TASK = "task";
}
