package dev.kairos.common.destination;

/**
 * The delivery mechanism a destination uses to forward scheduled messages.
 * Persisted as a string column ({@code type}) in the {@code destinations}
 * table and round-tripped via {@link #name()} / {@link #valueOf(String)}.
 *
 * <p>Lives in {@code common} — rather than in {@code kairos-api}'s domain
 * package — so that both the domain and {@code kairos-admin} can use the very
 * same enum, alongside {@link DestinationConfigSchema} (the single source of
 * truth for per-type config shape). {@code kairos-admin} depends on
 * {@code common} but not on {@code kairos-api}, and pointing it at the API
 * module would invert the dependency graph. Being plain Java, the enum keeps
 * the domain framework-free.
 *
 * <p>This is the sanctioned pattern for a closed-set domain enum that both
 * {@code kairos-api} and {@code kairos-admin} must agree on: define it once in
 * {@code common}. The alternative — a copy per module — couples the copies by a
 * bare string (`valueOf(name())`), so adding a constant to one and not the other
 * still compiles and only fails at runtime. {@code ScheduleType} is still
 * duplicated that way ({@code domain.schedule} + {@code admin.feature.schedule.dto})
 * and should be collapsed here too when it is next touched.
 */
public enum DestinationType {

    /** Apache Kafka topic. */
    KAFKA,

    /** Amazon SQS queue. */
    SQS,

    /** HTTP/HTTPS webhook endpoint. */
    WEBHOOK,

    /** RabbitMQ exchange. */
    RABBITMQ
}
