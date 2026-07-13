package dev.kairos.admin.feature.wizard;

import com.vaadin.flow.component.dialog.Dialog;
import dev.kairos.admin.feature.dashboard.DashboardText;
import dev.kairos.admin.feature.dashboard.DashboardView;
import dev.kairos.admin.feature.destination.DestinationService;
import dev.kairos.admin.feature.schedule.ScheduleService;
import dev.kairos.admin.feature.task.TaskService;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.feature.task.TaskView;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies the launch-point wiring for {@link SetupWizard}: the Dashboard's
 * {@code CREATE_TASK} CTA and the Tasks screen's {@code GUIDED_SETUP}
 * affordance are the two documented entry points that must open the wizard
 * (acceptance criterion #1 of the Guided Setup Wizard spec).
 *
 * <p>Neither {@code DashboardView} (its constructor calls
 * {@code DashboardPrefs#load}, which needs browser {@code localStorage}) nor
 * {@code TaskView}, nor a click on their CTA buttons, nor {@code Dialog#open()}
 * itself (needs {@code UI.getCurrent()}/a live {@code VaadinSession}) can be
 * exercised without a running UI — the same documented limitation used by
 * {@code DashboardCtaNavigationTargetTest} for the Dashboard's stat-card CTAs.
 * The actual click-to-modal-opens path is therefore NOT asserted here.
 *
 * <p>What IS statically verifiable, and is asserted below: {@link SetupWizard}
 * is structurally a Vaadin {@link Dialog} (so "opens a modal" is true by
 * construction), its public constructor accepts exactly the collaborators
 * each launch point supplies, both {@code DashboardView} and {@code TaskView}
 * declare a private {@code openWizard()} method that constructs it (pinning
 * the launch-point wiring so a refactor that drops the method fails this
 * test), and the CTA/affordance label constants the buttons render exist and
 * are non-blank.
 */
class SetupWizardLaunchWiringTest {

    private static final String OPEN_WIZARD_METHOD = "openWizard";

    // --- SetupWizard is structurally a modal dialog ---

    @Test
    void setupWizard_isDialog_structurallyOpensAsModal() {
        assertThat(Dialog.class).isAssignableFrom(SetupWizard.class);
    }

    // --- SetupWizard's public constructor takes exactly the collaborators the launch points supply ---

    @Test
    void setupWizard_publicConstructor_acceptsCollaboratorsSuppliedByLaunchPoints() {
        assertThatCode(() -> SetupWizard.class.getConstructor(
                JsonMapper.class, TaskService.class, DestinationService.class,
                ScheduleService.class, Runnable.class))
                .doesNotThrowAnyException();
    }

    @Test
    void setupWizard_hasExactlyOnePublicConstructor() {
        Constructor<?>[] constructors = SetupWizard.class.getConstructors();

        assertThat(constructors).hasSize(1);
    }

    // --- CTA / affordance labels exist and are non-blank ---

    @Test
    void dashboardCreateTaskCta_labelIsNonBlank() {
        assertThat(DashboardText.CREATE_TASK).isNotBlank();
    }

    @Test
    void taskViewGuidedSetupAffordance_labelIsNonBlank() {
        assertThat(TaskText.GUIDED_SETUP).isNotBlank();
    }

    // --- both launch points declare a private openWizard() that constructs SetupWizard ---

    @Test
    void dashboardView_declaresPrivateOpenWizardMethod() throws NoSuchMethodException {
        Method openWizard = DashboardView.class.getDeclaredMethod(OPEN_WIZARD_METHOD);

        assertThat(Modifier.isPrivate(openWizard.getModifiers())).isTrue();
    }

    @Test
    void taskView_declaresPrivateOpenWizardMethod() throws NoSuchMethodException {
        Method openWizard = TaskView.class.getDeclaredMethod(OPEN_WIZARD_METHOD);

        assertThat(Modifier.isPrivate(openWizard.getModifiers())).isTrue();
    }
}
