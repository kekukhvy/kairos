package dev.kairos;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.api.Router;
import dev.kairos.api.destination.DestinationHandler;
import dev.kairos.api.schedule.ScheduleHandler;
import dev.kairos.api.task.TaskHandler;
import dev.kairos.application.destination.usecases.*;
import dev.kairos.application.schedule.usecases.*;
import dev.kairos.application.task.usecases.*;
import dev.kairos.config.AppConfig;
import dev.kairos.config.DataSourceFactory;
import dev.kairos.config.DatabaseMigrator;
import dev.kairos.domain.destination.DestinationRepository;
import dev.kairos.domain.schedule.ScheduleRepository;
import dev.kairos.domain.task.TaskRepository;
import dev.kairos.infrastructure.DSLContextFactory;
import dev.kairos.infrastructure.ObjectMapperFactory;
import dev.kairos.infrastructure.destination.JooqDestinationRepository;
import dev.kairos.infrastructure.schedule.JooqScheduleRepository;
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
        ScheduleRepository scheduleRepository = new JooqScheduleRepository(dsl);

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

        DestinationHandler destinationHandler = new DestinationHandler(
                new CreateDestinationUseCase(destinationRepository, clock),
                new UpdateDestinationUseCase(destinationRepository),
                new DeleteDestinationUseCase(destinationRepository, taskRepository),
                new ListDestinationsUseCase(destinationRepository),
                new GetDestinationByIdUseCase(destinationRepository),
                objectMapper
        );

        ScheduleHandler scheduleHandler = new ScheduleHandler(
                new CreateScheduleUseCase(scheduleRepository, taskRepository, clock),
                new GetScheduleByIdUseCase(scheduleRepository),
                new ListSchedulesByTaskUseCase(scheduleRepository),
                new UpdateScheduleUseCase(scheduleRepository, clock),
                new DeleteScheduleUseCase(scheduleRepository),
                new SetScheduleActiveUseCase(scheduleRepository, clock)
        );

        // ── HTTP ──────────────────────────────────────────────────────────────
        Javalin app = Router.create(objectMapper);
        Router.registerTaskRoutes(app, taskHandler);
        Router.registerDestinationRoutes(app, destinationHandler);
        Router.registerScheduleRoutes(app, scheduleHandler);

        return new ApplicationContext(app, config.getIntProperty("server.port", 8080));
    }

    void start() {
        app.start(port);
    }
}