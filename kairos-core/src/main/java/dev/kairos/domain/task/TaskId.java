package dev.kairos.domain.task;

import java.util.Objects;
import java.util.UUID;

public record TaskId(UUID value) {

    public TaskId {
        Objects.requireNonNull(value, "TaskId cannot be null!");
    }

    /**
     * A brand-new random identity for a task being created.
     */
    public static TaskId newId() {
        return new TaskId(UUID.randomUUID());
    }

    public static TaskId of(UUID value) {
        return new TaskId(value);
    }

    public static TaskId fromString(String id) {
        return new TaskId(UUID.fromString(id));
    }

    @Override
    public String toString() {
        return "TaskId{" +
                "value=" + value +
                '}';
    }
}
