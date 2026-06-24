package dev.kairos.application.task.usecases;

import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;

import java.time.Instant;
import java.util.UUID;

/**
 * Shared test-data factory for use-case tests. Mirrors the conventions of
 * the domain-layer {@code TaskBuilder} but lives in the use-case test package
 * so use-case tests don't depend on the domain test helper.
 */
final class UseCaseTaskBuilder {

    static final TaskId TASK_ID =
            new TaskId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    static final String DESTINATION_ID = "dest-kafka-1";
    static final String SERVICE = "payment-service";
    static final String NAME = "send-receipt";
    static final String MESSAGE_TYPE = "payment.receipt.send";
    static final int TIMEOUT_MS = 5_000;
    static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    static final Instant FIXED_NOW = Instant.parse("2026-06-01T12:00:00Z");

    private UseCaseTaskBuilder() {}

    /** Builds a fully valid live (not deleted) task with predictable defaults. */
    static Task liveTask() {
        return Task.builder()
                .id(TASK_ID)
                .service(SERVICE)
                .name(NAME)
                .destinationId(DestinationId.of(DESTINATION_ID))
                .messageType(MESSAGE_TYPE)
                .timeoutMs(TIMEOUT_MS)
                .createdAt(CREATED_AT)
                .updatedAt(CREATED_AT)
                .build();
    }

    /** Builds a live task with a caller-supplied id. */
    static Task liveTaskWithId(TaskId id) {
        return Task.builder()
                .id(id)
                .service(SERVICE)
                .name(NAME)
                .destinationId(DestinationId.of(DESTINATION_ID))
                .messageType(MESSAGE_TYPE)
                .timeoutMs(TIMEOUT_MS)
                .createdAt(CREATED_AT)
                .updatedAt(CREATED_AT)
                .build();
    }

    /** Builds a soft-deleted task. */
    static Task deletedTask() {
        Instant deletedAt = Instant.parse("2026-02-01T00:00:00Z");
        return Task.builder()
                .id(TASK_ID)
                .service(SERVICE)
                .name(NAME)
                .destinationId(DestinationId.of(DESTINATION_ID))
                .messageType(MESSAGE_TYPE)
                .timeoutMs(TIMEOUT_MS)
                .createdAt(CREATED_AT)
                .updatedAt(deletedAt)
                .deletedAt(deletedAt)
                .build();
    }
}
