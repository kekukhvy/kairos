package dev.kairos.domain.task;

import dev.kairos.domain.destination.DestinationId;

import java.time.Instant;
import java.util.Objects;

import static dev.kairos.common.util.helpers.Validation.requirePositive;
import static dev.kairos.common.util.helpers.Validation.requireText;

/**
 * Task: what to deliver, where, and with which delivery settings (see the tasks
 * table). Pure domain — no framework dependencies.
 *
 * <p>This is a mutable entity. Its identity is {@link TaskId}; editable state is
 * changed only through {@link #update} and {@link #softDelete}. Timestamps are
 * supplied by the caller (the application layer, via a Clock) so the domain stays
 * deterministic and testable.
 *
 * <p><b>Invariant (M1):</b> soft delete. Once {@code deletedAt} is set the task is
 * frozen; any further {@link #update} or {@link #softDelete} is rejected with
 * {@link TaskAlreadyDeletedException}.
 */

public final class Task {

    public static final int MAX_SERVICE_LENGTH = 128;       // tasks.service VARCHAR(128)
    public static final int MAX_NAME_LENGTH = 255;          // tasks.name VARCHAR(255)
    public static final int MAX_MESSAGE_TYPE_LENGTH = 255;  // tasks.message_type VARCHAR(255)

    private final TaskId id;
    private final String service;   // owning service — immutable for the task's lifetime
    private String name;
    private String description;      // nullable
    private boolean active;
    private DestinationId destinationId;
    private String messageType;
    private String payload;          // raw JSON, nullable, opaque to the domain
    private int timeoutMs;
    private boolean supportsRetry;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private Task(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id must not be null");
        this.service = requireText(b.service, "service", MAX_SERVICE_LENGTH);
        this.name = requireText(b.name, "name", MAX_NAME_LENGTH);
        this.description = b.description;
        this.active = b.active;
        this.destinationId = Objects.requireNonNull(b.destinationId, "value must not be null");
        this.messageType = requireText(b.messageType, "messageType", MAX_MESSAGE_TYPE_LENGTH);
        this.payload = b.payload;
        this.timeoutMs = requirePositive(b.timeoutMs, "timeoutMs");
        this.supportsRetry = b.supportsRetry;
        this.createdAt = Objects.requireNonNull(b.createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(b.updatedAt, "updatedAt must not be null");
        this.deletedAt = b.deletedAt;
    }


    public void update(TaskEdit edit, Instant now) {
        ensureNotDeleted();
        Objects.requireNonNull(edit, "edit must not be null");
        Objects.requireNonNull(now, "now must not be null");

        this.name = requireText(edit.name(), "name", MAX_NAME_LENGTH);
        this.description = edit.description();
        this.active = edit.active();
        this.destinationId = Objects.requireNonNull(edit.destinationId(), "value must not be null");
        this.messageType = requireText(edit.messageType(), "messageType", MAX_MESSAGE_TYPE_LENGTH);
        this.payload = edit.payload();
        this.timeoutMs = requirePositive(edit.timeoutMs(), "timeoutMs");
        this.supportsRetry = edit.supportsRetry();
        this.updatedAt = now;
    }


    /**
     * Marks the task as soft-deleted. Rejected if it was already deleted (the
     * application maps that to a 409).
     */
    public void softDelete(Instant now) {
        ensureNotDeleted();
        Objects.requireNonNull(now, "now must not be null");
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public void setActive(boolean active, Instant now) {
        if (isDeleted()) {
            throw new TaskNotFoundException(this.id);
        }

        this.active = active;
        this.updatedAt = now;
    }

    private void ensureNotDeleted() {
        if (isDeleted()) {
            throw new TaskAlreadyDeletedException(id);
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    // --- getters ------------------------------------------------------------

    public TaskId id() {
        return id;
    }

    public String service() {
        return service;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public boolean active() {
        return active;
    }

    public DestinationId destinationId() {
        return destinationId;
    }

    public String messageType() {
        return messageType;
    }

    public String payload() {
        return payload;
    }

    public int timeoutMs() {
        return timeoutMs;
    }

    public boolean supportsRetry() {
        return supportsRetry;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    // --- builder ------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private TaskId id;
        private String service;
        private String name;
        private String description;
        private boolean active = true;          // tasks.active DEFAULT true
        private DestinationId destinationId;
        private String messageType;
        private String payload;
        private int timeoutMs;
        private boolean supportsRetry = false;  // tasks.supports_retry DEFAULT false
        private Instant createdAt;
        private Instant updatedAt;
        private Instant deletedAt;

        private Builder() {
        }

        public Builder id(TaskId id) {
            this.id = id;
            return this;
        }

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder destinationId(DestinationId destinationId) {
            this.destinationId = destinationId;
            return this;
        }

        public Builder messageType(String messageType) {
            this.messageType = messageType;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder timeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder supportsRetry(boolean supportsRetry) {
            this.supportsRetry = supportsRetry;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder deletedAt(Instant deletedAt) {
            this.deletedAt = deletedAt;
            return this;
        }

        public Task build() {
            return new Task(this);
        }
    }

}
