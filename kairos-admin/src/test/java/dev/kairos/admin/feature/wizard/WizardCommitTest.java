package dev.kairos.admin.feature.wizard;

import dev.kairos.admin.feature.destination.dto.CreateDestinationRequest;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.feature.task.dto.CreateTaskRequest;
import dev.kairos.admin.feature.task.dto.TaskDto;
import dev.kairos.common.dto.schedule.CreateScheduleRequest;
import dev.kairos.common.dto.schedule.ScheduleResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the pure Finish commit orchestration: dependency-order sequencing
 * (destination → task → schedule), substitution of resolved ids, and the
 * idempotent-retry behaviour that skips whatever a prior Finish attempt
 * already created. All three collaborators are hand-written stubs — no
 * Vaadin UI, no Spring, no Mockito.
 */
class WizardCommitTest {

    private static final String NEW_DESTINATION_ID = "kafka-dest-1";
    private static final String EXISTING_DESTINATION_ID = "kafka-dest-existing";
    private static final UUID CREATED_TASK_ID = UUID.randomUUID();
    private static final UUID EXISTING_TASK_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final List<CreateDestinationRequest> destinationCalls = new ArrayList<>();
    private final List<CreateTaskRequest> taskCalls = new ArrayList<>();
    private final List<UUID> scheduleTaskIdCalls = new ArrayList<>();

    // --- happy path: create-new destination, task, and schedule ---

    @Test
    void commit_createNewDestinationAndSchedule_createsInDependencyOrder() {
        WizardDraft draft = draftWithNewDestinationTaskAndSchedule();

        WizardCommit commit = new WizardCommit(destinationCreatorStub(), taskCreatorStub(), scheduleCreatorStub());
        commit.execute(draft);

        assertThat(destinationCalls).hasSize(1);
        assertThat(taskCalls).hasSize(1);
        assertThat(scheduleTaskIdCalls).containsExactly(CREATED_TASK_ID);
    }

    @Test
    void commit_createNewDestination_substitutesCreatedDestinationIdIntoTaskRequest() {
        WizardDraft draft = draftWithNewDestinationTaskAndSchedule();

        WizardCommit commit = new WizardCommit(destinationCreatorStub(), taskCreatorStub(), scheduleCreatorStub());
        commit.execute(draft);

        assertThat(taskCalls.getFirst().destinationId()).isEqualTo(NEW_DESTINATION_ID);
    }

    @Test
    void commit_createNewDestination_capturesCreatedDestinationIdInDraft() {
        WizardDraft draft = draftWithNewDestinationTaskAndSchedule();

        WizardCommit commit = new WizardCommit(destinationCreatorStub(), taskCreatorStub(), scheduleCreatorStub());
        commit.execute(draft);

        assertThat(draft.destinationId()).isEqualTo(NEW_DESTINATION_ID);
    }

    @Test
    void commit_createNewTask_capturesCreatedTaskIdInDraft() {
        WizardDraft draft = draftWithNewDestinationTaskAndSchedule();

        WizardCommit commit = new WizardCommit(destinationCreatorStub(), taskCreatorStub(), scheduleCreatorStub());
        commit.execute(draft);

        assertThat(draft.taskId()).isEqualTo(CREATED_TASK_ID);
    }

    @Test
    void commit_createNewSchedule_marksScheduleAsCreatedInDraft() {
        WizardDraft draft = draftWithNewDestinationTaskAndSchedule();

        WizardCommit commit = new WizardCommit(destinationCreatorStub(), taskCreatorStub(), scheduleCreatorStub());
        commit.execute(draft);

        assertThat(draft.scheduleCreated()).isTrue();
    }

    // --- select-existing destination: skips creation, uses selected id directly ---

