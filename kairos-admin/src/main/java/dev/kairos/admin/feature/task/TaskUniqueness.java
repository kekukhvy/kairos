package dev.kairos.admin.feature.task;

import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.admin.shared.form.FieldValidation;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the set of {@code (service, name)} keys already taken by the
 * already-loaded task list, for the client-side duplicate check in
 * {@code TaskForm} and {@code TaskStep}. No extra query is made — the caller
 * passes in the same list it already loaded for the grid.
 */
public final class TaskUniqueness {

    private TaskUniqueness() {
    }

    /**
     * Keys for every task in {@code tasks} except {@code excludeId} (the task
     * currently being edited, if any). Pass {@code null} when there is no task
     * to exclude, e.g. the create form or the setup wizard.
     */
    public static Set<String> keysExcluding(List<TaskDto> tasks, UUID excludeId) {
        return tasks.stream()
                .filter(task -> !task.id().equals(excludeId))
                .map(task -> FieldValidation.serviceNameKey(task.service(), task.name()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
