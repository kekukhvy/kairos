package dev.kairos.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.api.destination.DestinationHandler;
import dev.kairos.application.destination.usecases.*;
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

import static dev.kairos.api.ApiDestinationBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end API tests for all five destination endpoints. Each test hits a real
 * Javalin instance backed by in-memory fakes — no database, no network beyond
 * localhost.
 *
 * <p>Coverage:
 * <ul>
 *   <li>POST /api/v1/destinations — 201 happy path, duplicate id → 409,
 *       invalid destinationType → 400, malformed JSON → 400</li>
 *   <li>GET /api/v1/destinations/{id} — 200 happy path, unknown id → 404</li>
 *   <li>PUT /api/v1/destinations/{id} — 200 with updated config,
 *       unknown id → 404</li>
 *   <li>DELETE /api/v1/destinations/{id} — 204 happy path,
 *       unknown id → 204 (idempotent), destination in use → 409</li>
 *   <li>GET /api/v1/destinations — 200 PageResponse; hasNext=false when
 *       items ≤ limit; hasNext=true when more items exist than the page limit</li>
 * </ul>
 */
class DestinationApiTest {

    // ── route constants ───────────────────────────────────────────────────────

    private static final String BASE_PATH = "/api/v1/destinations";
    private static final int PORT_RANDOM = 0;

    // ── HTTP status constants ─────────────────────────────────────────────────

    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;
    private static final int HTTP_NO_CONTENT = 204;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_CONFLICT = 409;

    // ── response field name constants ─────────────────────────────────────────

    private static final String FIELD_DESTINATION_ID = "destinationId";
    private static final String FIELD_DESTINATION_TYPE = "destinationType";
    private static final String FIELD_CONFIG = "config";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_ITEMS = "items";
    private static final String FIELD_LIMIT = "limit";
    private static final String FIELD_OFFSET = "offset";
    private static final String FIELD_HAS_NEXT = "hasNext";
    private static final String FIELD_ERROR = "error";

    // ── pagination constants ──────────────────────────────────────────────────

    private static final int DEFAULT_LIMIT = 20;
    private static final int DEFAULT_OFFSET = 0;
    private static final int PAGE_LIMIT_ONE = 1;
    private static final int PAGE_OFFSET_ZERO = 0;

    // ── test fixtures ─────────────────────────────────────────────────────────

    private static final String INVALID_DESTINATION_TYPE = "INVALID_TYPE";
    private static final String TOPIC_KEY = "topic";
    private static final String TOPIC_VALUE = "payments";
    private static final String UPDATED_TOPIC_VALUE = "payments-v2";

    private static final Clock FIXED_CLOCK =
            Clock.fixed(CREATED_AT, ZoneOffset.UTC);

    // ── test infrastructure ───────────────────────────────────────────────────

    private ObjectMapper objectMapper;
    private InMemoryDestinationRepositoryForApi destinationRepository;
    private StubTaskRepositoryForApi taskRepository;
    private Javalin app;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void startApp() {
        objectMapper = ObjectMapperFactory.create();
        destinationRepository = new InMemoryDestinationRepositoryForApi();
        taskRepository = new StubTaskRepositoryForApi();

        DestinationHandler handler = new DestinationHandler(
                new CreateDestinationUseCase(destinationRepository, FIXED_CLOCK),
                new UpdateDestinationUseCase(destinationRepository),
                new DeleteDestinationUseCase(destinationRepository, taskRepository),
                new ListDestinationsUseCase(destinationRepository),
                new GetDestinationByIdUseCase(destinationRepository),
                objectMapper
        );

        app = Router.create(objectMapper);
        Router.registerDestinationRoutes(app, handler);
        app.start(PORT_RANDOM);

        httpClient = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + app.port();
    }

    @AfterEach
    void stopApp() {
        app.stop();
    }

    // ── POST /api/v1/destinations ─────────────────────────────────────────────

