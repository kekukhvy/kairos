package dev.kairos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.api.destination.DestinationHandler;
import dev.kairos.api.task.TaskHandler;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public final class Router {

    private static final String TASKS = "/api/v1/tasks";
    private static final String TASKS_BY_ID = "/api/v1/tasks/{id}";
    private static final String TASK_START = "/api/v1/tasks/{id}/start";
    private static final String TASK_STOP = "/api/v1/tasks/{id}/stop";

    private static final String DESTINATIONS = "/api/v1/destinations";
    private static final String DESTINATIONS_BY_ID = "/api/v1/destinations/{id}";

    private Javalin javalin;

    private Router() {
    }

    public static Javalin create(ObjectMapper objectMapper) {
        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.jsonMapper(new JavalinJackson(objectMapper, true));
            javalinConfig.bundledPlugins.enableRouteOverview("/routes");
        });

        GlobalExceptionHandler.register(app);

        return app;
    }

    public static void registerTaskRoutes(Javalin app, TaskHandler taskHandler) {
        app.get(TASKS, taskHandler::list);
        app.post(TASKS, taskHandler::create);
        app.get(TASKS_BY_ID, taskHandler::getById);
        app.put(TASKS_BY_ID, taskHandler::update);
        app.delete(TASKS_BY_ID, taskHandler::delete);
        app.post(TASK_START, taskHandler::start);
        app.post(TASK_STOP, taskHandler::stop);
    }

    /**
     * Registers all {@code /api/v1/destinations} routes on the given Javalin app.
     *
     * @param app                the Javalin instance to register routes on
     * @param destinationHandler the handler wiring HTTP requests to destination use cases
     */
    public static void registerDestinationRoutes(Javalin app, DestinationHandler destinationHandler) {
        app.get(DESTINATIONS, destinationHandler::list);
        app.post(DESTINATIONS, destinationHandler::create);
        app.put(DESTINATIONS_BY_ID, destinationHandler::update);
        app.delete(DESTINATIONS_BY_ID, destinationHandler::delete);
        app.get(DESTINATIONS_BY_ID, destinationHandler::getById);
    }
}
