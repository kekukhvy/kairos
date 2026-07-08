package dev.kairos.domain.schedule;

/**
 * Timezone constants for schedules. The DB column defaults to {@code 'UTC'};
 * this mirrors that default so the domain and schema stay consistent.
 */
final class ScheduleTimezone {

    /** Default timezone when none is supplied — matches {@code schedules.timezone DEFAULT 'UTC'}. */
    static final String DEFAULT = "UTC";

    private ScheduleTimezone() {
    }
}
