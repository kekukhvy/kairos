package dev.kairos.domain.destination;

import java.time.Instant;
import java.util.Objects;

import static dev.kairos.common.util.helpers.Validation.requireText;

/**
 * A delivery target for scheduled messages (Kafka topic, SQS queue, webhook
 * endpoint, RabbitMQ exchange, etc.).
 *
 * <p>{@code Destination} is an entity identified by {@link DestinationId};
 * equality and hashing are based on identity only, not on the current
 * {@code type}/{@code config}/{@code createdAt} values.
 *
 * <p>{@code type} and {@code createdAt} are immutable once the destination is
 * created — changing the delivery mechanism is modelled as deleting one
 * destination and creating another, not as mutating an existing one.
 * {@code config} may be corrected after creation (e.g. fixing a typo in a
 * topic name or webhook URL) via {@link #updateConfig(String)}.
 */
public class Destination {


    private final DestinationId destinationId;
    private final DestinationType destinationType;
    private String config;
    private final Instant createdAt;

    private Destination(Builder builder) {
        this.destinationId = Objects.requireNonNull(builder.destinationId, "destinationId cannot be null!");
        this.destinationType = Objects.requireNonNull(builder.destinationType, "destinationType cannot be null!");
        this.config = validateConfig(builder.config);
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt cannot be null!");
    }

    public static Builder builder() {
        return new Builder();
    }

    public DestinationId destinationId() {
        return destinationId;
    }

    public DestinationType destinationType() {
        return destinationType;
    }

    public String config() {
        return config;
    }

    public Instant createdAt() {
        return createdAt;
    }

    /**
     * Replaces the delivery config (e.g. a Kafka topic name or webhook URL).
     * Does not touch {@code type} or {@code createdAt} — swapping the delivery
     * mechanism itself is out of scope for an update.
     *
     * @param config new connectivity config; must be non-null and non-blank
     * @throws dev.kairos.common.exceptions.ValidationException if {@code config} is null or blank
     */
    public void updateConfig(String config) {
        this.config = validateConfig(config);
    }

    private String validateConfig(String config) {
        requireText(config, "config");
        return config;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Destination that = (Destination) o;
        return Objects.equals(destinationId, that.destinationId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(destinationId);
    }

    @Override
    public String toString() {
        return "Destination{" +
                "destinationId=" + destinationId +
                ", destinationType=" + destinationType +
                ", config='" + config + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    public static final class Builder {
        private DestinationId destinationId;
        private DestinationType destinationType;
        private String config;
        private Instant createdAt;

        private Builder() {
        }

        public Builder destinationId(DestinationId destinationId) {
            this.destinationId = destinationId;
            return this;
        }

        public Builder destinationType(DestinationType destinationType) {
            this.destinationType = destinationType;
            return this;
        }


        public Builder destinationConfig(String destinationConfig) {
            this.config = destinationConfig;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Destination build() {
            return new Destination(this);
        }
    }
}