    @Test
    void commit_selectExistingDestination_doesNotCreateDestination() {
        WizardDraft draft = new WizardDraft();
        draft.selectExistingDestination(EXISTING_DESTINATION_ID);
        draft.setTaskRequestFactory(this::taskRequest);
        draft.createNewSchedule(taskId -> scheduleRequest());

        WizardCommit commit = new WizardCommit(destinationCreatorStub(), taskCreatorStub(), scheduleCreatorStub());
        commit.execute(draft);

        assertThat(destinationCalls).isEmpty();
        assertThat(taskCalls.getFirst().destinationId()).isEqualTo(EXISTING_DESTINATION_ID);
    }

    // --- partial failure: stop, keep already-created, surface the error ---

    @Test
    void commit_taskCreationFails_leavesCreatedDestinationIdInDraft() {
        WizardDraft draft = draftWithNewDestinationTaskAndSchedule();

        TaskCreator failingTaskCreator = request -> {
            throw new RuntimeException("task API down");
        };
        WizardCommit commit = new WizardCommit(destinationCreatorStub(), failingTaskCreator, scheduleCreatorStub());

        assertThatThrownBy(() -> commit.execute(draft)).isInstanceOf(RuntimeException.class);
        assertThat(draft.destinationId()).isEqualTo(NEW_DESTINATION_ID);
        assertThat(draft.taskId()).isNull();
    }

    @Test
    void commit_destinationCreationFails_doesNotAttemptTaskCreation() {
        WizardDraft draft = draftWithNewDestinationTaskAndSchedule();

        DestinationCreator failingDestinationCreator = request -> {
            throw new RuntimeException("destination API down");
        };
        WizardCommit commit = new WizardCommit(failingDestinationCreator, taskCreatorStub(), scheduleCreatorStub());

        assertThatThrownBy(() -> commit.execute(draft)).isInstanceOf(RuntimeException.class);
        assertThat(taskCalls).isEmpty();
    }

    // --- idempotent retry: a repeated Finish must not duplicate what already succeeded ---

    @Test
    void commit_retryAfterDestinationAlreadyCreated_doesNotRecreateDestination() {
        WizardDraft draft = draftWithNewDestinationTaskAndSchedule();
        draft.setDestinationId(NEW_DESTINATION_ID); // simulates a prior successful Finish attempt

        WizardCommit commit = new WizardCommit(destinationCreatorStub(), taskCreatorStub(), scheduleCreatorStub());
        commit.execute(draft);

        assertThat(destinationCalls).isEmpty();
        assertThat(taskCalls).hasSize(1);
    }

    @Test
    void commit_retryAfterTaskAlreadyCreated_doesNotRecreateTaskOrDestination() {
        WizardDraft draft = draftWithNewDestinationTaskAndSchedule();
        draft.setDestinationId(NEW_DESTINATION_ID);
        draft.setTaskId(EXISTING_TASK_ID); // simulates destination + task both already created

        WizardCommit commit = new WizardCommit(destinationCreatorStub(), taskCreatorStub(), scheduleCreatorStub());
        commit.execute(draft);

        assertThat(destinationCalls).isEmpty();
        assertThat(taskCalls).isEmpty();
        assertThat(scheduleTaskIdCalls).containsExactly(EXISTING_TASK_ID);
    }

    @Test
    void commit_retryAfterFailure_thenSucceeding_onlyCreatesRemainingSteps() {
        WizardDraft draft = draftWithNewDestinationTaskAndSchedule();

        TaskCreator failsOnce = new TaskCreator() {
            private boolean failed = false;

            @Override
            public TaskDto create(CreateTaskRequest request) {
                taskCalls.add(request);
                if (!failed) {
                    failed = true;
                    throw new RuntimeException("transient failure");
                }
                return new TaskDto(CREATED_TASK_ID, "svc", "name", null, true,
                        request.destinationId(), "event", null, 30_000, false, NOW, NOW, 0);
            }
        };
        WizardCommit commit = new WizardCommit(destinationCreatorStub(), failsOnce, scheduleCreatorStub());

        assertThatThrownBy(() -> commit.execute(draft)).isInstanceOf(RuntimeException.class);
        assertThat(destinationCalls).hasSize(1); // created once on the first attempt

        commit.execute(draft); // retry

        assertThat(destinationCalls).hasSize(1); // still just once — not recreated
        assertThat(taskCalls).hasSize(2);         // first (failed) + retry (succeeded)
        assertThat(draft.taskId()).isEqualTo(CREATED_TASK_ID);
    }

