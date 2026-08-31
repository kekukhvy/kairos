package dev.kairos.domain.schedule;

import dev.kairos.common.exceptions.ValidationException;
import dev.kairos.domain.task.TaskId;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import static dev.kairos.common.util.helpers.Validation.requireText;

/**
 * Schedule: the "when" rule for a task. Its own aggregate — it references a
 * {@link Task} only by {@link TaskId} and is never loaded through the task.
 * Pure domain — no framework dependencies.
 *
 * <p>The three schedule types are constructed only through the
 * {@link #once(ScheduleId, TaskId, String, Instant, Instant)},
 * {@link #cron(ScheduleId, TaskId, String, String, String, Instant)} and
 * {@link #fixed(ScheduleId, TaskId, String, int, Instant)} factory methods, so
 * an invalid type/field combination cannot be constructed. This mirrors the
 * {@code schedules_type_fields_check} DB CHECK.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>{@code ONCE} — {@code runAt} required and must be in the future at
 *       creation; {@code cronExpression}/{@code intervalSeconds} null.</li>
 *   <li>{@code CRON} — non-blank {@code cronExpression} and a valid
 *       {@code timezone} (parseable {@link ZoneId}); {@code runAt}/
 *       {@code intervalSeconds} null. Cron syntax is <b>not</b> parsed here.</li>
 *   <li>{@code FIXED} — {@code intervalSeconds} with
 *       {@code 0 < intervalSeconds <= MAX_INTERVAL_SECONDS};
 *       {@code runAt}/{@code cronExpression} null.</li>
 * </ul>
 *
 * <p>{@code type} is immutable. {@link #update} changes only the "when" field
 * belonging to the current type, plus {@code label} and {@code timezone}.
 */
public final class Schedule {

    public static final int MAX_LABEL_LENGTH = 128;            // schedules.label VARCHAR(128)
    public static final int MAX_CRON_EXPRESSION_LENGTH = 128;  // schedules.cron_expression VARCHAR(128)
    public static final int MAX_TIMEZONE_LENGTH = 64;          // schedules.timezone VARCHAR(64)
    public static final int MAX_INTERVAL_SECONDS = 86_400;     // one day — longer is a CRON concern

