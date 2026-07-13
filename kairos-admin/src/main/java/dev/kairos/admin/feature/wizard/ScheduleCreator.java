package dev.kairos.admin.feature.wizard;

import dev.kairos.common.dto.schedule.CreateScheduleRequest;
import dev.kairos.common.dto.schedule.ScheduleResponse;

import java.util.UUID;

/**
 * Narrow collaborator {@link WizardCommit} depends on instead of the full
 * {@code ScheduleService}, so unit tests can stub it with a lambda instead of
 * standing up a Spring bean.
 */
@FunctionalInterface
public interface ScheduleCreator {

    ScheduleResponse create(UUID taskId, CreateScheduleRequest request);
}
