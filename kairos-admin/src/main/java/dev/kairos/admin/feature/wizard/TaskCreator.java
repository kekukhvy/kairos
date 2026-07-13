package dev.kairos.admin.feature.wizard;

import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.task.dto.TaskDto;

/**
 * Narrow collaborator {@link WizardCommit} depends on instead of the full
 * {@code TaskService}, so unit tests can stub it with a lambda instead of
 * standing up a Spring bean.
 */
@FunctionalInterface
public interface TaskCreator {

    TaskDto create(CreateTaskRequest request);
}
