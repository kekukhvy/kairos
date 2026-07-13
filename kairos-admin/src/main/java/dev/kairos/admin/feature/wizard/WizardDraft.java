package dev.kairos.admin.feature.wizard;

import dev.kairos.admin.feature.destination.dto.CreateDestinationRequest;
import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.common.dto.schedule.CreateScheduleRequest;

import java.util.UUID;
import java.util.function.Function;

/**
 * Mutable holder for everything the {@code SetupWizard} collects across its
 * three steps, plus the ids/flags captured once each entity is actually
 * created.
 *
 * <p>The captured destination/task ids ({@link #destinationId} / {@link
 * #taskId}) start {@code null} and are written by {@link WizardCommit} only
 * after the corresponding POST succeeds; {@link #scheduleCreated} plays the
 * same role for the schedule, which the wizard always creates new (it has no
 * "select existing" mode — the wizard always creates a new task, so its
 * schedule is always newly created for that task). That is what lets a
 * repeated Finish (after a partial failure) skip whatever already succeeded
 * — see {@link WizardCommit}.
 *
 * <p>Field values are captured as request factories rather than raw request
 * objects because {@link CreateTaskRequest} needs the destination id, which is
 * only known once the Destination step has been resolved at commit time.
 */
public final class WizardDraft {

    private Function<String, CreateTaskRequest> taskRequestFactory;

    private WizardMode destinationMode;
    private CreateDestinationRequest newDestination;
    private String selectedDestinationId;

    private Function<UUID, CreateScheduleRequest> scheduleRequestFactory;
    private boolean scheduleCreated;

    private String destinationId;
    private UUID taskId;

    public String destinationId() {
        return destinationId;
    }

    public void setDestinationId(String destinationId) {
        this.destinationId = destinationId;
    }

    public UUID taskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public void setTaskRequestFactory(Function<String, CreateTaskRequest> taskRequestFactory) {
        this.taskRequestFactory = taskRequestFactory;
    }

    public Function<String, CreateTaskRequest> taskRequestFactory() {
        return taskRequestFactory;
    }

    public void selectExistingDestination(String destinationId) {
        this.destinationMode = WizardMode.SELECT_EXISTING;
        this.selectedDestinationId = destinationId;
    }

    public void createNewDestination(CreateDestinationRequest request) {
        this.destinationMode = WizardMode.CREATE_NEW;
        this.newDestination = request;
    }

    public WizardMode destinationMode() {
        return destinationMode;
    }

    public CreateDestinationRequest newDestination() {
        return newDestination;
    }

    public String selectedDestinationId() {
        return selectedDestinationId;
    }

    public void createNewSchedule(Function<UUID, CreateScheduleRequest> scheduleRequestFactory) {
        this.scheduleRequestFactory = scheduleRequestFactory;
    }

    public Function<UUID, CreateScheduleRequest> scheduleRequestFactory() {
        return scheduleRequestFactory;
    }

    /** {@code true} once {@link WizardCommit} has successfully created the schedule. */
    public boolean scheduleCreated() {
        return scheduleCreated;
    }

    /** Records that the schedule POST succeeded, so a repeated Finish does not duplicate it. */
    public void markScheduleCreated() {
        this.scheduleCreated = true;
    }
}