    private final ScheduleId id;
    private final TaskId taskId;
    private final ScheduleType type;   // immutable — change type = delete + recreate
    private String label;              // nullable
    private Instant runAt;             // ONCE only
    private String cronExpression;     // CRON only
    private Integer intervalSeconds;   // FIXED only
    private String timezone;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private Schedule(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id must not be null");
        this.taskId = Objects.requireNonNull(b.taskId, "taskId must not be null");
        this.type = Objects.requireNonNull(b.type, "type must not be null");
        this.label = b.label;
        this.runAt = b.runAt;
        this.cronExpression = b.cronExpression;
        this.intervalSeconds = b.intervalSeconds;
        this.timezone = b.timezone;
        this.active = b.active;
        this.createdAt = Objects.requireNonNull(b.createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(b.updatedAt, "updatedAt must not be null");
    }

    // --- factory methods ----------------------------------------------------

    /**
     * Creates a {@code ONCE} schedule that fires a single time at {@code runAt}.
     *
     * @throws ValidationException if {@code runAt} is null or not in the future
     */
    public static Schedule once(ScheduleId id, TaskId taskId, String label, Instant runAt, Instant now) {
        return baseBuilder(id, taskId, label, now)
                .type(ScheduleType.ONCE)
                .runAt(requireFutureRunAt(runAt, now))
                .build();
    }

    /**
     * Creates a {@code CRON} schedule driven by {@code cronExpression} in the
     * given {@code timezone}. The cron syntax is not parsed here.
     *
     * @throws ValidationException if {@code cronExpression} is blank or
     *                             {@code timezone} is not a valid {@link ZoneId}
     */
    public static Schedule cron(ScheduleId id, TaskId taskId, String label,
                                String cronExpression, String timezone, Instant now) {
        return baseBuilder(id, taskId, label, now)
                .type(ScheduleType.CRON)
                .cronExpression(requireCron(cronExpression))
                .timezone(requireTimezone(timezone))
                .build();
    }

    /**
     * Creates a {@code FIXED} schedule that fires every {@code intervalSeconds}.
     *
     * @throws ValidationException if {@code intervalSeconds} is outside
     *                             {@code (0, MAX_INTERVAL_SECONDS]}
     */
    public static Schedule fixed(ScheduleId id, TaskId taskId, String label, int intervalSeconds, Instant now) {
        return baseBuilder(id, taskId, label, now)
                .type(ScheduleType.FIXED)
                .intervalSeconds(requireInterval(intervalSeconds))
                .build();
    }

    private static Builder baseBuilder(ScheduleId id, TaskId taskId, String label, Instant now) {
        return builder()
                .id(id)
                .taskId(taskId)
                .label(requireLabelWithinLength(label))
                .timezone(ScheduleTimezone.DEFAULT)
                .active(true)
                .createdAt(now)
                .updatedAt(now);
    }

    // --- behavior -----------------------------------------------------------

    /**
     * Updates the "when" field for this schedule's current type, plus
     * {@code label} and {@code timezone}. {@code type} cannot change: to switch
     * type, delete and recreate. Only the field relevant to the current type is
     * read from {@code edit}; the others are ignored.
     */
    public void update(ScheduleEdit edit, Instant now) {
        Objects.requireNonNull(edit, "edit must not be null");
        Objects.requireNonNull(now, "now must not be null");

        this.label = requireLabelWithinLength(edit.label());
        this.timezone = requireTimezone(edit.timezone());
        applyWhenField(edit, now);
        this.updatedAt = now;
    }

    private void applyWhenField(ScheduleEdit edit, Instant now) {
        switch (type) {
            case ONCE -> this.runAt = requireFutureRunAt(edit.runAt(), now);
            case CRON -> this.cronExpression = requireCron(edit.cronExpression());
            case FIXED -> this.intervalSeconds = requireInterval(nullSafeInterval(edit.intervalSeconds()));
        }
    }

    /** Pauses this single rule ({@code active = false}); independent of the task. */
    public void pause(Instant now) {
        this.active = false;
        this.updatedAt = now;
    }

    /** Resumes this single rule ({@code active = true}). */
    public void resume(Instant now) {
        this.active = true;
        this.updatedAt = now;
    }

    // --- invariant guards ---------------------------------------------------

    private static Instant requireFutureRunAt(Instant runAt, Instant now) {
        if (runAt == null) {
            throw new ValidationException("runAt is required for ONCE schedules");
        }
        if (!runAt.isAfter(now)) {
            throw new ValidationException("runAt must be in the future");
        }
        return runAt;
    }

    private static String requireCron(String cronExpression) {
        return requireText(cronExpression, "cronExpression", MAX_CRON_EXPRESSION_LENGTH);
    }

    private static String requireTimezone(String timezone) {
        String value = requireText(timezone, "timezone", MAX_TIMEZONE_LENGTH);
        try {
            ZoneId.of(value);
        } catch (DateTimeException e) {
            throw new ValidationException("Invalid timezone: " + value);
        }
        return value;
    }

    private static int requireInterval(int intervalSeconds) {
        if (intervalSeconds <= 0) {
            throw new ValidationException("intervalSeconds must be greater than 0");
        }
        if (intervalSeconds > MAX_INTERVAL_SECONDS) {
            throw new ValidationException(
                    "intervalSeconds must be at most " + MAX_INTERVAL_SECONDS + " (one day)");
        }
        return intervalSeconds;
    }

    private static int nullSafeInterval(Integer intervalSeconds) {
        if (intervalSeconds == null) {
            throw new ValidationException("intervalSeconds is required for FIXED schedules");
        }
        return intervalSeconds;
    }

    private static String requireLabelWithinLength(String label) {
        if (label != null && label.length() > MAX_LABEL_LENGTH) {
            throw new ValidationException("label must be at most " + MAX_LABEL_LENGTH + " characters");
        }
        return label;
    }

    // --- getters ------------------------------------------------------------

    public ScheduleId id() {
        return id;
    }

    public TaskId taskId() {
        return taskId;
    }

    public ScheduleType type() {
        return type;
    }

    public String label() {
        return label;
    }

    public Instant runAt() {
        return runAt;
    }

    public String cronExpression() {
        return cronExpression;
    }

    public Integer intervalSeconds() {
        return intervalSeconds;
    }

    public String timezone() {
        return timezone;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    // --- builder ------------------------------------------------------------

    /**
     * Low-level builder used by the persistence layer to rehydrate a stored
     * schedule. Application code should use the {@code once}/{@code cron}/
     * {@code fixed} factory methods, which enforce the type invariants.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ScheduleId id;
        private TaskId taskId;
        private ScheduleType type;
        private String label;
        private Instant runAt;
        private String cronExpression;
        private Integer intervalSeconds;
        private String timezone = ScheduleTimezone.DEFAULT;
        private boolean active = true;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {
        }

        public Builder id(ScheduleId id) {
            this.id = id;
            return this;
        }

        public Builder taskId(TaskId taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder type(ScheduleType type) {
            this.type = type;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder runAt(Instant runAt) {
            this.runAt = runAt;
            return this;
        }

        public Builder cronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
            return this;
        }

        public Builder intervalSeconds(Integer intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
            return this;
        }

        public Builder timezone(String timezone) {
            this.timezone = timezone;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
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

        public Schedule build() {
            return new Schedule(this);
        }
    }
}
