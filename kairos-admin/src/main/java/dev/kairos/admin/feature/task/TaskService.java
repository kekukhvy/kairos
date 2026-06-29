package dev.kairos.admin.feature.task;

import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.feature.task.dto.TaskPage;
import dev.kairos.admin.shared.client.ApiProperties;
import dev.kairos.admin.shared.client.KairosApiClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

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
}