package dev.kairos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.api.task.TaskHandler;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public final class Router {

    private static final String TASKS = "/api/v1/tasks";
    private static final String TASKS_BY_ID = "/api/v1/tasks/{id}";
    private static final String TASK_START = "/api/v1/tasks/{id}/start";
    private static final String TASK_STOP = "/api/v1/tasks/{id}/stop";

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
}
