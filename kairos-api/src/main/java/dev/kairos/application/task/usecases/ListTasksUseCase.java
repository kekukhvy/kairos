package dev.kairos.application.task.usecases;

import dev.kairos.common.pagination.Pagination;
import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskRepository;

import java.util.List;
import java.util.Objects;

/**
 * Lists live (not soft-deleted) tasks, newest first, within the given
 * pagination window. Exclusion of deleted rows happens in the repository (SQL),
 * not here.
 */
public final class ListTasksUseCase {

    private final TaskRepository repository;

    public ListTasksUseCase(TaskRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public List<Task> execute(Pagination pagination) {
        Objects.requireNonNull(pagination);

        return this.repository.findAll(pagination.limit(), pagination.offset());
    }
}
