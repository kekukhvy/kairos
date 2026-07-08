package dev.kairos.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.api.schedule.ScheduleHandler;
import dev.kairos.application.schedule.usecases.*;
import dev.kairos.domain.schedule.Schedule;
import dev.kairos.infrastructure.ObjectMapperFactory;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.ZoneOffset;

import static dev.kairos.api.ApiScheduleBuilder.*;
import static dev.kairos.api.ApiTaskBuilder.TASK_UUID;
import static dev.kairos.api.ApiTaskBuilder.liveTask;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end API tests for all seven schedule endpoints. Each test hits a real
 * Javalin instance backed by in-memory fakes — no database, no network beyond
 * localhost.
 *
 * <p>Coverage:
 * <ul>
 *   <li>POST /api/v1/tasks/{taskId}/schedules — 201 for each type, unknown
 *       task → 404, invalid type → 400, ONCE past date → 400, FIXED 0/over-max
 *       → 400, blank cron → 400, invalid timezone → 400</li>
 *   <li>GET /api/v1/tasks/{taskId}/schedules — 200 list, pagination,
 *       unknown task returns empty (not 404), malformed taskId → 400</li>
 *   <li>GET /api/v1/schedules/{id} — 200, unknown → 404, malformed → 400</li>
 *   <li>PUT /api/v1/schedules/{id} — 200, unknown → 404, validation → 400</li>
 *   <li>DELETE /api/v1/schedules/{id} — 204, unknown → 204 (idempotent)</li>
 *   <li>PATCH /api/v1/schedules/{id}/pause — 200, unknown → 404</li>
 *   <li>PATCH /api/v1/schedules/{id}/resume — 200, unknown → 404</li>
 *   <li>Full happy path: create → get → list → pause → resume → update → delete</li>
 * </ul>
 */
class ScheduleApiTest {

    // ── route constants ───────────────────────────────────────────────────────

    private static final String TASK_SCHEDULES_PATH = "/api/v1/tasks/%s/schedules";
    private static final String SCHEDULES_BY_ID_PATH = "/api/v1/schedules/%s";
    private static final String SCHEDULE_PAUSE_PATH = "/api/v1/schedules/%s/pause";
    private static final String SCHEDULE_RESUME_PATH = "/api/v1/schedules/%s/resume";
    private static final int PORT_RANDOM = 0;

    // ── HTTP status constants ─────────────────────────────────────────────────

    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_NOT_FOUND = 404;

    // ── response field name constants ─────────────────────────────────────────

    private static final String FIELD_ID = "id";
    private static final String FIELD_TASK_ID = "taskId";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_LABEL = "label";
    private static final String FIELD_RUN_AT = "runAt";
    private static final String FIELD_CRON_EXPRESSION = "cronExpression";
    private static final String FIELD_INTERVAL_SECONDS = "intervalSeconds";
    private static final String FIELD_TIMEZONE = "timezone";
    private static final String FIELD_ACTIVE = "active";
    private static final String FIELD_ITEMS = "items";
    private static final String FIELD_LIMIT = "limit";
    private static final String FIELD_OFFSET = "offset";
    private static final String FIELD_HAS_NEXT = "hasNext";
    private static final String FIELD_ERROR = "error";

    // ── pagination constants ──────────────────────────────────────────────────

    private static final int DEFAULT_LIMIT = 20;
    private static final int DEFAULT_OFFSET = 0;
    private static final int EXPLICIT_LIMIT = 1;
    private static final int EXPLICIT_OFFSET = 0;

