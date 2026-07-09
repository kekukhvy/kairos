package dev.kairos.admin.feature.schedule;

import dev.kairos.admin.shared.client.ApiProperties;
import dev.kairos.admin.shared.client.KairosApiClient;
import dev.kairos.common.dto.PageResponse;
import dev.kairos.common.dto.schedule.CreateScheduleRequest;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import dev.kairos.common.dto.schedule.UpdateScheduleRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * REST client for the schedule resource. Create/list are nested under a task
 * ({@code {taskEndpoint}/{taskId}/schedules}); get/update/delete/pause/resume
 * are flat by id ({@code {scheduleEndpoint}/{id}}).
 */
@Service
public class ScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    private static final String SCHEDULES_SUFFIX = "/{taskId}/schedules";
    private static final String BY_ID = "/{id}";
    private static final String PAUSE_COMMAND = "/pause";
    private static final String RESUME_COMMAND = "/resume";

    private final KairosApiClient client;
    private final ApiProperties apiProperties;

    public ScheduleService(KairosApiClient client, ApiProperties apiProperties) {
        this.client = client;
        this.apiProperties = apiProperties;
    }

    /**
     * Returns all schedules belonging to the given task.
     * The endpoint is task-scoped ({@code {taskEndpoint}/{taskId}/schedules});
     * a {@code null} response body is treated as an empty list.
     *
     * @param taskId the task whose schedules to fetch
     * @return the task's schedules, never {@code null}
     */
    public List<ScheduleResponse> listByTask(UUID taskId) {
        logger.debug("Fetching schedules for task {}", taskId);

        PageResponse<ScheduleResponse> page = client.rest()
                .get()
                .uri(apiProperties.taskEndpoint() + SCHEDULES_SUFFIX, taskId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (page == null) {
            logger.warn("Schedule list response body is null — returning empty list");
            return List.of();
        }

        logger.debug("Fetched {} schedule(s) for task {}", page.items().size(), taskId);
        return page.items();
    }

    /**
     * Returns the combined schedules of every given task. Since the list API is
     * task-scoped, this fans out one request per task and concatenates the
     * results, preserving task order.
     *
     * @param taskIds the tasks whose schedules to fetch
     * @return all schedules across the given tasks, never {@code null}
     */
    public List<ScheduleResponse> listForTasks(Collection<UUID> taskIds) {
        List<ScheduleResponse> all = new ArrayList<>();
        for (UUID taskId : taskIds) {
            all.addAll(listByTask(taskId));
        }
        logger.debug("Fetched {} schedule(s) across {} task(s)", all.size(), taskIds.size());
        return all;
    }

    /**
     * Creates a new schedule under the given task.
     *
     * @param taskId  the task to attach the schedule to
     * @param request the schedule definition; only the "when" field matching the type should be set
     * @return the created schedule as returned by the API
     */
    public ScheduleResponse create(UUID taskId, CreateScheduleRequest request) {
        logger.debug("Creating schedule of type {} for task {}", request.type(), taskId);

        return client.rest()
                .post()
                .uri(apiProperties.taskEndpoint() + SCHEDULES_SUFFIX, taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ScheduleResponse.class);
    }

    /**
     * Fetches a single schedule by its id, using the flat schedule endpoint.
     *
     * @param id the schedule id
     * @return the schedule, or {@code null} if the API returns an empty body
     */
    public ScheduleResponse getById(UUID id) {
        logger.debug("Fetching schedule {}", id);

        return client.rest()
                .get()
                .uri(apiProperties.scheduleEndpoint() + BY_ID, id)
                .retrieve()
                .body(ScheduleResponse.class);
    }

    /**
     * Updates the mutable fields of a schedule. Schedule type is immutable and
     * is not accepted by this endpoint; to change type the caller must delete
     * and recreate the schedule.
     *
     * @param id      the schedule id
     * @param request the fields to update
     * @return the updated schedule as returned by the API
     */
    public ScheduleResponse update(UUID id, UpdateScheduleRequest request) {
        logger.debug("Updating schedule {}", id);

        return client.rest()
                .put()
                .uri(apiProperties.scheduleEndpoint() + BY_ID, id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ScheduleResponse.class);
    }

    /**
     * Permanently removes a schedule. This action cannot be undone.
     *
     * @param id the schedule id to delete
     */
    public void delete(UUID id) {
        logger.debug("Deleting schedule {}", id);

        client.rest()
                .delete()
                .uri(apiProperties.scheduleEndpoint() + BY_ID, id)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Pauses a schedule, preventing it from triggering further executions until resumed.
     *
     * @param id the schedule id
     * @return the updated schedule with {@code active} set to {@code false}
     */
    public ScheduleResponse pause(UUID id) {
        return setActive(id, PAUSE_COMMAND);
    }

    /**
     * Resumes a paused schedule, allowing it to trigger executions again.
     *
     * @param id the schedule id
     * @return the updated schedule with {@code active} set to {@code true}
     */
    public ScheduleResponse resume(UUID id) {
        return setActive(id, RESUME_COMMAND);
    }

    private ScheduleResponse setActive(UUID id, String command) {
        logger.debug("Applying command {} to schedule {}", command, id);

        return client.rest()
                .patch()
                .uri(apiProperties.scheduleEndpoint() + BY_ID + command, id)
                .retrieve()
                .body(ScheduleResponse.class);
    }
}
