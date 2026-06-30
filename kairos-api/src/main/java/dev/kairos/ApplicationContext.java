package dev.kairos;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.api.Router;
import dev.kairos.api.task.TaskHandler;
import dev.kairos.application.task.usecases.*;
import dev.kairos.config.AppConfig;
import dev.kairos.config.DataSourceFactory;
import dev.kairos.config.DatabaseMigrator;
import dev.kairos.domain.destination.DestinationRepository;
import dev.kairos.domain.task.TaskRepository;
import dev.kairos.infrastructure.DSLContextFactory;
import dev.kairos.infrastructure.ObjectMapperFactory;
import dev.kairos.infrastructure.destination.JooqDestinationRepository;
import dev.kairos.infrastructure.task.JooqTaskRepository;
import io.javalin.Javalin;
import org.jooq.DSLContext;

import javax.sql.DataSource;
import java.time.Clock;

/**
 * Wires every layer together and owns the running {@link Javalin} instance.
 * Order: infrastructure → repositories → use cases → handlers → HTTP.
 */
final class ApplicationContext {

    private final Javalin app;
    private final int port;

    private ApplicationContext(Javalin app, int port) {
        this.app = app;
        this.port = port;
    }

    static ApplicationContext build(AppConfig config) {
        // ── Infrastructure ────────────────────────────────────────────────────
        DataSource dataSource = DataSourceFactory.getDataSource(config);
        DatabaseMigrator.migrate(dataSource, config);
        DSLContext dsl = DSLContextFactory.create(dataSource);
        ObjectMapper objectMapper = ObjectMapperFactory.create();
        Clock clock = Clock.systemUTC();

        // ── Repositories ──────────────────────────────────────────────────────
        TaskRepository taskRepository = new JooqTaskRepository(dsl);
        DestinationRepository destinationRepository = new JooqDestinationRepository(dsl);

        // ── Use cases ─────────────────────────────────────────────────────────
        TaskHandler taskHandler = new TaskHandler(
                objectMapper,
                new CreateTaskUseCase(destinationRepository, taskRepository, clock),
                new UpdateTaskUseCase(taskRepository, destinationRepository, clock),
                new SoftDeleteTaskUseCase(taskRepository, clock),
                new GetTaskUseCase(taskRepository),
                new ListTasksUseCase(taskRepository),
                new SetTaskActiveUseCase(taskRepository, clock)
        );

        // ── HTTP ──────────────────────────────────────────────────────────────
        Javalin app = Router.create(objectMapper);
        Router.registerTaskRoutes(app, taskHandler);

        return new ApplicationContext(app, config.getIntProperty("server.port", 8080));
    }

    void start() {
        app.start(port);
    }
}