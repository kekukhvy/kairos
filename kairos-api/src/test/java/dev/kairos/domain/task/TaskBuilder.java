package dev.kairos.domain.task;

import dev.kairos.domain.destination.DestinationId;

import java.time.Instant;
import java.util.UUID;

/**
 * Test-only builder helper that produces a fully valid {@link Task} with
 * sensible defaults. Individual tests override only the fields they care
 * about, keeping each test focused and DRY.
 */
final class TaskBuilder {

    static final TaskId DEFAULT_ID = new TaskId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    static final String DEFAULT_SERVICE = "payment-service";
    static final String DEFAULT_NAME = "send-receipt";
    static final DestinationId DEFAULT_DESTINATION_ID = DestinationId.of("dest-kafka-1");
    static final String DEFAULT_MESSAGE_TYPE = "payment.receipt.send";
    static final String DEFAULT_PAYLOAD = "{\"orderId\":\"abc\"}";
    static final int DEFAULT_TIMEOUT_MS = 5_000;
    static final Instant DEFAULT_CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    static final Instant DEFAULT_UPDATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private TaskBuilder() {}

    static Task.Builder defaults() {
        return Task.builder()
                .id(DEFAULT_ID)
                .service(DEFAULT_SERVICE)
                .name(DEFAULT_NAME)
                .destinationId(DEFAULT_DESTINATION_ID)
                .messageType(DEFAULT_MESSAGE_TYPE)
                .payload(DEFAULT_PAYLOAD)
                .timeoutMs(DEFAULT_TIMEOUT_MS)
                .createdAt(DEFAULT_CREATED_AT)
                .updatedAt(DEFAULT_UPDATED_AT);
    }

    static Task buildDefault() {
        return defaults().build();
    }

    static TaskEdit defaultEdit() {
        return new TaskEdit(
                "updated-name",
                "updated description",
                false,
                DestinationId.of("dest-kafka-2"),
                "payment.receipt.updated",
                "{\"updated\":true}",
                10_000,
                true
        );
    }
}
