package dev.kairos.api;

import dev.kairos.common.dto.ErrorResponse;
import dev.kairos.domain.task.TaskAlreadyDeletedException;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers all exception → HTTP status mappings on the Javalin app. Keeping
 * this in one place means the mapping table is visible at a glance and each
 * handler stays free of try/catch boilerplate.
 *
 * <p>Status mapping:
 * <ul>
 *   <li>400 — {@link dev.kairos.common.exceptions.ValidationException}, {@link IllegalArgumentException}
 *             (malformed path param / bad UUID)</li>
 *   <li>404 — {@link dev.kairos.domain.task.TaskNotFoundException}</li>
 *   <li>409 — {@link TaskAlreadyDeletedException}</li>
 *   <li>500 — anything else (logged, opaque message to client)</li>
 * </ul>
 */
public final class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private GlobalExceptionHandler() {
    }

    public static void register(Javalin app) {
        app.exception(dev.kairos.common.exceptions.ValidationException.class, (e, ctx) ->
                ctx.status(400).json(new ErrorResponse(e.getMessage())));

        app.exception(IllegalArgumentException.class, (e, ctx) ->
                ctx.status(400).json(new ErrorResponse(e.getMessage())));

        app.exception(dev.kairos.domain.task.TaskNotFoundException.class, (e, ctx) ->
                ctx.status(404).json(new ErrorResponse(e.getMessage())));

        app.exception(TaskAlreadyDeletedException.class, (e, ctx) ->
                ctx.status(409).json(new ErrorResponse(e.getMessage())));

        app.exception(Exception.class, (e, ctx) -> {
            log.error("Unhandled exception on {} {}", ctx.method(), ctx.path(), e);
            ctx.status(500).json(new ErrorResponse("Internal server error"));
        });
    }
}