    // ── test infrastructure ───────────────────────────────────────────────────

    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private ObjectMapper objectMapper;
    private InMemoryScheduleRepositoryForApi scheduleRepository;
    private InMemoryTaskRepositoryForApi taskRepository;
    private Javalin app;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void startApp() {
        objectMapper = ObjectMapperFactory.create();
        scheduleRepository = new InMemoryScheduleRepositoryForApi();
        taskRepository = new InMemoryTaskRepositoryForApi();
        taskRepository.seed(liveTask());   // seeds task with TASK_UUID / TASK_ID

        ScheduleHandler handler = new ScheduleHandler(
                new CreateScheduleUseCase(scheduleRepository, taskRepository, FIXED_CLOCK),
                new GetScheduleByIdUseCase(scheduleRepository),
                new ListSchedulesByTaskUseCase(scheduleRepository),
                new UpdateScheduleUseCase(scheduleRepository, FIXED_CLOCK),
                new DeleteScheduleUseCase(scheduleRepository),
                new SetScheduleActiveUseCase(scheduleRepository, FIXED_CLOCK)
        );

        app = Router.create(objectMapper);
        Router.registerScheduleRoutes(app, handler);
        app.start(PORT_RANDOM);

        httpClient = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + app.port();
    }

    @AfterEach
    void stopApp() {
        app.stop();
    }

    // ── POST /api/v1/tasks/{taskId}/schedules — ONCE ──────────────────────────