    @Test
    void create_withValidRequest_returns201() throws Exception {
        HttpResponse<String> response = post(BASE_PATH, validCreateBody());

        assertEquals(HTTP_CREATED, response.statusCode());
    }

    @Test
    void create_withValidRequest_responseContainsDestinationId() throws Exception {
        HttpResponse<String> response = post(BASE_PATH, validCreateBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(DESTINATION_ID, body.get(FIELD_DESTINATION_ID).asText());
    }

    @Test
    void create_withValidRequest_responseReflectsSubmittedType() throws Exception {
        HttpResponse<String> response = post(BASE_PATH, validCreateBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(DESTINATION_TYPE.name(), body.get(FIELD_DESTINATION_TYPE).asText());
    }

    @Test
    void create_withValidRequest_configIsEmbeddedAsJsonObject() throws Exception {
        HttpResponse<String> response = post(BASE_PATH, validCreateBody());

        JsonNode body = objectMapper.readTree(response.body());
        JsonNode config = body.get(FIELD_CONFIG);
        assertTrue(config.isObject(), "config must be a JSON object, not an escaped string");
        assertEquals(TOPIC_VALUE, config.get(TOPIC_KEY).asText());
    }

    @Test
    void create_withValidRequest_responseContainsCreatedAt() throws Exception {
        HttpResponse<String> response = post(BASE_PATH, validCreateBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertFalse(body.get(FIELD_CREATED_AT).isNull());
    }

    @Test
    void create_withDuplicateId_returns409() throws Exception {
        destinationRepository.seed(destination());

        HttpResponse<String> response = post(BASE_PATH, validCreateBody());

        assertEquals(HTTP_CONFLICT, response.statusCode());
    }

    @Test
    void create_withDuplicateId_responseContainsErrorField() throws Exception {
        destinationRepository.seed(destination());

        HttpResponse<String> response = post(BASE_PATH, validCreateBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertNotNull(body.get(FIELD_ERROR));
        assertFalse(body.get(FIELD_ERROR).asText().isBlank());
    }

    @Test
    void create_withInvalidDestinationType_returns400() throws Exception {
        HttpResponse<String> response = post(BASE_PATH, createBodyWithType(INVALID_DESTINATION_TYPE));

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    @Test
    void create_withInvalidDestinationType_responseContainsErrorField() throws Exception {
        HttpResponse<String> response = post(BASE_PATH, createBodyWithType(INVALID_DESTINATION_TYPE));

        JsonNode body = objectMapper.readTree(response.body());
        assertNotNull(body.get(FIELD_ERROR));
        assertFalse(body.get(FIELD_ERROR).asText().isBlank());
    }

    @Test
    void create_withMalformedJson_returns400() throws Exception {
        HttpResponse<String> response = post(BASE_PATH, "not-json");

        assertEquals(HTTP_BAD_REQUEST, response.statusCode());
    }

    // ── GET /api/v1/destinations/{id} ─────────────────────────────────────────

    @Test
    void getById_withExistingDestination_returns200() throws Exception {
        destinationRepository.seed(destination());

        HttpResponse<String> response = get(destinationPath(DESTINATION_ID));

        assertEquals(HTTP_OK, response.statusCode());
    }

    @Test
    void getById_withExistingDestination_responseContainsCorrectId() throws Exception {
        destinationRepository.seed(destination());

        HttpResponse<String> response = get(destinationPath(DESTINATION_ID));

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(DESTINATION_ID, body.get(FIELD_DESTINATION_ID).asText());
    }

    @Test
    void getById_withExistingDestination_configIsEmbeddedAsJsonObject() throws Exception {
        destinationRepository.seed(destination());

        HttpResponse<String> response = get(destinationPath(DESTINATION_ID));

        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.get(FIELD_CONFIG).isObject(),
                "config must be a JSON object, not an escaped string");
    }

    @Test
    void getById_withUnknownId_returns404() throws Exception {
        HttpResponse<String> response = get(destinationPath(UNKNOWN_DESTINATION_ID));

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    @Test
    void getById_withUnknownId_responseContainsErrorField() throws Exception {
        HttpResponse<String> response = get(destinationPath(UNKNOWN_DESTINATION_ID));

        JsonNode body = objectMapper.readTree(response.body());
        assertNotNull(body.get(FIELD_ERROR));
        assertFalse(body.get(FIELD_ERROR).asText().isBlank());
    }

    // ── PUT /api/v1/destinations/{id} ─────────────────────────────────────────

    @Test
    void update_withExistingDestination_returns200() throws Exception {
        destinationRepository.seed(destination());

        HttpResponse<String> response = put(destinationPath(DESTINATION_ID), validUpdateBody());

        assertEquals(HTTP_OK, response.statusCode());
    }

    @Test
    void update_withExistingDestination_responseReflectsUpdatedConfig() throws Exception {
        destinationRepository.seed(destination());

        HttpResponse<String> response = put(destinationPath(DESTINATION_ID), validUpdateBody());

        JsonNode body = objectMapper.readTree(response.body());
        JsonNode config = body.get(FIELD_CONFIG);
        assertTrue(config.isObject(), "updated config must be a JSON object");
        assertEquals(UPDATED_TOPIC_VALUE, config.get(TOPIC_KEY).asText());
    }

    @Test
    void update_withUnknownId_returns404() throws Exception {
        HttpResponse<String> response = put(destinationPath(UNKNOWN_DESTINATION_ID), validUpdateBody());

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    @Test
    void update_withUnknownId_responseContainsErrorField() throws Exception {
        HttpResponse<String> response = put(destinationPath(UNKNOWN_DESTINATION_ID), validUpdateBody());

        JsonNode body = objectMapper.readTree(response.body());
        assertNotNull(body.get(FIELD_ERROR));
        assertFalse(body.get(FIELD_ERROR).asText().isBlank());
    }

    // ── DELETE /api/v1/destinations/{id} ──────────────────────────────────────

    @Test
    void delete_withExistingDestination_returns204() throws Exception {
        destinationRepository.seed(destination());

        HttpResponse<String> response = delete(destinationPath(DESTINATION_ID));

        assertEquals(HTTP_NO_CONTENT, response.statusCode());
    }

    @Test
    void delete_withUnknownId_isIdempotentAndReturns204() throws Exception {
        HttpResponse<String> response = delete(destinationPath(UNKNOWN_DESTINATION_ID));

        assertEquals(HTTP_NO_CONTENT, response.statusCode());
    }

    @Test
    void delete_whenDestinationIsInUseByTask_returns409() throws Exception {
        destinationRepository.seed(destination());
        taskRepository.markDestinationInUse(DESTINATION_ID);

        HttpResponse<String> response = delete(destinationPath(DESTINATION_ID));

        assertEquals(HTTP_CONFLICT, response.statusCode());
    }

    @Test
    void delete_whenDestinationIsInUseByTask_responseContainsErrorField() throws Exception {
        destinationRepository.seed(destination());
        taskRepository.markDestinationInUse(DESTINATION_ID);

        HttpResponse<String> response = delete(destinationPath(DESTINATION_ID));

        JsonNode body = objectMapper.readTree(response.body());
        assertNotNull(body.get(FIELD_ERROR));
        assertFalse(body.get(FIELD_ERROR).asText().isBlank());
    }

    @Test
    void delete_afterSuccessfulDelete_getByIdReturns404() throws Exception {
        destinationRepository.seed(destination());
        delete(destinationPath(DESTINATION_ID));

        HttpResponse<String> response = get(destinationPath(DESTINATION_ID));

        assertEquals(HTTP_NOT_FOUND, response.statusCode());
    }

    // ── GET /api/v1/destinations (list) ───────────────────────────────────────

    @Test
    void list_withNoDestinations_returnsEmptyItems() throws Exception {
        HttpResponse<String> response = get(BASE_PATH);

        assertEquals(HTTP_OK, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(0, body.get(FIELD_ITEMS).size());
    }

    @Test
    void list_defaultPagination_limitIsDefaultAndOffsetIsZero() throws Exception {
        HttpResponse<String> response = get(BASE_PATH);

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(DEFAULT_LIMIT, body.get(FIELD_LIMIT).asInt());
        assertEquals(DEFAULT_OFFSET, body.get(FIELD_OFFSET).asInt());
    }

    @Test
    void list_withOneDestination_returnsOneItem() throws Exception {
        destinationRepository.seed(destination());

        HttpResponse<String> response = get(BASE_PATH);

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(1, body.get(FIELD_ITEMS).size());
    }

    @Test
    void list_itemsContainExpectedFields() throws Exception {
        destinationRepository.seed(destination());

        HttpResponse<String> response = get(BASE_PATH);

        JsonNode firstItem = objectMapper.readTree(response.body()).get(FIELD_ITEMS).get(0);
        assertEquals(DESTINATION_ID, firstItem.get(FIELD_DESTINATION_ID).asText());
        assertEquals(DESTINATION_TYPE.name(), firstItem.get(FIELD_DESTINATION_TYPE).asText());
    }

    @Test
    void list_hasNextIsFalse_whenItemsDoNotExceedLimit() throws Exception {
        destinationRepository.seed(destination());

        HttpResponse<String> response = get(BASE_PATH);

        JsonNode body = objectMapper.readTree(response.body());
        assertFalse(body.get(FIELD_HAS_NEXT).asBoolean(),
                "hasNext must be false when total items fit within one page");
    }

    @Test
    void list_hasNextIsTrue_whenMoreItemsExistThanPageLimit() throws Exception {
        destinationRepository.seed(destination());
        destinationRepository.seed(destinationWithId(DESTINATION_ID_2));

        HttpResponse<String> response = get(BASE_PATH + "?limit=" + PAGE_LIMIT_ONE + "&offset=" + PAGE_OFFSET_ZERO);

        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.get(FIELD_HAS_NEXT).asBoolean(),
                "hasNext must be true when more items exist beyond the current page");
    }

    @Test
    void list_withExplicitLimitOne_returnsOneItem() throws Exception {
        destinationRepository.seed(destination());
        destinationRepository.seed(destinationWithId(DESTINATION_ID_2));

        HttpResponse<String> response = get(BASE_PATH + "?limit=" + PAGE_LIMIT_ONE + "&offset=" + PAGE_OFFSET_ZERO);

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(PAGE_LIMIT_ONE, body.get(FIELD_ITEMS).size());
    }

    @Test
    void list_withExplicitLimitOne_paginationReflectedInResponse() throws Exception {
        destinationRepository.seed(destination());

        HttpResponse<String> response = get(BASE_PATH + "?limit=" + PAGE_LIMIT_ONE + "&offset=" + PAGE_OFFSET_ZERO);

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(PAGE_LIMIT_ONE, body.get(FIELD_LIMIT).asInt());
        assertEquals(PAGE_OFFSET_ZERO, body.get(FIELD_OFFSET).asInt());
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

    private static String destinationPath(String id) {
        return BASE_PATH + "/" + id;
    }

    // ── request body builders ─────────────────────────────────────────────────

    private static String validCreateBody() {
        return """
                {
                  "destinationId": "%s",
                  "destinationType": "%s",
                  "config": {"topic": "%s"}
                }
                """.formatted(DESTINATION_ID, DESTINATION_TYPE.name(), TOPIC_VALUE);
    }

    private static String createBodyWithType(String type) {
        return """
                {
                  "destinationId": "%s",
                  "destinationType": "%s",
                  "config": {"topic": "%s"}
                }
                """.formatted(DESTINATION_ID, type, TOPIC_VALUE);
    }

    private static String validUpdateBody() {
        return """
                {
                  "config": {"topic": "%s"}
                }
                """.formatted(UPDATED_TOPIC_VALUE);
    }
}
