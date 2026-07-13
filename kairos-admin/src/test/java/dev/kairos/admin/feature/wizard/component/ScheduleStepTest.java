package dev.kairos.admin.feature.wizard.component;

import dev.kairos.admin.feature.schedule.dto.ScheduleType;
import dev.kairos.admin.feature.wizard.WizardDraft;
import dev.kairos.common.dto.schedule.CreateScheduleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@code ScheduleStep}, which always creates a new schedule for the
 * task being set up (the wizard always creates a new task, so its schedule
 * is always newly created for that task — there is no "select existing"
 * mode). Mirrors {@code ScheduleForm}'s type-driven when-field validation.
 */
class ScheduleStepTest {

    private static final Instant FUTURE = Instant.now().plusSeconds(3_600);
    private static final String CRON = "0 0 * * *";
    private static final UUID RESOLVED_TASK_ID = UUID.randomUUID();

    private ScheduleStep step;

    @BeforeEach
    void setUp() {
        step = new ScheduleStep();
    }

    @Test
    void defaultTypeOnce_missingRunAt_validateFails() {
        assertThat(step.validate()).isFalse();
    }

    @Test
    void onceWithFutureRunAt_validateSucceeds() {
        step.type().setValue(ScheduleType.ONCE);
        step.runAt().setValue(LocalDateTime.ofInstant(FUTURE, ZoneOffset.UTC));

        assertThat(step.validate()).isTrue();
    }

    @Test
    void cronWithoutExpression_validateFails() {
        step.type().setValue(ScheduleType.CRON);

        assertThat(step.validate()).isFalse();
    }

    @Test
    void cronWithExpression_validateSucceeds() {
        step.type().setValue(ScheduleType.CRON);
        step.cronExpression().setValue(CRON);

        assertThat(step.validate()).isTrue();
    }

    @Test
    void readInto_capturesFactoryProducingCronRequest() {
        step.type().setValue(ScheduleType.CRON);
        step.cronExpression().setValue(CRON);
        WizardDraft draft = new WizardDraft();

        step.readInto(draft);

        CreateScheduleRequest request = draft.scheduleRequestFactory().apply(RESOLVED_TASK_ID);
        assertThat(request.type()).isEqualTo("CRON");
        assertThat(request.cronExpression()).isEqualTo(CRON);
    }
}
