package dev.kairos.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.UUID;

import static dev.kairos.api.ApiTaskBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end API tests for all five task endpoints. Each test hits a real
 * Javalin instance (started by {@link JavalinApiTestBase}) backed by
 * in-memory fakes — no database, no network beyond localhost.
 *
 * <p>Coverage:
 * <ul>
 *   <li>POST /api/v1/tasks — 201 happy path, nullable active/supportsRetry
 *       defaults, missing name → 400, timeoutMs ≤ 0 → 400,
 *       unknown destination → 400, payload round-trips as JSON node</li>
 *   <li>GET /api/v1/tasks/{id} — 200 happy path, deleted → 404,
 *       unknown id → 404, malformed UUID → 400</li>
 *   <li>PUT /api/v1/tasks/{id} — 200 full replacement, deleted → 404,
 *       unknown id → 404</li>
 *   <li>DELETE /api/v1/tasks/{id} — 204 happy path, already deleted → 409,
 *       unknown id → 404</li>
 *   <li>GET /api/v1/tasks — 200 list with items, default pagination,
 *       explicit limit/offset, deleted tasks excluded from list</li>
 * </ul>
 */
class TaskApiTest extends JavalinApiTestBase {

    // ── constants ─────────────────────────────────────────────────────────────

    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;

    private static final String FIELD_ID = "id";
    private static final String FIELD_SERVICE = "service";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_ACTIVE = "active";
    private static final String FIELD_DESTINATION_ID = "destinationId";
    private static final String FIELD_EVENT_NAME = "eventName";
    private static final String FIELD_PAYLOAD = "payload";
    private static final String FIELD_TIMEOUT_MS = "timeoutMs";
    private static final String FIELD_SUPPORTS_RETRY = "supportsRetry";
    private static final String FIELD_ITEMS = "items";
    private static final String FIELD_LIMIT = "limit";
    private static final String FIELD_OFFSET = "offset";
    private static final String FIELD_HAS_NEXT = "hasNext";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_ORDER_ID = "orderId";

    private static final String UPDATED_NAME = "updated-task";
    private static final String UPDATED_EVENT_NAME = "payment.updated.v1";
    private static final int UPDATED_TIMEOUT_MS = 10_000;

    private static final String PAYLOAD_ORDER_VALUE = "abc";
    private static final String UNKNOWN_UUID = "ffffffff-0000-0000-0000-000000000099";
    private static final String MALFORMED_UUID = "not-a-uuid";

    private static final int DEFAULT_LIMIT = 20;
    private static final int DEFAULT_OFFSET = 0;
    private static final int EXPLICIT_LIMIT = 1;
    private static final int EXPLICIT_OFFSET = 1;

    // ── POST /api/v1/tasks ───────────────────────────────────────────────────

    @Test
    void create_withValidRequest_returns201() throws Exception {
        HttpResponse<String> response = post(BASE_PATH, validCreateBody());

        assertEquals(HTTP_CREATED, response.statusCode());
    }

