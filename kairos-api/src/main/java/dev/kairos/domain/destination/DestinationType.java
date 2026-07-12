package dev.kairos.domain.destination;

/**
 * The delivery mechanism a {@link Destination} uses to forward scheduled
 * messages. The value is persisted as a string column ({@code type}) in the
 * {@code destinations} table and round-tripped via {@link #name()} /
 * {@link #valueOf(String)}.
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
