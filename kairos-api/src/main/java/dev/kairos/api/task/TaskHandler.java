package dev.kairos.api.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.application.task.commands.CreateTaskCommand;
import dev.kairos.application.task.commands.UpdateTaskCommand;
import dev.kairos.application.task.usecases.*;
import dev.kairos.common.dto.PageResponse;
import dev.kairos.common.dto.task.CreateTaskRequest;
import dev.kairos.common.dto.task.TaskResponse;
import dev.kairos.common.dto.task.UpdateTaskRequest;
import dev.kairos.common.pagination.Pagination;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import io.javalin.http.Context;

import java.util.List;
import java.util.Objects;

import static dev.kairos.common.util.helpers.JsonConverter.jsonToString;

public final class TaskHandler {

    private final ObjectMapper objectMapper;
    private final CreateTaskUseCase createTaskUseCase;
    private final UpdateTaskUseCase updateTaskUseCase;
    private final SoftDeleteTaskUseCase softDeleteTaskUseCase;
    private final GetTaskUseCase getTaskUseCase;
    private final ListTasksUseCase listTasksUseCase;
    private final SetTaskActiveUseCase setTaskActiveUseCase;

    public TaskHandler(ObjectMapper objectMapper, CreateTaskUseCase createTaskUseCase, UpdateTaskUseCase updateTaskUseCase, SoftDeleteTaskUseCase softDeleteTaskUseCase, GetTaskUseCase getTaskUseCase, ListTasksUseCase listTasksUseCase, SetTaskActiveUseCase setTaskActiveUseCase) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
        this.createTaskUseCase = Objects.requireNonNull(createTaskUseCase, "createTaskUseCase cannot be null");
        this.updateTaskUseCase = Objects.requireNonNull(updateTaskUseCase, "updateTaskUseCase cannot be null");
        this.softDeleteTaskUseCase = Objects.requireNonNull(softDeleteTaskUseCase, "softDeleteTaskUseCase cannot be null");
        this.getTaskUseCase = Objects.requireNonNull(getTaskUseCase, "getTaskUseCase cannot be null");
        this.listTasksUseCase = Objects.requireNonNull(listTasksUseCase, "listTasksUseCase cannot be null");
        this.setTaskActiveUseCase = setTaskActiveUseCase;
    }

    /**
     * GET /api/v1/tasks?limit=20&offset=0 → 200 + PageResponse
     */
    public void list(Context ctx) {
        Pagination pagination = Pagination.of(
                ctx.queryParamAsClass("limit", Integer.class).allowNullable().get(),
                ctx.queryParamAsClass("offset", Integer.class).allowNullable().get()
        );

        List<TaskResponse> items = listTasksUseCase.execute(pagination)
                .stream()
                .map(task -> TaskDtoMapper.toResponse(task, objectMapper))
                .toList();

        ctx.json(new PageResponse<>(items, pagination.limit(), pagination.offset()));
    }


    /**
     * GET /api/v1/tasks/{id} → 200 + TaskResponse
     */
    public void getById(Context ctx) {
        TaskId id = TaskId.fromString(ctx.pathParam("id"));
        Task task = getTaskUseCase.execute(id);
        ctx.json(TaskDtoMapper.toResponse(task, objectMapper));
    }

    /**
     * POST /api/v1/tasks → 201 + TaskResponse
     */
    public void create(Context ctx) {
        CreateTaskRequest request = ctx.bodyAsClass(CreateTaskRequest.class);
        CreateTaskCommand command = new CreateTaskCommand(
                request.service(),
                request.name(),
                request.description(),
                request.active(),
                request.destinationId(),
                request.messageType(),
                jsonToString(request.payload(), objectMapper),
                request.timeoutMs(),
                request.supportsRetry()
        );

        Task task = createTaskUseCase.execute(command);
        ctx.status(201).json(TaskDtoMapper.toResponse(task, objectMapper));
    }

    /**
     * PUT /api/v1/tasks/{id} → 200 + TaskResponse
     */
    public void update(Context ctx) {

        TaskId id = TaskId.fromString(ctx.pathParam("id"));
        UpdateTaskRequest req = ctx.bodyAsClass(UpdateTaskRequest.class);

        UpdateTaskCommand command = new UpdateTaskCommand(
                req.name(),
                req.description(),
                req.active(),
                req.destinationId(),
                req.messageType(),
                jsonToString(req.payload(), objectMapper),
                req.timeoutMs(),
                req.supportsRetry()
        );

        Task task = updateTaskUseCase.execute(id, command);
        ctx.json(TaskDtoMapper.toResponse(task, objectMapper));
    }

    /**
     * DELETE /api/v1/tasks/{id} → 204
     */
    public void delete(Context ctx) {
        TaskId id = TaskId.fromString(ctx.pathParam("id"));
        softDeleteTaskUseCase.execute(id);
        ctx.status(204);
    }

    /**
     * POST /api/v1/tasks/{id}/start → 200 + TaskResponse
     */
    public void start(Context ctx) {
        TaskId id = TaskId.fromString(ctx.pathParam("id"));
        Task task = setTaskActiveUseCase.execute(id, true);
        ctx.json(TaskDtoMapper.toResponse(task, objectMapper));
    }

    /**
     * POST /api/v1/tasks/{id}/stop → 200 + TaskResponse
     */
    public void stop(Context ctx) {
        TaskId id = TaskId.fromString(ctx.pathParam("id"));
        Task task = setTaskActiveUseCase.execute(id, false);
        ctx.json(TaskDtoMapper.toResponse(task, objectMapper));
    }
}
