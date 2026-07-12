package dev.kairos.api.schedule;

import dev.kairos.application.schedule.commands.CreateScheduleCommand;
import dev.kairos.application.schedule.commands.UpdateScheduleCommand;
import dev.kairos.application.schedule.usecases.*;
import dev.kairos.common.dto.PageResponse;
import dev.kairos.common.dto.schedule.CreateScheduleRequest;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import dev.kairos.common.dto.schedule.UpdateScheduleRequest;
import dev.kairos.common.pagination.Pagination;
import dev.kairos.domain.schedule.Schedule;
import dev.kairos.domain.schedule.ScheduleId;
import dev.kairos.domain.task.TaskId;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.List;
import java.util.Objects;

import static dev.kairos.api.schedule.ScheduleDtoMapper.toResponse;

/**
 * Javalin request handler for the schedule resource. Create/list are nested
 * under a task ({@code /api/v1/tasks/{taskId}/schedules}); get/update/delete/
 * pause/resume are flat by id ({@code /api/v1/schedules/{id}}).
 *
 * <p>Each method maps to one HTTP endpoint and delegates all domain logic to
 * the corresponding use case. Exception-to-status mapping is centralised in
 * {@link dev.kairos.api.GlobalExceptionHandler}.
 */
public final class ScheduleHandler {

    private static final String PATH_PARAM_TASK_ID = "taskId";
    private static final String PATH_PARAM_ID = "id";

    private final CreateScheduleUseCase createScheduleUseCase;
    private final GetScheduleByIdUseCase getScheduleByIdUseCase;
    private final ListSchedulesByTaskUseCase listSchedulesByTaskUseCase;
    private final UpdateScheduleUseCase updateScheduleUseCase;
    private final DeleteScheduleUseCase deleteScheduleUseCase;
    private final SetScheduleActiveUseCase setScheduleActiveUseCase;

    public ScheduleHandler(CreateScheduleUseCase createScheduleUseCase,
                           GetScheduleByIdUseCase getScheduleByIdUseCase,
                           ListSchedulesByTaskUseCase listSchedulesByTaskUseCase,
                           UpdateScheduleUseCase updateScheduleUseCase,
                           DeleteScheduleUseCase deleteScheduleUseCase,
                           SetScheduleActiveUseCase setScheduleActiveUseCase) {
        this.createScheduleUseCase = Objects.requireNonNull(createScheduleUseCase);
        this.getScheduleByIdUseCase = Objects.requireNonNull(getScheduleByIdUseCase);
        this.listSchedulesByTaskUseCase = Objects.requireNonNull(listSchedulesByTaskUseCase);
        this.updateScheduleUseCase = Objects.requireNonNull(updateScheduleUseCase);
        this.deleteScheduleUseCase = Objects.requireNonNull(deleteScheduleUseCase);
        this.setScheduleActiveUseCase = Objects.requireNonNull(setScheduleActiveUseCase);
    }

    /**
     * POST /api/v1/tasks/{taskId}/schedules → 201 + ScheduleResponse
     */
    public void create(Context ctx) {
        String taskId = ctx.pathParam(PATH_PARAM_TASK_ID);
        CreateScheduleRequest request = ctx.bodyAsClass(CreateScheduleRequest.class);

        CreateScheduleCommand command = new CreateScheduleCommand(
                taskId,
                request.type(),
                request.label(),
                request.runAt(),
                request.cronExpression(),
                request.intervalSeconds(),
                request.timezone());

        Schedule schedule = createScheduleUseCase.execute(command);
        ctx.status(HttpStatus.CREATED).json(toResponse(schedule));
    }

    /**
     * GET /api/v1/tasks/{taskId}/schedules?limit=20&offset=0 → 200 + PageResponse
     */
    public void listByTask(Context ctx) {
        TaskId taskId = TaskId.fromString(ctx.pathParam(PATH_PARAM_TASK_ID));
        Pagination pagination = Pagination.of(
                ctx.queryParamAsClass("limit", Integer.class).allowNullable().get(),
                ctx.queryParamAsClass("offset", Integer.class).allowNullable().get()
        );

        Pagination fetchPagination = Pagination.of(pagination.limit() + 1, pagination.offset());

        List<Schedule> schedules = listSchedulesByTaskUseCase.execute(taskId, fetchPagination);
        boolean hasNext = schedules.size() > pagination.limit();
        List<Schedule> pageItems = hasNext ? schedules.subList(0, pagination.limit()) : schedules;

        List<ScheduleResponse> items = pageItems.stream()
                .map(ScheduleDtoMapper::toResponse)
                .toList();

        ctx.status(HttpStatus.OK)
                .json(new PageResponse<>(items, pagination.limit(), pagination.offset(), hasNext));
    }

    /**
     * GET /api/v1/schedules/{id} → 200 + ScheduleResponse
     */
    public void getById(Context ctx) {
        ScheduleId id = ScheduleId.fromString(ctx.pathParam(PATH_PARAM_ID));
        Schedule schedule = getScheduleByIdUseCase.execute(id);
        ctx.status(HttpStatus.OK).json(toResponse(schedule));
    }

    /**
     * PUT /api/v1/schedules/{id} → 200 + ScheduleResponse
     */
    public void update(Context ctx) {
        ScheduleId id = ScheduleId.fromString(ctx.pathParam(PATH_PARAM_ID));
        UpdateScheduleRequest request = ctx.bodyAsClass(UpdateScheduleRequest.class);

        UpdateScheduleCommand command = new UpdateScheduleCommand(
                request.label(),
                request.runAt(),
                request.cronExpression(),
                request.intervalSeconds(),
                request.timezone());

        Schedule schedule = updateScheduleUseCase.execute(id, command);
        ctx.status(HttpStatus.OK).json(toResponse(schedule));
    }

    /**
     * DELETE /api/v1/schedules/{id} → 204
     */
    public void delete(Context ctx) {
        ScheduleId id = ScheduleId.fromString(ctx.pathParam(PATH_PARAM_ID));
        deleteScheduleUseCase.execute(id);
        ctx.status(HttpStatus.NO_CONTENT);
    }

    /**
     * PATCH /api/v1/schedules/{id}/pause → 200 + ScheduleResponse
     */
    public void pause(Context ctx) {
        ScheduleId id = ScheduleId.fromString(ctx.pathParam(PATH_PARAM_ID));
        Schedule schedule = setScheduleActiveUseCase.execute(id, false);
        ctx.status(HttpStatus.OK).json(toResponse(schedule));
    }

    /**
     * PATCH /api/v1/schedules/{id}/resume → 200 + ScheduleResponse
     */
    public void resume(Context ctx) {
        ScheduleId id = ScheduleId.fromString(ctx.pathParam(PATH_PARAM_ID));
        Schedule schedule = setScheduleActiveUseCase.execute(id, true);
        ctx.status(HttpStatus.OK).json(toResponse(schedule));
    }
}
