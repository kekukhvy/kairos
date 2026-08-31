package dev.kairos.domain.schedule;

import dev.kairos.common.exceptions.ValidationException;

/**
 * The three ways a task's timing can be expressed. Each type requires exactly
 * its own "when" field ({@code ONCE} → {@code runAt}, {@code CRON} →
 * {@code cronExpression}, {@code FIXED} → {@code intervalSeconds}); the
 * {@link Schedule} factory methods enforce this so invalid combinations cannot
 * be constructed, mirroring the {@code schedules_type_fields_check} DB CHECK.
 */
public enum ScheduleType {

    ONCE,
    CRON,
    FIXED;

    /**
     * Parses a raw type string, rejecting anything that is not one of the three
     * known values with a {@link ValidationException} (mapped to HTTP 400).
     *
     * @param raw the type string from the request
     * @return the matching {@link ScheduleType}
     * @throws ValidationException if {@code raw} is null, blank, or unknown
     */
    public static ScheduleType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("type is required");
        }
        try {
            return ScheduleType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid schedule type: " + raw);
        }
    }
}
