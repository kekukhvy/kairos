package dev.kairos.admin.feature.wizard;

import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.task.dto.TaskDto;

/**
 * Pure orchestration of the Finish commit sequence, resolving the dependency
 * chain in the order the API requires it: destination → task → schedule
 * (even though the wizard displays its steps in Task → Destination →
 * Schedule order). Depends only on the three narrow creator interfaces, so it
 * can be unit-tested with hand-written stubs — no Vaadin UI, no Spring.
 *
 * <p>Each captured id/flag on {@link WizardDraft} — {@code destinationId},
 * {@code taskId}, {@code scheduleCreated} — is written only after its POST
 * succeeds. A thrown exception therefore leaves earlier ids populated, and
 * the guards below mean a repeated {@link #execute} call skips whatever a
 * prior attempt already created and retries only the failed/remaining step.
 * The schedule step is always create-new (the wizard always creates a new
 * task, so its schedule is always newly created for that task).
 */
public final class WizardCommit {

    private final DestinationCreator destinationCreator;
    private final TaskCreator taskCreator;
    private final ScheduleCreator scheduleCreator;

    public WizardCommit(DestinationCreator destinationCreator, TaskCreator taskCreator,
                        ScheduleCreator scheduleCreator) {
        this.destinationCreator = destinationCreator;
        this.taskCreator = taskCreator;
        this.scheduleCreator = scheduleCreator;
    }

    /**
     * Runs the commit sequence against {@code draft}, mutating its captured
     * ids as each step succeeds. Propagates the first failure to the caller
     * (the dialog surfaces it via {@code Notifications.error} and stays open).
     *
     * @param draft the collected wizard input and any ids already captured by a prior attempt
     */
    public void execute(WizardDraft draft) {
        resolveDestination(draft);
        resolveTask(draft);
        resolveSchedule(draft);
    }

    private void resolveDestination(WizardDraft draft) {
        if (draft.destinationId() != null) {
            return;
        }
        if (draft.destinationMode() == WizardMode.SELECT_EXISTING) {
            draft.setDestinationId(draft.selectedDestinationId());
            return;
        }
        DestinationDTO created = destinationCreator.create(draft.newDestination());
        draft.setDestinationId(created.destinationId());
    }

    private void resolveTask(WizardDraft draft) {
        if (draft.taskId() != null) {
            return;
        }
        CreateTaskRequest request = draft.taskRequestFactory().apply(draft.destinationId());
        TaskDto created = taskCreator.create(request);
        draft.setTaskId(created.id());
    }

    private void resolveSchedule(WizardDraft draft) {
        if (draft.scheduleCreated()) {
            return;
        }
        scheduleCreator.create(draft.taskId(), draft.scheduleRequestFactory().apply(draft.taskId()));
        draft.markScheduleCreated();
    }
}
