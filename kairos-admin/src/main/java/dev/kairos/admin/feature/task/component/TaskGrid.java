package dev.kairos.admin.feature.task.component;

import com.vaadin.flow.component.grid.Grid;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.dto.TaskDto;

import java.util.List;

public class TaskGrid extends Grid<TaskDto> {

    public TaskGrid() {
        super(TaskDto.class, false);

        addColumn(TaskDto::service).setHeader(TaskText.COL_SERVICE).setAutoWidth(true);
        addColumn(TaskDto::name).setHeader(TaskText.COL_NAME).setAutoWidth(true);
        addColumn(TaskDto::destinationId).setHeader(TaskText.COL_DESTINATION).setAutoWidth(true);
        addColumn(TaskDto::messageType).setHeader(TaskText.COL_MESSAGE_TYPE).setAutoWidth(true);
        addColumn(TaskDto::active).setHeader(TaskText.COL_ACTIVE).setAutoWidth(true);
        addColumn(TaskDto::timeoutMs).setHeader(TaskText.COL_TIMEOUT).setAutoWidth(true);

        setSizeFull();
    }

    public void setTasks(List<TaskDto> tasks) {
        setItems(tasks);
    }

}