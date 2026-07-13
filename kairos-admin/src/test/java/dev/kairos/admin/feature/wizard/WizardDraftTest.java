package dev.kairos.admin.feature.wizard;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@code WizardDraft}'s captured-id/flag bookkeeping: the destination
 * and task ids, and the schedule-created flag, all start unset and are set
 * independently — which is what lets {@code WizardCommit} skip
 * already-created entities on a retry.
 */
class WizardDraftTest {

    @Test
    void newDraft_destinationIdStartsNull() {
        WizardDraft draft = new WizardDraft();

        assertThat(draft.destinationId()).isNull();
    }

    @Test
    void newDraft_taskIdStartsNull() {
        WizardDraft draft = new WizardDraft();

        assertThat(draft.taskId()).isNull();
    }

    @Test
    void newDraft_scheduleCreatedStartsFalse() {
        WizardDraft draft = new WizardDraft();

        assertThat(draft.scheduleCreated()).isFalse();
    }

    @Test
    void setDestinationId_isReflectedByGetter() {
        WizardDraft draft = new WizardDraft();

        draft.setDestinationId("kafka-dest-1");

        assertThat(draft.destinationId()).isEqualTo("kafka-dest-1");
    }

    @Test
    void setTaskId_isReflectedByGetter() {
        WizardDraft draft = new WizardDraft();
        UUID taskId = UUID.randomUUID();

        draft.setTaskId(taskId);

        assertThat(draft.taskId()).isEqualTo(taskId);
    }

    @Test
    void markScheduleCreated_isReflectedByGetter() {
        WizardDraft draft = new WizardDraft();

        draft.markScheduleCreated();

        assertThat(draft.scheduleCreated()).isTrue();
    }
}
