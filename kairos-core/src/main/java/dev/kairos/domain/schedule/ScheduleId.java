package dev.kairos.domain.schedule;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@link Schedule} aggregate. A schedule is its own aggregate and
 * is never loaded through {@link dev.kairos.domain.task.Task}.
 */
public record ScheduleId(UUID value) {

    public ScheduleId {
        Objects.requireNonNull(value, "ScheduleId cannot be null!");
    }

    /**
     * A brand-new random identity for a schedule being created.
     */
    public static ScheduleId newId() {
        return new ScheduleId(UUID.randomUUID());
    }

    public static ScheduleId of(UUID value) {
        return new ScheduleId(value);
    }

    public static ScheduleId fromString(String id) {
        return new ScheduleId(UUID.fromString(id));
    }

    @Override
    public String toString() {
        return "ScheduleId{value=" + value + '}';
    }
}
