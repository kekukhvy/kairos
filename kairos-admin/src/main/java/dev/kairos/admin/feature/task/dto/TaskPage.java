package dev.kairos.admin.feature.task.dto;

import java.util.List;

public record TaskPage(
        List<TaskDto> items,
        int limit,
        int offset
) {
}