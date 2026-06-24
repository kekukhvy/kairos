package dev.kairos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.api.task.TaskHandler;
import dev.kairos.application.task.usecases.*;
import dev.kairos.infrastructure.ObjectMapperFactory;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.ZoneOffset;

import static dev.kairos.api.ApiTaskBuilder.DESTINATION_ID;

/**
 * Base for end-to-end API tests. Spins up a real {@link Javalin} instance on a
 * random free port before each test and tears it down afterwards. Every test
 * subclass gets a fresh in-memory repository and stub destination registry, so
 * tests are fully isolated from each other.
 *
 * <p>HTTP calls are made with Java's built-in {@link HttpClient} — no extra
 * test-only HTTP dependency is needed.
 */
abstract class JavalinApiTestBase {

    static final String BASE_PATH = "/api/v1/tasks";
    static final int PORT_RANDOM = 0;

    protected static final Clock FIXED_CLOCK =
            Clock.fixed(ApiTaskBuilder.CREATED_AT, ZoneOffset.UTC);

    protected ObjectMapper objectMapper;
    protected InMemoryTaskRepositoryForApi taskRepository;
    protected StubDestinationRepositoryForApi destinationRepository;
    protected Javalin app;
    protected HttpClient httpClient;
    protected String baseUrl;

    @BeforeEach
    void startApp() {
        objectMapper = ObjectMapperFactory.create();
        taskRepository = new InMemoryTaskRepositoryForApi();
        destinationRepository = new StubDestinationRepositoryForApi();
        destinationRepository.register(DESTINATION_ID);

        TaskHandler taskHandler = new TaskHandler(
                objectMapper,
                new CreateTaskUseCase(destinationRepository, taskRepository, FIXED_CLOCK),
                new UpdateTaskUseCase(taskRepository, destinationRepository, FIXED_CLOCK),
                new SoftDeleteTaskUseCase(taskRepository, FIXED_CLOCK),
                new GetTaskUseCase(taskRepository),
                new ListTasksUseCase(taskRepository)
        );

        app = Router.create(objectMapper);
        Router.registerTaskRoutes(app, taskHandler);
        app.start(PORT_RANDOM);

        httpClient = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + app.port();
    }

    @AfterEach
    void stopApp() {
        app.stop();
    }

    // ── HTTP helpers ─────────────────────────────────────────────────────────

    protected HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> put(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> delete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .DELETE()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected String taskPath(String id) {
        return BASE_PATH + "/" + id;
    }
}
