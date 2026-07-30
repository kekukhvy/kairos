package dev.kairos.application.task.usecases;

import dev.kairos.domain.task.Task;
import dev.kairos.domain.task.TaskId;
import dev.kairos.domain.task.TaskNotFoundException;
import dev.kairos.domain.task.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Soft-deletes a task (stamps deleted_at). A missing task is reported as not
 * found (404); an already soft-deleted task is a conflict (409) — the
 * {@link Task#softDelete} guard raises {@code TaskAlreadyDeletedException}.
 */
public final class SoftDeleteTaskUseCase {

    private static final Logger logger = LoggerFactory.getLogger(SoftDeleteTaskUseCase.class);

    private final TaskRepository taskRepository;
    private final Clock clock;


    public SoftDeleteTaskUseCase(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = Objects.requireNonNull(taskRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public void execute (TaskId taskId) {

        Objects.requireNonNull(taskId, "taskId must not be null");

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        Instant now = clock.instant();
        task.softDelete(now); // already-deleted -> TaskAlreadyDeletedException (409)

        taskRepository.softDelete(taskId, now);

        logger.info("Task deleted: id='{}'", taskId.value());
    }
}
