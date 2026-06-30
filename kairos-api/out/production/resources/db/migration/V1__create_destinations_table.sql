-- destinations: where to deliver (Kafka / SQS / Webhook / RabbitMQ).
-- Stored separately from tasks so connectivity config isn't duplicated.
CREATE TABLE destinations (
    id         VARCHAR(128) PRIMARY KEY,        -- human-readable id, e.g. 'booking-kafka'
    type       VARCHAR(32)  NOT NULL,           -- KAFKA | SQS | WEBHOOK | RABBITMQ
    config     JSONB        NOT NULL,           -- topic / url / credentials reference, etc.
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT destinations_type_check
        CHECK (type IN ('KAFKA', 'SQS', 'WEBHOOK', 'RABBITMQ'))
);
