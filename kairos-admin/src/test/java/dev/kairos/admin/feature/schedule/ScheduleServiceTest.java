package dev.kairos.admin.feature.schedule;

import dev.kairos.common.dto.schedule.CreateScheduleRequest;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import dev.kairos.common.dto.schedule.UpdateScheduleRequest;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String TASK_ENDPOINT = "/api/v1/tasks";
    private static final String DESTINATION_ENDPOINT = "/api/v1/destinations";
    private static final String SCHEDULE_ENDPOINT = "/api/v1/schedules";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private static final String SCHEDULES_PATH = "/%s/schedules";
    private static final String CREATE_URL_TEMPLATE = BASE_URL + TASK_ENDPOINT + SCHEDULES_PATH;
    private static final String LIST_PAGE_URL_TEMPLATE =
            BASE_URL + TASK_ENDPOINT + SCHEDULES_PATH + "?limit=100&offset=%d";
    private static final String BY_ID_URL_TEMPLATE = BASE_URL + SCHEDULE_ENDPOINT + "/%s";
    private static final String PAUSE_URL_TEMPLATE = BY_ID_URL_TEMPLATE + "/pause";
    private static final String RESUME_URL_TEMPLATE = BY_ID_URL_TEMPLATE + "/resume";

    private static final String SCHEDULE_JSON = """
            {
              "id": "%s",
              "taskId": "%s",
              "type": "CRON",
              "label": "nightly",
              "runAt": null,
              "cronExpression": "0 0 * * *",
              "intervalSeconds": null,
              "timezone": "UTC",
              "active": true,
              "createdAt": "2026-01-01T00:00:00Z",
              "updatedAt": "2026-01-01T00:00:00Z"
            }
            """;

    private static final String PAGE_JSON_TEMPLATE = """
            {"items": [%s], "limit": 100, "offset": 0, "hasNext": false}
            """;

    private static final String PAGE_JSON_TEMPLATE_HAS_NEXT = """
            {"items": [%s], "limit": 100, "offset": 0, "hasNext": true}
            """;

    @Mock
    private KairosApiClient apiClient;

    private MockRestServiceServer mockServer;
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        ApiProperties apiProperties = new ApiProperties(
                BASE_URL, TASK_ENDPOINT, DESTINATION_ENDPOINT, SCHEDULE_ENDPOINT, CONNECT_TIMEOUT, READ_TIMEOUT);
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.createServer(builder);
        RestClient restClient = builder.build();
        when(apiClient.rest()).thenReturn(restClient);
        scheduleService = new ScheduleService(apiClient, apiProperties);
    }

    @Test
    void listByTask_apiReturnsPage_returnsPageItems() {
        UUID taskId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        String pageJson = PAGE_JSON_TEMPLATE.formatted(SCHEDULE_JSON.formatted(scheduleId, taskId));

        mockServer.expect(requestTo(LIST_PAGE_URL_TEMPLATE.formatted(taskId, 0)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(pageJson, MediaType.APPLICATION_JSON));

        List<ScheduleResponse> result = scheduleService.listByTask(taskId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(scheduleId);
        mockServer.verify();
    }

    @Test
    void listByTask_hasNext_followsPagesUntilExhausted() {
        UUID taskId = UUID.randomUUID();
        UUID scheduleA = UUID.randomUUID();
        UUID scheduleB = UUID.randomUUID();
        String firstPage = PAGE_JSON_TEMPLATE_HAS_NEXT.formatted(SCHEDULE_JSON.formatted(scheduleA, taskId));
        String secondPage = PAGE_JSON_TEMPLATE.formatted(SCHEDULE_JSON.formatted(scheduleB, taskId));

        mockServer.expect(requestTo(LIST_PAGE_URL_TEMPLATE.formatted(taskId, 0)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(firstPage, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(LIST_PAGE_URL_TEMPLATE.formatted(taskId, 100)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(secondPage, MediaType.APPLICATION_JSON));

        List<ScheduleResponse> result = scheduleService.listByTask(taskId);

        assertThat(result).extracting(ScheduleResponse::id).containsExactly(scheduleA, scheduleB);
        mockServer.verify();
    }

    @Test
    void listForTasks_fansOutPerTask_concatenatesResults() {
        UUID taskA = UUID.randomUUID();
        UUID taskB = UUID.randomUUID();
        UUID scheduleA = UUID.randomUUID();
        UUID scheduleB = UUID.randomUUID();

        mockServer.expect(requestTo(LIST_PAGE_URL_TEMPLATE.formatted(taskA, 0)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        PAGE_JSON_TEMPLATE.formatted(SCHEDULE_JSON.formatted(scheduleA, taskA)),
                        MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(LIST_PAGE_URL_TEMPLATE.formatted(taskB, 0)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        PAGE_JSON_TEMPLATE.formatted(SCHEDULE_JSON.formatted(scheduleB, taskB)),
                        MediaType.APPLICATION_JSON));

        List<ScheduleResponse> result = scheduleService.listForTasks(List.of(taskA, taskB));

        assertThat(result).extracting(ScheduleResponse::id).containsExactly(scheduleA, scheduleB);
        mockServer.verify();
    }

    @Test
    void listByTask_apiReturnsEmptyItems_returnsEmptyList() {
        UUID taskId = UUID.randomUUID();
        String emptyPageJson = """
                {"items": [], "limit": 100, "offset": 0, "hasNext": false}
                """;

        mockServer.expect(requestTo(LIST_PAGE_URL_TEMPLATE.formatted(taskId, 0)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(emptyPageJson, MediaType.APPLICATION_JSON));

        List<ScheduleResponse> result = scheduleService.listByTask(taskId);

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void create_sendsPostToNestedEndpoint() {
        UUID taskId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                "CRON", "nightly", null, "0 0 * * *", null, "UTC");

        mockServer.expect(requestTo(CREATE_URL_TEMPLATE.formatted(taskId)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(SCHEDULE_JSON.formatted(scheduleId, taskId), MediaType.APPLICATION_JSON));

        ScheduleResponse result = scheduleService.create(taskId, request);

        assertThat(result.id()).isEqualTo(scheduleId);
        mockServer.verify();
    }

    @Test
    void getById_sendsGetToScheduleEndpoint() {
        UUID taskId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        mockServer.expect(requestTo(BY_ID_URL_TEMPLATE.formatted(scheduleId)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(SCHEDULE_JSON.formatted(scheduleId, taskId), MediaType.APPLICATION_JSON));

        ScheduleResponse result = scheduleService.getById(scheduleId);

        assertThat(result.id()).isEqualTo(scheduleId);
        mockServer.verify();
    }

    @Test
    void update_sendsPutToScheduleEndpoint() {
        UUID taskId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        UpdateScheduleRequest request = new UpdateScheduleRequest(
                "nightly", null, "0 0 * * *", null, "UTC");

        mockServer.expect(requestTo(BY_ID_URL_TEMPLATE.formatted(scheduleId)))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(SCHEDULE_JSON.formatted(scheduleId, taskId), MediaType.APPLICATION_JSON));

        ScheduleResponse result = scheduleService.update(scheduleId, request);

        assertThat(result.id()).isEqualTo(scheduleId);
        mockServer.verify();
    }

    @Test
    void delete_sendsDeleteToScheduleEndpoint() {
        UUID scheduleId = UUID.randomUUID();

        mockServer.expect(requestTo(BY_ID_URL_TEMPLATE.formatted(scheduleId)))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        scheduleService.delete(scheduleId);

        mockServer.verify();
    }

    @Test
    void pause_sendsPatchToPauseEndpoint() {
        UUID taskId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        mockServer.expect(requestTo(PAUSE_URL_TEMPLATE.formatted(scheduleId)))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess(SCHEDULE_JSON.formatted(scheduleId, taskId), MediaType.APPLICATION_JSON));

        ScheduleResponse result = scheduleService.pause(scheduleId);

        assertThat(result.id()).isEqualTo(scheduleId);
        mockServer.verify();
    }

    @Test
    void resume_sendsPatchToResumeEndpoint() {
        UUID taskId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        mockServer.expect(requestTo(RESUME_URL_TEMPLATE.formatted(scheduleId)))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess(SCHEDULE_JSON.formatted(scheduleId, taskId), MediaType.APPLICATION_JSON));

        ScheduleResponse result = scheduleService.resume(scheduleId);

        assertThat(result.id()).isEqualTo(scheduleId);
        mockServer.verify();
    }
}
