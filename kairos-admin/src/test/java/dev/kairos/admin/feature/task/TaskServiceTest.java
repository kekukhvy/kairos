package dev.kairos.admin.feature.task;

import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.feature.task.dto.TaskPage;
import dev.kairos.admin.feature.task.dto.UpdateTaskRequest;
import dev.kairos.admin.shared.client.ApiProperties;
import dev.kairos.admin.shared.client.KairosApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String TASK_ENDPOINT = "/api/v1/tasks";
    private static final String DESTINATION_ENDPOINT = "/api/v1/destinations";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private static final String TASK_LIST_URL = BASE_URL + TASK_ENDPOINT;
    private static final String START_URL_TEMPLATE = BASE_URL + TASK_ENDPOINT + "/%s/start";
    private static final String STOP_URL_TEMPLATE = BASE_URL + TASK_ENDPOINT + "/%s/stop";
    private static final String TASK_BY_ID_URL_TEMPLATE = BASE_URL + TASK_ENDPOINT + "/%s";

    private static final String TASK_JSON = """
            {
              "id": "%s",
              "service": "billing",
              "name": "monthly-invoice",
              "description": null,
              "active": true,
              "destinationId": "dest-1",
              "eventName": "InvoiceReady",
              "payload": null,
              "timeoutMs": 5000,
              "supportsRetry": false,
              "createdAt": "2026-01-01T00:00:00Z",
              "updatedAt": "2026-01-01T00:00:00Z"
            }
            """;

    private static final String TASK_PAGE_JSON_TEMPLATE = """
            {"items": [%s], "limit": 20, "offset": 0}
            """;

    @Mock
    private KairosApiClient apiClient;

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private TaskService taskService;
    private ApiProperties apiProperties;

    @BeforeEach
    void setUp() {
        apiProperties = new ApiProperties(BASE_URL, TASK_ENDPOINT, DESTINATION_ENDPOINT, CONNECT_TIMEOUT, READ_TIMEOUT);
        restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.createServer(restClientBuilder);
        RestClient restClient = restClientBuilder.build();
        when(apiClient.rest()).thenReturn(restClient);
        taskService = new TaskService(apiClient, apiProperties);
    }

    // --- list() ---

    @Test
    void list_apiReturnsPage_returnsPageItems() {
        UUID taskId = UUID.randomUUID();
        String pageJson = TASK_PAGE_JSON_TEMPLATE.formatted(TASK_JSON.formatted(taskId));

        mockServer.expect(requestTo(TASK_LIST_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(pageJson, MediaType.APPLICATION_JSON));

        List<TaskDto> result = taskService.list();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(taskId);
        mockServer.verify();
    }

    @Test
    void list_apiReturnsEmptyItems_returnsEmptyList() {
        String emptyPageJson = """
                {"items": [], "limit": 20, "offset": 0}
                """;

        mockServer.expect(requestTo(TASK_LIST_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(emptyPageJson, MediaType.APPLICATION_JSON));

        List<TaskDto> result = taskService.list();

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    // --- start() ---

    @Test
    void start_sendsPostToStartEndpoint() {
        UUID taskId = UUID.randomUUID();
        String expectedUrl = START_URL_TEMPLATE.formatted(taskId);

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(TASK_JSON.formatted(taskId), MediaType.APPLICATION_JSON));

        TaskDto result = taskService.start(taskId);

        assertThat(result.id()).isEqualTo(taskId);
        mockServer.verify();
    }

    // --- stop() ---

    @Test
    void stop_sendsPostToStopEndpoint() {
        UUID taskId = UUID.randomUUID();
        String expectedUrl = STOP_URL_TEMPLATE.formatted(taskId);

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(TASK_JSON.formatted(taskId), MediaType.APPLICATION_JSON));

        TaskDto result = taskService.stop(taskId);

        assertThat(result.id()).isEqualTo(taskId);
        mockServer.verify();
    }

    // --- delete() ---

    @Test
    void delete_sendsDeleteToTaskEndpoint() {
        UUID taskId = UUID.randomUUID();
        String expectedUrl = TASK_BY_ID_URL_TEMPLATE.formatted(taskId);

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        taskService.delete(taskId);

        mockServer.verify();
    }

    // --- update() ---

    @Test
    void update_sendsPutToTaskEndpoint() {
        UUID taskId = UUID.randomUUID();
        String expectedUrl = TASK_BY_ID_URL_TEMPLATE.formatted(taskId);
        UpdateTaskRequest request = new UpdateTaskRequest(
                "updated-name", null, true, "dest-1", "InvoiceReady", null, 5000, false
        );

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(TASK_JSON.formatted(taskId), MediaType.APPLICATION_JSON));

        TaskDto result = taskService.update(taskId, request);

        assertThat(result.id()).isEqualTo(taskId);
        mockServer.verify();
    }

    // --- create() ---

    @Test
    void create_sendsPostToTaskEndpoint() {
        UUID taskId = UUID.randomUUID();
        CreateTaskRequest request = new CreateTaskRequest(
                "billing", "monthly-invoice", null, true, "dest-1", "InvoiceReady", null, 5000, false
        );

        mockServer.expect(requestTo(TASK_LIST_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(TASK_JSON.formatted(taskId), MediaType.APPLICATION_JSON));

        TaskDto result = taskService.create(request);

        assertThat(result.id()).isEqualTo(taskId);
        mockServer.verify();
    }
}
