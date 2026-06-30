package dev.kairos.admin.feature.task;

import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.feature.task.dto.TaskPage;
import dev.kairos.admin.feature.task.dto.UpdateTaskRequest;
import dev.kairos.admin.shared.client.ApiProperties;
import dev.kairos.admin.shared.client.KairosApiClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private static final String START_COMMAND = "start";
    private static final String STOP_COMMAND = "stop";

    private final KairosApiClient client;
    private final ApiProperties apiProperties;

    public TaskService(KairosApiClient client, ApiProperties apiProperties) {
        this.client = client;
        this.apiProperties = apiProperties;
    }

    public List<TaskDto> list() {

        TaskPage page = client.rest()
                .get()
                .uri(apiProperties.taskEndpoint())
                .retrieve()
                .body(TaskPage.class);


        return page == null ? List.of() : page.items();
    }


    public TaskDto create(CreateTaskRequest request) {
        return client.rest()
                .post()
                .uri(apiProperties.taskEndpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TaskDto.class);
    }

    public TaskDto start(UUID id) {
        return setActive(id, START_COMMAND);
    }

    public TaskDto stop(UUID id) {
        return setActive(id, STOP_COMMAND);
    }

    private TaskDto setActive(UUID id, String action) {
        return client.rest()
                .post()
                .uri(apiProperties.taskEndpoint() + "/{id}/" + action, id)
                .retrieve()
                .body(TaskDto.class);
    }

    public void delete(UUID id) {
        client.rest()
                .delete()
                .uri(apiProperties.taskEndpoint() + "/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }

    public TaskDto update(UUID id, UpdateTaskRequest request) {
        return client.rest()
                .put()
                .uri(apiProperties.taskEndpoint() + "/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TaskDto.class);
    }
}