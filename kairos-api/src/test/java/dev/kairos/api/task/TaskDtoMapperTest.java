package dev.kairos.api.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.common.dto.task.TaskResponse;
import dev.kairos.domain.destination.DestinationId;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import dev.kairos.infrastructure.ObjectMapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TaskDtoMapper}. Verifies scalar field mapping and all
 * three branches of the payload conversion:
 * <ol>
 *   <li>null payload → null JsonNode</li>
 *   <li>valid JSON string → proper JsonNode (object/array/value)</li>
 *   <li>corrupt stored JSON string → text node fallback (guard against bad DB data)</li>
 * </ol>
 */
class TaskDtoMapperTest {

    private static final UUID TASK_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final TaskId TASK_ID = new TaskId(TASK_UUID);
    private static final String SERVICE = "payment-service";
    private static final String NAME = "send-receipt";
    private static final String DESCRIPTION = "Sends a payment receipt";
    private static final String DESTINATION_ID = "dest-kafka-1";
    private static final String EVENT_NAME = "payment.receipt.send";
    private static final int TIMEOUT_MS = 5_000;
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private static final String VALID_PAYLOAD_JSON = "{\"orderId\":\"abc\"}";
    private static final String CORRUPT_PAYLOAD = "not { valid } json {{";
    private static final String ORDER_ID_FIELD = "orderId";
    private static final String ORDER_ID_VALUE = "abc";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = ObjectMapperFactory.create();
    }

    // ── scalar field mapping ─────────────────────────────────────────────────

    @Test
    void toResponse_mapsIdCorrectly() {
        Task task = taskWithPayload(null);

        TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper);

        assertEquals(TASK_UUID, response.id());
    }

    @Test
    void toResponse_mapsServiceCorrectly() {
        Task task = taskWithPayload(null);

        TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper);

        assertEquals(SERVICE, response.service());
    }

    @Test
    void toResponse_mapsNameCorrectly() {
        Task task = taskWithPayload(null);

        TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper);

        assertEquals(NAME, response.name());
    }

    @Test
    void toResponse_mapsDescriptionCorrectly() {
        Task task = taskWithPayload(null);

        TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper);

        assertEquals(DESCRIPTION, response.description());
    }

    @Test
    void toResponse_mapsActiveCorrectly() {
        Task task = taskWithPayload(null);

        TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper);

        assertTrue(response.active());
    }

    @Test
    void toResponse_mapsDestinationIdCorrectly() {
        Task task = taskWithPayload(null);

        TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper);

        assertEquals(DESTINATION_ID, response.destinationId());
    }

    @Test
    void toResponse_mapsEventNameCorrectly() {
        Task task = taskWithPayload(null);

        TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper);

        assertEquals(EVENT_NAME, response.eventName());
    }

    @Test
    void toResponse_mapsTimeoutMsCorrectly() {
        Task task = taskWithPayload(null);

        TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper);

        assertEquals(TIMEOUT_MS, response.timeoutMs());
    }

    @Test
    void toResponse_mapsCreatedAtCorrectly() {
        Task task = taskWithPayload(null);

        TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper);

        assertEquals(CREATED_AT, response.createdAt());
    }

    @Test
    void toResponse_mapsUpdatedAtCorrectly() {
        Task task = taskWithPayload(null);

        TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper);

        assertEquals(UPDATED_AT, response.updatedAt());
    }

    // ── payload conversion branches ──────────────────────────────────────────

    @Test
    void toResponse_withNullPayload_payloadIsNull() {
        Task task = taskWithPayload(null);

        TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper);

        assertNull(response.payload());
    }

    @Test
    void toResponse_withValidJsonPayload_payloadIsJsonObject() {
        Task task = taskWithPayload(VALID_PAYLOAD_JSON);

        TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper);

        JsonNode payload = response.payload();
        assertNotNull(payload);
        assertTrue(payload.isObject());
    }

    @Test
    void toResponse_withValidJsonPayload_payloadContainsExpectedFields() {
        Task task = taskWithPayload(VALID_PAYLOAD_JSON);

        TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper);

        assertEquals(ORDER_ID_VALUE, response.payload().get(ORDER_ID_FIELD).asText());
    }

    @Test
    void toResponse_withCorruptStoredJson_payloadFallsBackToTextNode() {
        Task task = taskWithPayload(CORRUPT_PAYLOAD);

        TaskResponse response = TaskDtoMapper.toResponse(task, objectMapper);

        JsonNode payload = response.payload();
        assertNotNull(payload);
        assertTrue(payload.isTextual(),
                "corrupt stored JSON must fall back to a text node, not throw");
        assertEquals(CORRUPT_PAYLOAD, payload.asText());
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private static Task taskWithPayload(String payload) {
        return Task.builder()
                .id(TASK_ID)
                .service(SERVICE)
                .name(NAME)
                .description(DESCRIPTION)
                .active(true)
                .destinationId(DestinationId.of(DESTINATION_ID))
                .eventName(EVENT_NAME)
                .payload(payload)
                .timeoutMs(TIMEOUT_MS)
                .supportsRetry(false)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .build();
    }
}
