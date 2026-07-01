package dev.kairos.api;

import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;

import java.time.Instant;
import java.util.UUID;

/**
 * Test-data factory for API-layer tests. Produces {@link Task} domain entities
 * with stable, well-known field values so each test overrides only what it
 * cares about.
 */
final class ApiTaskBuilder {

    static final UUID TASK_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final TaskId TASK_ID = new TaskId(TASK_UUID);
    static final String SERVICE = "payment-service";
    static final String NAME = "send-receipt";
    static final String DESCRIPTION = "Sends a payment receipt";
    static final String DESTINATION_ID = "dest-kafka-1";
    static final String EVENT_NAME = "payment.receipt.send";
    static final String PAYLOAD_JSON = "{\"orderId\":\"abc\"}";
    static final int TIMEOUT_MS = 5_000;
    static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    static final Instant UPDATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private ApiTaskBuilder() {
    }

    /** A fully populated live task with a predictable id. */
    static Task liveTask() {
        return Task.builder()
                .id(TASK_ID)
                .service(SERVICE)
                .name(NAME)
                .description(DESCRIPTION)
                .active(true)
                .destinationId(DestinationId.of(DESTINATION_ID))
                .eventName(EVENT_NAME)
                .payload(PAYLOAD_JSON)
                .timeoutMs(TIMEOUT_MS)
                .supportsRetry(false)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }

    /** A live task with a custom id (no payload, no description). */
    static Task liveTaskWithId(TaskId id) {
        return Task.builder()
                .id(id)
                .service(SERVICE)
                .name(NAME)
                .destinationId(DestinationId.of(DESTINATION_ID))
                .eventName(EVENT_NAME)
                .timeoutMs(TIMEOUT_MS)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }

    /** A soft-deleted task with the default id. */
    static Task deletedTask() {
        Instant deletedAt = Instant.parse("2026-02-01T00:00:00Z");
        return Task.builder()
                .id(TASK_ID)
                .service(SERVICE)
                .name(NAME)
                .destinationId(DestinationId.of(DESTINATION_ID))
                .eventName(EVENT_NAME)
                .timeoutMs(TIMEOUT_MS)
                .createdAt(CREATED_AT)
                .updatedAt(deletedAt)
                .deletedAt(deletedAt)
                .build();
    }

    /** Generates a fresh random {@link TaskId}. */
    static TaskId randomTaskId() {
        return new TaskId(UUID.randomUUID());
    }
}
