package dev.kairos.admin.feature.schedule.dto;

/**
 * The three schedule timing kinds, mirroring the domain {@code ScheduleType}.
 * Held locally so the admin module stays free of a dependency on {@code domain}.
 * Each type carries exactly one "when" field:
 * {@code ONCE} → {@code runAt}, {@code CRON} → {@code cronExpression},
 * {@code FIXED} → {@code intervalSeconds}.
 */
public enum ScheduleType {
    ONCE,
    CRON,
    FIXED
}