    @Test
    void create_onceWithValidRequest_returns201() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), validOnceBody());

        assertEquals(HTTP_CREATED, response.statusCode());
    }

    @Test
    void create_onceWithValidRequest_responseContainsGeneratedId() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), validOnceBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertFalse(body.get(FIELD_ID).isNull());
        assertDoesNotThrow(() -> java.util.UUID.fromString(body.get(FIELD_ID).asText()));
    }

    @Test
    void create_onceWithValidRequest_responseReflectsType() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), validOnceBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("ONCE", body.get(FIELD_TYPE).asText());
    }

    @Test
    void create_onceWithValidRequest_responseReflectsTaskId() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), validOnceBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(TASK_UUID.toString(), body.get(FIELD_TASK_ID).asText());
    }

    @Test
    void create_onceWithValidRequest_runAtIsPresent() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), validOnceBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertFalse(body.get(FIELD_RUN_AT).isNull());
    }

    @Test
    void create_onceWithValidRequest_activeIsTrue() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), validOnceBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.get(FIELD_ACTIVE).asBoolean());
    }

    // ── POST — CRON ───────────────────────────────────────────────────────────

    @Test
    void create_cronWithValidRequest_returns201() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), validCronBody());

        assertEquals(HTTP_CREATED, response.statusCode());
    }

    @Test
    void create_cronWithValidRequest_responseReflectsType() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), validCronBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("CRON", body.get(FIELD_TYPE).asText());
    }

    @Test
    void create_cronWithValidRequest_cronExpressionIsReturned() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), validCronBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(DEFAULT_CRON, body.get(FIELD_CRON_EXPRESSION).asText());
    }

    // ── POST — FIXED ──────────────────────────────────────────────────────────

    @Test
    void create_fixedWithValidRequest_returns201() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), validFixedBody());

        assertEquals(HTTP_CREATED, response.statusCode());
    }

    @Test
    void create_fixedWithValidRequest_responseReflectsType() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), validFixedBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("FIXED", body.get(FIELD_TYPE).asText());
    }

    @Test
    void create_fixedWithValidRequest_intervalSecondsIsReturned() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), validFixedBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(DEFAULT_INTERVAL_SECONDS, body.get(FIELD_INTERVAL_SECONDS).asInt());
    }

    // ── POST — 404 for unknown task ───────────────────────────────────────────

    @Test
    void create_withUnknownTaskId_returns404() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(UNKNOWN_TASK_UUID), validOnceBody());

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    @Test
    void create_withUnknownTaskId_responseContainsErrorField() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(UNKNOWN_TASK_UUID), validOnceBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertNotNull(body.get(FIELD_ERROR));
        assertFalse(body.get(FIELD_ERROR).asText().isBlank());
    }

    // ── POST — 400 for invalid type ───────────────────────────────────────────

    @Test
    void create_withUnknownType_returns400() throws Exception {
        String body = """
                {"type":"MONTHLY","runAt":"2027-01-01T08:00:00Z"}
                """;
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    // ── POST — 400 for ONCE with past runAt ───────────────────────────────────

    @Test
    void create_onceWithPastRunAt_returns400() throws Exception {
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), oncePastBody());

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void create_onceWithNullRunAt_returns400() throws Exception {
        String body = """
                {"type":"ONCE"}
                """;
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    // ── POST — 400 for FIXED boundary violations ──────────────────────────────

    @Test
    void create_fixedWithZeroInterval_returns400() throws Exception {
        String body = """
                {"type":"FIXED","intervalSeconds":0}
                """;
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void create_fixedWithNegativeInterval_returns400() throws Exception {
        String body = """
                {"type":"FIXED","intervalSeconds":-1}
                """;
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void create_fixedWithIntervalExceedingOneDay_returns400() throws Exception {
        String body = """
                {"type":"FIXED","intervalSeconds":86401}
                """;
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void create_fixedWithNullInterval_returns400() throws Exception {
        String body = """
                {"type":"FIXED"}
                """;
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    // ── POST — 400 for CRON validation ───────────────────────────────────────

    @Test
    void create_cronWithBlankExpression_returns400() throws Exception {
        String body = """
                {"type":"CRON","cronExpression":"   ","timezone":"UTC"}
                """;
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void create_cronWithNullExpression_returns400() throws Exception {
        String body = """
                {"type":"CRON","timezone":"UTC"}
                """;
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void create_cronWithInvalidTimezone_returns400() throws Exception {
        String body = """
                {"type":"CRON","cronExpression":"0 8 * * *","timezone":"Not/AZone"}
                """;
        HttpResponse<String> response = post(taskSchedulesPath(TASK_UUID.toString()), body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    // ── GET /api/v1/tasks/{taskId}/schedules ─────────────────────────────────

    @Test
    void listByTask_withNoSchedules_returnsEmptyItems() throws Exception {
        HttpResponse<String> response = get(taskSchedulesPath(TASK_UUID.toString()));

        assertEquals(HTTP_OK, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(0, body.get(FIELD_ITEMS).size());
    }

    @Test
    void listByTask_withOneSchedule_returnsOneItem() throws Exception {
        scheduleRepository.seed(onceSchedule());

        HttpResponse<String> response = get(taskSchedulesPath(TASK_UUID.toString()));

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(1, body.get(FIELD_ITEMS).size());
    }

    @Test
    void listByTask_defaultPagination_limitIsDefaultAndOffsetIsZero() throws Exception {
        HttpResponse<String> response = get(taskSchedulesPath(TASK_UUID.toString()));

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(DEFAULT_LIMIT, body.get(FIELD_LIMIT).asInt());
        assertEquals(DEFAULT_OFFSET, body.get(FIELD_OFFSET).asInt());
    }

    @Test
    void listByTask_itemsContainExpectedFields() throws Exception {
        scheduleRepository.seed(onceSchedule());

        HttpResponse<String> response = get(taskSchedulesPath(TASK_UUID.toString()));

        JsonNode firstItem = objectMapper.readTree(response.body()).get(FIELD_ITEMS).get(0);
        assertEquals(SCHEDULE_UUID.toString(), firstItem.get(FIELD_ID).asText());
        assertEquals(TASK_UUID.toString(), firstItem.get(FIELD_TASK_ID).asText());
        assertEquals("ONCE", firstItem.get(FIELD_TYPE).asText());
    }

    @Test
    void listByTask_hasNextIsFalse_whenItemsDoNotExceedLimit() throws Exception {
        scheduleRepository.seed(onceSchedule());

        HttpResponse<String> response = get(taskSchedulesPath(TASK_UUID.toString()));

        JsonNode body = objectMapper.readTree(response.body());
        assertFalse(body.get(FIELD_HAS_NEXT).asBoolean());
    }

    @Test
    void listByTask_hasNextIsTrue_whenMoreItemsExistThanPageLimit() throws Exception {
        scheduleRepository.seed(onceSchedule());
        scheduleRepository.seed(onceScheduleWithRandomId());

        String path = taskSchedulesPath(TASK_UUID.toString()) + "?limit=" + EXPLICIT_LIMIT + "&offset=" + EXPLICIT_OFFSET;
        HttpResponse<String> response = get(path);

        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.get(FIELD_HAS_NEXT).asBoolean());
    }

    @Test
    void listByTask_withMalformedTaskId_returns400() throws Exception {
        HttpResponse<String> response = get(taskSchedulesPath(MALFORMED_UUID));

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    // ── GET /api/v1/schedules/{id} ────────────────────────────────────────────

    @Test
    void getById_withExistingSchedule_returns200() throws Exception {
        scheduleRepository.seed(onceSchedule());

        HttpResponse<String> response = get(schedulePath(SCHEDULE_UUID.toString()));

        assertEquals(HTTP_OK, response.statusCode());
    }

    @Test
    void getById_withExistingSchedule_responseContainsCorrectId() throws Exception {
        scheduleRepository.seed(onceSchedule());

        HttpResponse<String> response = get(schedulePath(SCHEDULE_UUID.toString()));

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(SCHEDULE_UUID.toString(), body.get(FIELD_ID).asText());
    }

    @Test
    void getById_withExistingSchedule_responseContainsCorrectTaskId() throws Exception {
        scheduleRepository.seed(onceSchedule());

        HttpResponse<String> response = get(schedulePath(SCHEDULE_UUID.toString()));

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(TASK_UUID.toString(), body.get(FIELD_TASK_ID).asText());
    }

    @Test
    void getById_withUnknownId_returns404() throws Exception {
        HttpResponse<String> response = get(schedulePath(UNKNOWN_SCHEDULE_UUID));

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    @Test
    void getById_withUnknownId_responseContainsErrorField() throws Exception {
        HttpResponse<String> response = get(schedulePath(UNKNOWN_SCHEDULE_UUID));

        JsonNode body = objectMapper.readTree(response.body());
        assertNotNull(body.get(FIELD_ERROR));
        assertFalse(body.get(FIELD_ERROR).asText().isBlank());
    }

    @Test
    void getById_withMalformedId_returns400() throws Exception {
        HttpResponse<String> response = get(schedulePath(MALFORMED_UUID));

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    // ── PUT /api/v1/schedules/{id} ────────────────────────────────────────────

    @Test
    void update_onOnceSchedule_returns200() throws Exception {
        scheduleRepository.seed(onceSchedule());

        HttpResponse<String> response = put(schedulePath(SCHEDULE_UUID.toString()), validOnceUpdateBody());

        assertEquals(HTTP_OK, response.statusCode());
    }

    @Test
    void update_onOnceSchedule_responseReflectsNewLabel() throws Exception {
        scheduleRepository.seed(onceSchedule());

        HttpResponse<String> response = put(schedulePath(SCHEDULE_UUID.toString()), validOnceUpdateBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("updated-label", body.get(FIELD_LABEL).asText());
    }

    @Test
    void update_withUnknownId_returns404() throws Exception {
        HttpResponse<String> response = put(schedulePath(UNKNOWN_SCHEDULE_UUID), validOnceUpdateBody());

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    @Test
    void update_withMalformedId_returns400() throws Exception {
        HttpResponse<String> response = put(schedulePath(MALFORMED_UUID), validOnceUpdateBody());

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void update_onOnceSchedule_withPastRunAt_returns400() throws Exception {
        scheduleRepository.seed(onceSchedule());
        String body = """
                {"runAt":"2025-01-01T08:00:00Z","timezone":"UTC"}
                """;

        HttpResponse<String> response = put(schedulePath(SCHEDULE_UUID.toString()), body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void update_onFixedSchedule_withIntervalExceedingMax_returns400() throws Exception {
        scheduleRepository.seed(fixedSchedule());
        String body = """
                {"intervalSeconds":86401,"timezone":"UTC"}
                """;

        HttpResponse<String> response = put(schedulePath(SCHEDULE_UUID.toString()), body);

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    // ── DELETE /api/v1/schedules/{id} ─────────────────────────────────────────

    @Test
    void delete_withExistingSchedule_returns204() throws Exception {
        scheduleRepository.seed(onceSchedule());

        HttpResponse<String> response = delete(schedulePath(SCHEDULE_UUID.toString()));

        assertEquals(HTTP_NO_CONTENT, response.statusCode());
    }

    @Test
    void delete_withUnknownId_isIdempotentAndReturns204() throws Exception {
        HttpResponse<String> response = delete(schedulePath(UNKNOWN_SCHEDULE_UUID));

        assertEquals(HTTP_NO_CONTENT, response.statusCode());
    }

    @Test
    void delete_withMalformedId_returns400() throws Exception {
        HttpResponse<String> response = delete(schedulePath(MALFORMED_UUID));

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void delete_afterSuccessfulDelete_getByIdReturns404() throws Exception {
        scheduleRepository.seed(onceSchedule());
        delete(schedulePath(SCHEDULE_UUID.toString()));

        HttpResponse<String> response = get(schedulePath(SCHEDULE_UUID.toString()));

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    // ── PATCH /api/v1/schedules/{id}/pause ───────────────────────────────────

    @Test
    void pause_withExistingSchedule_returns200() throws Exception {
        scheduleRepository.seed(onceSchedule());

        HttpResponse<String> response = patch(pausePath(SCHEDULE_UUID.toString()));

        assertEquals(HTTP_OK, response.statusCode());
    }

    @Test
    void pause_withExistingSchedule_responseHasActiveFalse() throws Exception {
        scheduleRepository.seed(onceSchedule());

        HttpResponse<String> response = patch(pausePath(SCHEDULE_UUID.toString()));

        JsonNode body = objectMapper.readTree(response.body());
        assertFalse(body.get(FIELD_ACTIVE).asBoolean());
    }

    @Test
    void pause_withUnknownId_returns404() throws Exception {
        HttpResponse<String> response = patch(pausePath(UNKNOWN_SCHEDULE_UUID));

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    @Test
    void pause_withMalformedId_returns400() throws Exception {
        HttpResponse<String> response = patch(pausePath(MALFORMED_UUID));

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    // ── PATCH /api/v1/schedules/{id}/resume ──────────────────────────────────

    @Test
    void resume_withPausedSchedule_returns200() throws Exception {
        Schedule paused = onceSchedule();
        paused.pause(FIXED_NOW);
        scheduleRepository.seed(paused);

        HttpResponse<String> response = patch(resumePath(SCHEDULE_UUID.toString()));

        assertEquals(HTTP_OK, response.statusCode());
    }

    @Test
    void resume_withPausedSchedule_responseHasActiveTrue() throws Exception {
        Schedule paused = onceSchedule();
        paused.pause(FIXED_NOW);
        scheduleRepository.seed(paused);

        HttpResponse<String> response = patch(resumePath(SCHEDULE_UUID.toString()));

        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.get(FIELD_ACTIVE).asBoolean());
    }

    @Test
    void resume_withUnknownId_returns404() throws Exception {
        HttpResponse<String> response = patch(resumePath(UNKNOWN_SCHEDULE_UUID));

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    @Test
    void resume_withMalformedId_returns400() throws Exception {
        HttpResponse<String> response = patch(resumePath(MALFORMED_UUID));

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    // ── Full happy path ───────────────────────────────────────────────────────

    @Test
    void happyPath_create_get_list_pause_resume_update_delete() throws Exception {
        // create
        HttpResponse<String> createResponse =
                post(taskSchedulesPath(TASK_UUID.toString()), validOnceBody());
        assertEquals(HTTP_CREATED, createResponse.statusCode());
        String scheduleId = objectMapper.readTree(createResponse.body()).get(FIELD_ID).asText();

        // get
        HttpResponse<String> getResponse = get(schedulePath(scheduleId));
        assertEquals(HTTP_OK, getResponse.statusCode());
        assertEquals(scheduleId, objectMapper.readTree(getResponse.body()).get(FIELD_ID).asText());

        // list
        HttpResponse<String> listResponse = get(taskSchedulesPath(TASK_UUID.toString()));
        assertEquals(HTTP_OK, listResponse.statusCode());
        assertEquals(1, objectMapper.readTree(listResponse.body()).get(FIELD_ITEMS).size());

        // pause
        HttpResponse<String> pauseResponse = patch(pausePath(scheduleId));
        assertEquals(HTTP_OK, pauseResponse.statusCode());
        assertFalse(objectMapper.readTree(pauseResponse.body()).get(FIELD_ACTIVE).asBoolean());

        // resume
        HttpResponse<String> resumeResponse = patch(resumePath(scheduleId));
        assertEquals(HTTP_OK, resumeResponse.statusCode());
        assertTrue(objectMapper.readTree(resumeResponse.body()).get(FIELD_ACTIVE).asBoolean());

        // update
        HttpResponse<String> updateResponse = put(schedulePath(scheduleId), validOnceUpdateBody());
        assertEquals(HTTP_OK, updateResponse.statusCode());
        assertEquals("updated-label",
                objectMapper.readTree(updateResponse.body()).get(FIELD_LABEL).asText());

        // delete
        HttpResponse<String> deleteResponse = delete(schedulePath(scheduleId));
        assertEquals(HTTP_NO_CONTENT, deleteResponse.statusCode());

        // confirm gone
        HttpResponse<String> afterDeleteGet = get(schedulePath(scheduleId));
        assertEquals(HTTP_NOT_FOUND, afterDeleteGet.statusCode());
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .DELETE()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> patch(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // ── path helpers ──────────────────────────────────────────────────────────

    private static String taskSchedulesPath(String taskId) {
        return TASK_SCHEDULES_PATH.formatted(taskId);
    }

    private static String schedulePath(String scheduleId) {
        return SCHEDULES_BY_ID_PATH.formatted(scheduleId);
    }

    private static String pausePath(String scheduleId) {
        return SCHEDULE_PAUSE_PATH.formatted(scheduleId);
    }

    private static String resumePath(String scheduleId) {
        return SCHEDULE_RESUME_PATH.formatted(scheduleId);
    }

    // ── request body builders ─────────────────────────────────────────────────

    private static String validOnceBody() {
        return """
                {
                  "type": "ONCE",
                  "label": "%s",
                  "runAt": "2027-06-01T08:00:00Z",
                  "timezone": "UTC"
                }
                """.formatted(DEFAULT_LABEL);
    }

    private static String validCronBody() {
        return """
                {
                  "type": "CRON",
                  "label": "%s",
                  "cronExpression": "%s",
                  "timezone": "%s"
                }
                """.formatted(DEFAULT_LABEL, DEFAULT_CRON, DEFAULT_TIMEZONE);
    }

    private static String validFixedBody() {
        return """
                {
                  "type": "FIXED",
                  "label": "%s",
                  "intervalSeconds": %d,
                  "timezone": "UTC"
                }
                """.formatted(DEFAULT_LABEL, DEFAULT_INTERVAL_SECONDS);
    }

    private static String oncePastBody() {
        return """
                {
                  "type": "ONCE",
                  "runAt": "2025-01-01T08:00:00Z",
                  "timezone": "UTC"
                }
                """;
    }

    private static String validOnceUpdateBody() {
        return """
                {
                  "label": "updated-label",
                  "runAt": "2027-06-01T08:00:00Z",
                  "timezone": "UTC"
                }
                """;
    }
}
