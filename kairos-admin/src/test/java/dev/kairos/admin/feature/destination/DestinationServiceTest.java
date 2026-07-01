package dev.kairos.admin.feature.destination;

import dev.kairos.admin.feature.destination.dto.CreateDestinationRequest;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.feature.destination.dto.UpdateDestinationRequest;
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
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRequestConflict;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class DestinationServiceTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String TASK_ENDPOINT = "/api/v1/tasks";
    private static final String DESTINATION_ENDPOINT = "/api/v1/destinations";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private static final String DESTINATION_LIST_URL = BASE_URL + DESTINATION_ENDPOINT;

    private static final String DESTINATION_ID = "dest-kafka-1";
    private static final String DESTINATION_TYPE = "KAFKA";
    private static final String CREATED_AT = "2026-01-01T00:00:00Z";

    private static final String DESTINATION_JSON = """
            {
              "destinationId": "%s",
              "destinationType": "%s",
              "config": {"topic": "invoices"},
              "createdAt": "%s"
            }
            """.formatted(DESTINATION_ID, DESTINATION_TYPE, CREATED_AT);

    private static final String DESTINATION_PAGE_JSON = """
            {"items": [%s], "limit": 20, "offset": 0}
            """.formatted(DESTINATION_JSON);

    private static final String EMPTY_PAGE_JSON = """
            {"items": [], "limit": 20, "offset": 0}
            """;

    @Mock
    private KairosApiClient apiClient;

    private MockRestServiceServer mockServer;
    private DestinationService destinationService;

    @BeforeEach
    void setUp() {
        ApiProperties apiProperties = new ApiProperties(
                BASE_URL, TASK_ENDPOINT, DESTINATION_ENDPOINT, CONNECT_TIMEOUT, READ_TIMEOUT
        );
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.createServer(restClientBuilder);
        RestClient restClient = restClientBuilder.build();
        when(apiClient.rest()).thenReturn(restClient);
        destinationService = new DestinationService(apiClient, apiProperties);
    }

    // --- list() ---

    @Test
    void list_apiReturnsPage_returnsPageItems() {
        mockServer.expect(requestTo(DESTINATION_LIST_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(DESTINATION_PAGE_JSON, MediaType.APPLICATION_JSON));

        List<DestinationDTO> result = destinationService.list();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().destinationId()).isEqualTo(DESTINATION_ID);
        assertThat(result.getFirst().destinationType()).isEqualTo(DESTINATION_TYPE);
        assertThat(result.getFirst().createdAt()).isEqualTo(Instant.parse(CREATED_AT));
        mockServer.verify();
    }

    @Test
    void list_apiReturnsEmptyItems_returnsEmptyList() {
        mockServer.expect(requestTo(DESTINATION_LIST_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(EMPTY_PAGE_JSON, MediaType.APPLICATION_JSON));

        List<DestinationDTO> result = destinationService.list();

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void list_apiReturnsNullBody_returnsEmptyList() {
        mockServer.expect(requestTo(DESTINATION_LIST_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

        List<DestinationDTO> result = destinationService.list();

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    // --- create() ---

    @Test
    void create_sendsPostToDestinationEndpoint_returnsDeserializedDto() {
        CreateDestinationRequest request = new CreateDestinationRequest(
                DESTINATION_ID, DESTINATION_TYPE, java.util.Map.of("topic", "invoices")
        );

        mockServer.expect(requestTo(DESTINATION_LIST_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(DESTINATION_JSON, MediaType.APPLICATION_JSON));

        DestinationDTO result = destinationService.create(request);

        assertThat(result.destinationId()).isEqualTo(DESTINATION_ID);
        assertThat(result.destinationType()).isEqualTo(DESTINATION_TYPE);
        assertThat(result.createdAt()).isEqualTo(Instant.parse(CREATED_AT));
        mockServer.verify();
    }

    // --- update() ---

    @Test
    void update_sendsPutToDestinationEndpoint_returnsDeserializedDto() {
        String expectedUrl = BASE_URL + DESTINATION_ENDPOINT + "/" + DESTINATION_ID;
        UpdateDestinationRequest request = new UpdateDestinationRequest(java.util.Map.of("topic", "invoices"));

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(DESTINATION_JSON, MediaType.APPLICATION_JSON));

        DestinationDTO result = destinationService.update(DESTINATION_ID, request);

        assertThat(result.destinationId()).isEqualTo(DESTINATION_ID);
        assertThat(result.destinationType()).isEqualTo(DESTINATION_TYPE);
        assertThat(result.createdAt()).isEqualTo(Instant.parse(CREATED_AT));
        mockServer.verify();
    }

    @Test
    void update_differentDestinationId_targetsCorrectUrl() {
        String otherId = "dest-webhook-2";
        String expectedUrl = BASE_URL + DESTINATION_ENDPOINT + "/" + otherId;
        UpdateDestinationRequest request = new UpdateDestinationRequest(java.util.Map.of("url", "https://example.com"));
        String responseJson = """
                {
                  "destinationId": "%s",
                  "destinationType": "WEBHOOK",
                  "config": {"url": "https://example.com"},
                  "createdAt": "%s"
                }
                """.formatted(otherId, CREATED_AT);

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        DestinationDTO result = destinationService.update(otherId, request);

        assertThat(result.destinationId()).isEqualTo(otherId);
        mockServer.verify();
    }

    @Test
    void create_differentDestinationType_returnsDeserializedDto() {
        String webhookId = "dest-webhook-1";
        String webhookType = "WEBHOOK";
        String webhookJson = """
                {
                  "destinationId": "%s",
                  "destinationType": "%s",
                  "config": {"url": "https://example.com/hook"},
                  "createdAt": "%s"
                }
                """.formatted(webhookId, webhookType, CREATED_AT);

        CreateDestinationRequest request = new CreateDestinationRequest(
                webhookId, webhookType, java.util.Map.of("url", "https://example.com/hook")
        );

        mockServer.expect(requestTo(DESTINATION_LIST_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(webhookJson, MediaType.APPLICATION_JSON));

        DestinationDTO result = destinationService.create(request);

        assertThat(result.destinationId()).isEqualTo(webhookId);
        assertThat(result.destinationType()).isEqualTo(webhookType);
        assertThat(result.createdAt()).isEqualTo(Instant.parse(CREATED_AT));
        mockServer.verify();
    }

    // --- delete() ---

    @Test
    void delete_sendsDeleteToDestinationEndpointWithId_completesWithoutException() {
        String expectedUrl = BASE_URL + DESTINATION_ENDPOINT + "/" + DESTINATION_ID;

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        assertThatNoException().isThrownBy(() -> destinationService.delete(DESTINATION_ID));
        mockServer.verify();
    }

    @Test
    void delete_differentDestinationId_targetsCorrectUrl() {
        String otherId = "dest-webhook-2";
        String expectedUrl = BASE_URL + DESTINATION_ENDPOINT + "/" + otherId;

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        assertThatNoException().isThrownBy(() -> destinationService.delete(otherId));
        mockServer.verify();
    }

    @Test
    void delete_apiResponds409_propagatesRestClientResponseExceptionWithConflictStatus() {
        String expectedUrl = BASE_URL + DESTINATION_ENDPOINT + "/" + DESTINATION_ID;

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withRequestConflict());

        assertThatThrownBy(() -> destinationService.delete(DESTINATION_ID))
                .isInstanceOf(RestClientResponseException.class)
                .satisfies(ex -> assertThat(((RestClientResponseException) ex).getStatusCode().value())
                        .isEqualTo(CONFLICT.value()));
        mockServer.verify();
    }
}