    @Test
    void commit_retryAfterScheduleCreationFails_doesNotDuplicateScheduleOnRetry() {
        WizardDraft draft = draftWithNewDestinationTaskAndSchedule();

        ScheduleCreator failsOnce = new ScheduleCreator() {
            private boolean failed = false;

            @Override
            public ScheduleResponse create(UUID taskId, CreateScheduleRequest request) {
                scheduleTaskIdCalls.add(taskId);
                if (!failed) {
                    failed = true;
                    throw new RuntimeException("schedule API down");
                }
                return new ScheduleResponse(UUID.randomUUID(), taskId, request.type(), request.label(),
                        request.runAt(), request.cronExpression(), request.intervalSeconds(), request.timezone(),
                        true, NOW, NOW);
            }
        };
        WizardCommit commit = new WizardCommit(destinationCreatorStub(), taskCreatorStub(), failsOnce);

        assertThatThrownBy(() -> commit.execute(draft)).isInstanceOf(RuntimeException.class);
        assertThat(draft.scheduleCreated()).isFalse();
        assertThat(destinationCalls).hasSize(1);
        assertThat(taskCalls).hasSize(1);

        commit.execute(draft); // retry: destination/task already resolved, schedule retried

        assertThat(destinationCalls).hasSize(1); // still just once
        assertThat(taskCalls).hasSize(1);         // still just once
        assertThat(scheduleTaskIdCalls).hasSize(2); // first (failed) + retry (succeeded) — not a third
        assertThat(draft.scheduleCreated()).isTrue();

        commit.execute(draft); // a further Finish click must not create a duplicate schedule

        assertThat(scheduleTaskIdCalls).hasSize(2); // unchanged — guarded by scheduleCreated()
    }

    // --- stubs ---

    private WizardDraft draftWithNewDestinationTaskAndSchedule() {
        WizardDraft draft = new WizardDraft();
        draft.createNewDestination(new CreateDestinationRequest(NEW_DESTINATION_ID, "KAFKA", null));
        draft.setTaskRequestFactory(this::taskRequest);
        draft.createNewSchedule(taskId -> scheduleRequest());
        return draft;
    }

    private CreateTaskRequest taskRequest(String destinationId) {
        return new CreateTaskRequest("svc", "name", null, true, destinationId, "event", null, 30_000, false);
    }

    private CreateScheduleRequest scheduleRequest() {
        return new CreateScheduleRequest("ONCE", "label", NOW, null, null, "UTC");
    }

    private DestinationCreator destinationCreatorStub() {
        return request -> {
            destinationCalls.add(request);
            return new DestinationDTO(request.destinationId(), request.destinationType(), request.config(), NOW);
        };
    }

    private TaskCreator taskCreatorStub() {
        return request -> {
            taskCalls.add(request);
            return new TaskDto(CREATED_TASK_ID, request.service(), request.name(), request.description(),
                    Boolean.TRUE.equals(request.active()), request.destinationId(), request.eventName(),
                    request.payload(), request.timeoutMs(), Boolean.TRUE.equals(request.supportsRetry()), NOW, NOW, 0);
        };
    }

    private ScheduleCreator scheduleCreatorStub() {
        return (taskId, request) -> {
            scheduleTaskIdCalls.add(taskId);
            return new ScheduleResponse(UUID.randomUUID(), taskId, request.type(), request.label(),
                    request.runAt(), request.cronExpression(), request.intervalSeconds(), request.timezone(),
                    true, NOW, NOW);
        };
    }
}
