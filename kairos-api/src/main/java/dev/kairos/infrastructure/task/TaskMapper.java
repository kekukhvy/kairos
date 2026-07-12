package dev.kairos.infrastructure.task;

import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import dev.kairos.infrastructure.generated.tables.records.TasksRecord;
import org.jooq.JSONB;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Converts between the jOOQ-generated {@link dev.kairos.infrastructure.generated.tables.records.TasksRecord}
 * and the {@link dev.kairos.domain.task.Task}
 * domain entity. Lives in the infrastructure package so the domain stays free of
 * any jOOQ types.
 *
 * <p>JSONB columns ({@code payload}) are represented as {@link org.jooq.JSONB} in
 * generated records. We convert to/from {@code String} here via
 * {@link org.jooq.JSONB#data()} and {@link org.jooq.JSONB#valueOf(String)} — no codegen forced-type
 * hacks needed.
 */
final class TaskMapper {

    private TaskMapper() {
    }

    // --- record -> domain ---------------------------------------------------

    static Task toDomain(TasksRecord r) {
        return Task.builder()
                .id(TaskId.of(r.getId()))
                .service(r.getService())
                .name(r.getName())
                .description(r.getDescription())
                .active(r.getActive())
                .destinationId(DestinationId.of(r.getDestinationId()))
                .eventName(r.getEventName())
                .payload(fromJsonb(r.getPayload()))
                .timeoutMs(r.getTimeoutMs())
                .supportsRetry(r.getSupportsRetry())
                .createdAt(toInstant(r.getCreatedAt()))
                .updatedAt(toInstant(r.getUpdatedAt()))
                .deletedAt(r.getDeletedAt() != null ? toInstant(r.getDeletedAt()) : null)
                .build();
    }

    static TasksRecord toRecord(Task task, TasksRecord r) {
        r.setId(task.id().value());
        r.setService(task.service());
        r.setName(task.name());
        r.setDescription(task.description());
        r.setActive(task.active());
        r.setDestinationId(task.destinationId().value());
        r.setEventName(task.eventName());
        r.setPayload(toJsonb(task.payload()));
        r.setTimeoutMs(task.timeoutMs());
        r.setSupportsRetry(task.supportsRetry());
        r.setCreatedAt(toOffsetDateTime(task.createdAt()));
        r.setUpdatedAt(toOffsetDateTime(task.updatedAt()));
        r.setDeletedAt(task.deletedAt() != null ? toOffsetDateTime(task.deletedAt()) : null);
        return r;
    }


    // --- JSONB helpers ------------------------------------------------------

    /** {@code null}-safe {@link JSONB} → {@code String}. */
    private static String fromJsonb(JSONB jsonb) {
        return jsonb != null ? jsonb.data() : null;
    }

    /** {@code null}-safe {@code String} → {@link JSONB}. */
    private static JSONB toJsonb(String value) {
        return value != null ? JSONB.valueOf(value) : null;
    }

    // --- timestamp helpers --------------------------------------------------

    private static Instant toInstant(OffsetDateTime odt) {
        return odt.toInstant();
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
