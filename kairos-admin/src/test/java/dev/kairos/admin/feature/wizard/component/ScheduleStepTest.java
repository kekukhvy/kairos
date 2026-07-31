package dev.kairos.admin.feature.wizard.component;

import com.vaadin.flow.component.button.Button;
import dev.kairos.admin.feature.schedule.CronText;
import dev.kairos.admin.feature.schedule.ScheduleText;
import dev.kairos.admin.feature.schedule.dto.ScheduleType;
import dev.kairos.admin.feature.wizard.WizardDraft;
import dev.kairos.common.dto.schedule.CreateScheduleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@code ScheduleStep}, which always creates a new schedule for the
 * task being set up (the wizard always creates a new task, so its schedule
 * is always newly created for that task — there is no "select existing"
 * mode). Mirrors {@code ScheduleForm}: the same type-driven when-field
 * validation and the same CRON row (field + visual builder).
 */
class ScheduleStepTest {

    private static final Instant FUTURE = Instant.now().plusSeconds(3_600);
    /** Far enough ahead to stay in the future in any timezone's wall-clock reinterpretation. */
    private static final Instant FAR_FUTURE = Instant.now().plus(Duration.ofDays(2));
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

    // --- cron builder: the wizard offers the same visual builder as ScheduleForm ---

    /**
     * Regression: the wizard used to place the bare cron text field, so picking
     * CRON gave the operator no "Build cron" button — unlike {@code ScheduleForm}.
     */
    @Test
    void cronType_cronRowExposesBuildButton() {
        step.type().setValue(ScheduleType.CRON);

        assertThat(buildButton()).isPresent();
    }

    @Test
    void cronType_cronRowIsVisible() {
        step.type().setValue(ScheduleType.CRON);

        assertThat(step.cronRow().isVisible()).isTrue();
    }

    @Test
    void nonCronType_cronRowIsHidden() {
        step.type().setValue(ScheduleType.ONCE);
        assertThat(step.cronRow().isVisible()).isFalse();

        step.type().setValue(ScheduleType.FIXED);
        assertThat(step.cronRow().isVisible()).isFalse();
    }

    /** The cron field the builder writes back into is the one the step reads on {@code readInto}. */
    @Test
    void cronRow_containsTheCronFieldTheStepReads() {
        step.type().setValue(ScheduleType.CRON);

        assertThat(step.cronRow().getChildren()).contains(step.cronExpression());
    }

    private Optional<Button> buildButton() {
        return step.cronRow().getChildren()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(b -> CronText.BUILD_BUTTON.equals(b.getText()))
                .findFirst();
    }

    // --- timezone picker ---

    @Test
    void timezone_defaultsToUtc() {
        assertThat(step.timezone().getValue()).isEqualTo(ScheduleText.DEFAULT_TIMEZONE);
    }

    @Test
    void timezone_offersSharedShortlist() {
        assertThat(step.timezone().getListDataView().getItems().toList())
                .isEqualTo(ScheduleText.TIMEZONE_OPTIONS);
    }

    @Test
    void timezone_shortlistZone_selectable() {
        step.timezone().setValue("Europe/Kyiv");

        assertThat(step.timezone().getValue()).isEqualTo("Europe/Kyiv");
    }

    @Test
    void timezone_validCustomZoneOffShortlist_acceptedByValidate() {
        step.type().setValue(ScheduleType.ONCE);
        step.runAt().setValue(LocalDateTime.ofInstant(FAR_FUTURE, ZoneOffset.UTC));
        step.timezone().setValue("Pacific/Chatham");

        assertThat(step.validate()).isTrue();
        assertThat(step.timezone().isInvalid()).isFalse();
    }

    @Test
    void timezone_invalidCustomZone_blocksValidateAndMarksInvalid() {
        step.type().setValue(ScheduleType.ONCE);
        step.runAt().setValue(LocalDateTime.ofInstant(FAR_FUTURE, ZoneOffset.UTC));
        step.timezone().setValue("Europe/Kyv");

        assertThat(step.validate()).isFalse();
        assertThat(step.timezone().isInvalid()).isTrue();
    }

    @Test
    void fixedType_timezoneFieldHidden() {
        step.type().setValue(ScheduleType.FIXED);

        assertThat(step.timezone().isVisible()).isFalse();
    }

    @Test
    void fixedType_invalidTimezoneDoesNotBlockValidate() {
        step.type().setValue(ScheduleType.FIXED);
        step.timezone().setValue("Europe/Kyv");
        step.intervalSeconds().setValue(60);

        assertThat(step.validate()).isTrue();
    }

    @Test
    void nonFixedType_timezoneFieldVisible() {
        step.type().setValue(ScheduleType.ONCE);
        assertThat(step.timezone().isVisible()).isTrue();

        step.type().setValue(ScheduleType.CRON);
        assertThat(step.timezone().isVisible()).isTrue();
    }
}
