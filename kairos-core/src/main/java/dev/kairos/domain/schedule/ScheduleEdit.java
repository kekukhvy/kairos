package dev.kairos.domain.schedule;

import java.time.Instant;

/**
 * Editable fields of a {@link Schedule} on update. {@code type} is immutable and
 * therefore absent. Only the "when" field matching the schedule's current type
 * is read ({@code runAt} for {@code ONCE}, {@code cronExpression} for
 * {@code CRON}, {@code intervalSeconds} for {@code FIXED}); the irrelevant ones
 * are ignored, so a caller may leave them null.
 */
public record ScheduleEdit(
        String label,
        Instant runAt,
        String cronExpression,
        Integer intervalSeconds,
        String timezone
) {
}