    @Test
    void create_withValidRequest_responseContainsGeneratedId() throws Exception {
        HttpResponse<String> response = post(BASE_PATH, validCreateBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertFalse(body.get(FIELD_ID).isNull());
        assertDoesNotThrow(() -> UUID.fromString(body.get(FIELD_ID).asText()));
    }

    @Test
    void create_withValidRequest_responseReflectsSubmittedFields() throws Exception {
        HttpResponse<String> response = post(BASE_PATH, validCreateBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(SERVICE, body.get(FIELD_SERVICE).asText());
        assertEquals(NAME, body.get(FIELD_NAME).asText());
        assertEquals(DESTINATION_ID, body.get(FIELD_DESTINATION_ID).asText());
        assertEquals(EVENT_NAME, body.get(FIELD_EVENT_NAME).asText());
        assertEquals(TIMEOUT_MS, body.get(FIELD_TIMEOUT_MS).asInt());
    }

    @Test
    void create_withNullActive_defaultsToTrue() throws Exception {
        String body = """
                {
                  "service": "%s",
                  "name": "%s",
                  "destinationId": "%s",
                  "eventName": "%s",
                  "timeoutMs": %d
                }
                """.formatted(SERVICE, NAME, DESTINATION_ID, EVENT_NAME, TIMEOUT_MS);

        HttpResponse<String> response = post(BASE_PATH, body);

        assertEquals(HTTP_CREATED, response.statusCode());
        JsonNode responseBody = objectMapper.readTree(response.body());
        assertTrue(responseBody.get(FIELD_ACTIVE).asBoolean());
    }

    @Test
    void create_withNullSupportsRetry_defaultsToFalse() throws Exception {
        String body = """
                {
                  "service": "%s",
                  "name": "%s",
                  "destinationId": "%s",
                  "eventName": "%s",
                  "timeoutMs": %d
                }
                """.formatted(SERVICE, NAME, DESTINATION_ID, EVENT_NAME, TIMEOUT_MS);

        HttpResponse<String> response = post(BASE_PATH, body);

        assertEquals(HTTP_CREATED, response.statusCode());
        JsonNode responseBody = objectMapper.readTree(response.body());
        assertFalse(responseBody.get(FIELD_SUPPORTS_RETRY).asBoolean());
    }

    @Test
    void create_withExplicitActiveFalse_persistsFalse() throws Exception {
        String body = """
                {
                  "service": "%s",
                  "name": "%s",
                  "destinationId": "%s",
                  "eventName": "%s",
                  "timeoutMs": %d,
                  "active": false
                }
                """.formatted(SERVICE, NAME, DESTINATION_ID, EVENT_NAME, TIMEOUT_MS);

        HttpResponse<String> response = post(BASE_PATH, body);

        assertEquals(HTTP_CREATED, response.statusCode());
        JsonNode responseBody = objectMapper.readTree(response.body());
        assertFalse(responseBody.get(FIELD_ACTIVE).asBoolean());
    }

    @Test
    void create_withExplicitSupportsRetryTrue_persistsTrue() throws Exception {
        String body = """
                {
                  "service": "%s",
                  "name": "%s",
                  "destinationId": "%s",
                  "eventName": "%s",
                  "timeoutMs": %d,
                  "supportsRetry": true
                }
                """.formatted(SERVICE, NAME, DESTINATION_ID, EVENT_NAME, TIMEOUT_MS);

        HttpResponse<String> response = post(BASE_PATH, body);

        assertEquals(HTTP_CREATED, response.statusCode());
        JsonNode responseBody = objectMapper.readTree(response.body());
        assertTrue(responseBody.get(FIELD_SUPPORTS_RETRY).asBoolean());
    }

    @Test
    void create_withJsonPayload_payloadRoundTripsAsJsonNode() throws Exception {
        String body = """
                {
                  "service": "%s",
                  "name": "%s",
                  "destinationId": "%s",
                  "eventName": "%s",
                  "timeoutMs": %d,
                  "payload": {"orderId": "abc"}
                }
                """.formatted(SERVICE, NAME, DESTINATION_ID, EVENT_NAME, TIMEOUT_MS);

        HttpResponse<String> response = post(BASE_PATH, body);

        assertEquals(HTTP_CREATED, response.statusCode());
        JsonNode responseBody = objectMapper.readTree(response.body());
        JsonNode payload = responseBody.get(FIELD_PAYLOAD);
        assertTrue(payload.isObject(), "payload must be a JSON object, not an escaped string");
        assertEquals(PAYLOAD_ORDER_VALUE, payload.get(FIELD_ORDER_ID).asText());
    }

    @Test
    void create_withMissingName_returns400() throws Exception {
        String body = """
                {
                  "service": "%s",
                  "destinationId": "%s",
                  "eventName": "%s",
                  "timeoutMs": %d
                }
                """.formatted(SERVICE, DESTINATION_ID, EVENT_NAME, TIMEOUT_MS);

        HttpResponse<String> response = post(BASE_PATH, body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void create_withZeroTimeoutMs_returns400() throws Exception {
        String body = """
                {
                  "service": "%s",
                  "name": "%s",
                  "destinationId": "%s",
                  "eventName": "%s",
                  "timeoutMs": 0
                }
                """.formatted(SERVICE, NAME, DESTINATION_ID, EVENT_NAME);

        HttpResponse<String> response = post(BASE_PATH, body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void create_withNegativeTimeoutMs_returns400() throws Exception {
        String body = """
                {
                  "service": "%s",
                  "name": "%s",
                  "destinationId": "%s",
                  "eventName": "%s",
                  "timeoutMs": -1
                }
                """.formatted(SERVICE, NAME, DESTINATION_ID, EVENT_NAME);

        HttpResponse<String> response = post(BASE_PATH, body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void create_withUnknownDestinationId_returns400() throws Exception {
        String body = """
                {
                  "service": "%s",
                  "name": "%s",
                  "destinationId": "does-not-exist",
                  "eventName": "%s",
                  "timeoutMs": %d
                }
                """.formatted(SERVICE, NAME, EVENT_NAME, TIMEOUT_MS);

        HttpResponse<String> response = post(BASE_PATH, body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void create_errorResponse_containsErrorField() throws Exception {
        String body = """
                {
                  "service": "%s",
                  "destinationId": "%s",
                  "eventName": "%s",
                  "timeoutMs": %d
                }
                """.formatted(SERVICE, DESTINATION_ID, EVENT_NAME, TIMEOUT_MS);

        HttpResponse<String> response = post(BASE_PATH, body);

        JsonNode responseBody = objectMapper.readTree(response.body());
        assertNotNull(responseBody.get(FIELD_ERROR));
        assertFalse(responseBody.get(FIELD_ERROR).asText().isBlank());
    }

    // ── GET /api/v1/tasks/{id} ────────────────────────────────────────────────

    @Test
    void getById_withLiveTask_returns200() throws Exception {
        taskRepository.seed(liveTask());

        HttpResponse<String> response = get(taskPath(TASK_UUID.toString()));

        assertEquals(HTTP_OK, response.statusCode());
    }

    @Test
    void getById_withLiveTask_responseContainsCorrectId() throws Exception {
        taskRepository.seed(liveTask());

        HttpResponse<String> response = get(taskPath(TASK_UUID.toString()));

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(TASK_UUID.toString(), body.get(FIELD_ID).asText());
    }

    @Test
    void getById_withLiveTask_payloadIsJsonNode() throws Exception {
        taskRepository.seed(liveTask());

        HttpResponse<String> response = get(taskPath(TASK_UUID.toString()));

        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.get(FIELD_PAYLOAD).isObject(),
                "payload must be embedded as a JSON object, not an escaped string");
    }

    @Test
    void getById_withUnknownId_returns404() throws Exception {
        HttpResponse<String> response = get(taskPath(UNKNOWN_UUID));

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    @Test
    void getById_withSoftDeletedTask_returns404() throws Exception {
        taskRepository.seed(deletedTask());

        HttpResponse<String> response = get(taskPath(TASK_UUID.toString()));

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    @Test
    void getById_withMalformedUuid_returns400() throws Exception {
        HttpResponse<String> response = get(taskPath(MALFORMED_UUID));

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    // ── PUT /api/v1/tasks/{id} ────────────────────────────────────────────────

    @Test
    void update_withLiveTask_returns200() throws Exception {
        taskRepository.seed(liveTask());

        HttpResponse<String> response = put(taskPath(TASK_UUID.toString()), validUpdateBody());

        assertEquals(HTTP_OK, response.statusCode());
    }

    @Test
    void update_withLiveTask_responseReflectsUpdatedFields() throws Exception {
        taskRepository.seed(liveTask());

        HttpResponse<String> response = put(taskPath(TASK_UUID.toString()), validUpdateBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(UPDATED_NAME, body.get(FIELD_NAME).asText());
        assertEquals(UPDATED_EVENT_NAME, body.get(FIELD_EVENT_NAME).asText());
        assertEquals(UPDATED_TIMEOUT_MS, body.get(FIELD_TIMEOUT_MS).asInt());
    }

    @Test
    void update_withSoftDeletedTask_returns404() throws Exception {
        taskRepository.seed(deletedTask());

        HttpResponse<String> response = put(taskPath(TASK_UUID.toString()), validUpdateBody());

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    @Test
    void update_withUnknownId_returns404() throws Exception {
        HttpResponse<String> response = put(taskPath(UNKNOWN_UUID), validUpdateBody());

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    @Test
    void update_withMalformedUuid_returns400() throws Exception {
        HttpResponse<String> response = put(taskPath(MALFORMED_UUID), validUpdateBody());

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    // ── DELETE /api/v1/tasks/{id} ─────────────────────────────────────────────

    @Test
    void delete_withLiveTask_returns204() throws Exception {
        taskRepository.seed(liveTask());

        HttpResponse<String> response = delete(taskPath(TASK_UUID.toString()));

        assertEquals(HTTP_NO_CONTENT, response.statusCode());
    }

    @Test
    void delete_withAlreadyDeletedTask_returns409() throws Exception {
        taskRepository.seed(deletedTask());

        HttpResponse<String> response = delete(taskPath(TASK_UUID.toString()));

        assertEquals(HTTP_CONFLICT, response.statusCode());
    }

    @Test
    void delete_withUnknownId_returns404() throws Exception {
        HttpResponse<String> response = delete(taskPath(UNKNOWN_UUID));

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    @Test
    void delete_withMalformedUuid_returns400() throws Exception {
        HttpResponse<String> response = delete(taskPath(MALFORMED_UUID));

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void delete_afterSuccessfulDelete_getByIdReturns404() throws Exception {
        taskRepository.seed(liveTask());
        delete(taskPath(TASK_UUID.toString()));

        HttpResponse<String> response = get(taskPath(TASK_UUID.toString()));

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    // ── GET /api/v1/tasks (list) ─────────────────────────────────────────────

    @Test
    void list_withNoTasks_returnsEmptyItems() throws Exception {
        HttpResponse<String> response = get(BASE_PATH);

        assertEquals(HTTP_OK, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(0, body.get(FIELD_ITEMS).size());
    }

    @Test
    void list_withOneLiveTask_returnsOneItem() throws Exception {
        taskRepository.seed(liveTask());

        HttpResponse<String> response = get(BASE_PATH);

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(1, body.get(FIELD_ITEMS).size());
    }

    @Test
    void list_defaultPagination_limitIsDefaultAndOffsetIsZero() throws Exception {
        HttpResponse<String> response = get(BASE_PATH);

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(DEFAULT_LIMIT, body.get(FIELD_LIMIT).asInt());
        assertEquals(DEFAULT_OFFSET, body.get(FIELD_OFFSET).asInt());
    }

    @Test
    void list_withExplicitLimitAndOffset_paginationReflectedInResponse() throws Exception {
        taskRepository.seed(liveTaskWithId(randomTaskId()));
        taskRepository.seed(liveTaskWithId(randomTaskId()));

        HttpResponse<String> response = get(BASE_PATH + "?limit=" + EXPLICIT_LIMIT + "&offset=" + EXPLICIT_OFFSET);

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(EXPLICIT_LIMIT, body.get(FIELD_LIMIT).asInt());
        assertEquals(EXPLICIT_OFFSET, body.get(FIELD_OFFSET).asInt());
        assertEquals(EXPLICIT_LIMIT, body.get(FIELD_ITEMS).size());
    }

    @Test
    void list_softDeletedTasksAreExcluded() throws Exception {
        taskRepository.seed(liveTaskWithId(randomTaskId()));
        taskRepository.seed(deletedTask());

        HttpResponse<String> response = get(BASE_PATH);

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(1, body.get(FIELD_ITEMS).size());
    }

    @Test
    void list_itemsContainExpectedFields() throws Exception {
        taskRepository.seed(liveTask());

        HttpResponse<String> response = get(BASE_PATH);

        JsonNode firstItem = objectMapper.readTree(response.body()).get(FIELD_ITEMS).get(0);
        assertEquals(TASK_UUID.toString(), firstItem.get(FIELD_ID).asText());
        assertEquals(NAME, firstItem.get(FIELD_NAME).asText());
        assertEquals(SERVICE, firstItem.get(FIELD_SERVICE).asText());
    }

    @Test
    void list_hasNextIsFalse_whenItemsDoNotExceedLimit() throws Exception {
        taskRepository.seed(liveTask());

        HttpResponse<String> response = get(BASE_PATH);

        JsonNode body = objectMapper.readTree(response.body());
        assertFalse(body.get(FIELD_HAS_NEXT).asBoolean(),
                "hasNext must be false when total items fit within one page");
    }

    @Test
    void list_hasNextIsTrue_whenMoreItemsExistThanPageLimit() throws Exception {
        taskRepository.seed(liveTask());
        taskRepository.seed(liveTaskWithId(randomTaskId()));

        HttpResponse<String> response = get(BASE_PATH + "?limit=" + EXPLICIT_LIMIT + "&offset=" + DEFAULT_OFFSET);

        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.get(FIELD_HAS_NEXT).asBoolean(),
                "hasNext must be true when more items exist beyond the current page");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String validCreateBody() {
        return """
                {
                  "service": "%s",
                  "name": "%s",
                  "description": "%s",
                  "destinationId": "%s",
                  "eventName": "%s",
                  "timeoutMs": %d
                }
                """.formatted(SERVICE, NAME, DESCRIPTION, DESTINATION_ID, EVENT_NAME, TIMEOUT_MS);
    }

    private static String validUpdateBody() {
        return """
                {
                  "name": "%s",
                  "destinationId": "%s",
                  "eventName": "%s",
                  "timeoutMs": %d
                }
                """.formatted(UPDATED_NAME, DESTINATION_ID, UPDATED_EVENT_NAME, UPDATED_TIMEOUT_MS);
    }
}
