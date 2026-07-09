package dev.kairos.admin.feature.schedule.component;

import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import dev.kairos.admin.feature.schedule.dto.ScheduleType;
import dev.kairos.admin.shared.util.DateTimes;

/**
 * Resolves a schedule's type-specific "when" value into a display string:
 * {@code ONCE} → formatted {@code runAt}, {@code CRON} → the cron expression,
 * {@code FIXED} → the interval in seconds. Returns a dash placeholder when the
 * relevant field is absent or the type is unknown.
 */
public final class ScheduleWhen {

    private ScheduleWhen() {
    }

    /** Human-readable "when" for the grid and details dialog. */
    public static String describe(ScheduleResponse schedule) {
        ScheduleType type = parseType(schedule.type());
        if (type == null) {
            return ScheduleText.WHEN_EMPTY;
        }
        return switch (type) {
            case ONCE -> orEmpty(DateTimes.forDisplay(schedule.runAt()));
            case CRON -> orEmpty(schedule.cronExpression());
            case FIXED -> describeInterval(schedule.intervalSeconds());
        };
    }

    private static String describeInterval(Integer intervalSeconds) {
        return intervalSeconds == null
                ? ScheduleText.WHEN_EMPTY
                : intervalSeconds + ScheduleText.INTERVAL_UNIT_SUFFIX;
    }

    private static ScheduleType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ScheduleType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String orEmpty(String value) {
        return value == null || value.isBlank() ? ScheduleText.WHEN_EMPTY : value;
    }
}
