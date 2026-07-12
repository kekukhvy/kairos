package dev.kairos.admin.shared.ui;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.checkbox.Checkbox;
import dev.kairos.admin.shared.style.Tokens;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ToggleSwitches#build}, the Apple-style switch factory used by
 * {@code TaskGrid} and {@code ScheduleGrid} for their Active column.
 *
 * <p>The confirm/cancel flow itself opens a {@link com.vaadin.flow.component.confirmdialog.ConfirmDialog},
 * which requires a live Vaadin session and cannot be exercised in a unit test.
 * These tests cover what can be verified without a session: the switch's
 * initial rendering, and that a value change not originating from the client
 * (i.e. the programmatic revert-on-cancel) never re-triggers the confirmation
 * callback that {@link #build} wires up.
 */
class ToggleSwitchesTest {

    @Test
    void build_initialValueTrue_checkboxChecked() {
        Checkbox toggle = ToggleSwitches.build(true, (active, onConfirm, onCancel) -> { }, active -> { });

        assertThat(toggle.getValue()).isTrue();
    }

    @Test
    void build_initialValueFalse_checkboxUnchecked() {
        Checkbox toggle = ToggleSwitches.build(false, (active, onConfirm, onCancel) -> { }, active -> { });

        assertThat(toggle.getValue()).isFalse();
    }

    @Test
    void build_appliesAppleSwitchStyleClass() {
        Checkbox toggle = ToggleSwitches.build(true, (active, onConfirm, onCancel) -> { }, active -> { });

        assertThat(toggle.getClassNames()).contains(Tokens.TOGGLE_SWITCH_CLASS);
    }

    @Test
    void programmaticValueChange_doesNotPromptConfirmation() {
        List<Boolean> prompted = new ArrayList<>();
        Checkbox toggle = ToggleSwitches.build(true, (active, onConfirm, onCancel) -> prompted.add(active), active -> { });

        toggle.setValue(false);

        assertThat(prompted).isEmpty();
    }

    @Test
    void clientValueChange_promptsConfirmationWithNewValue() {
        List<Boolean> prompted = new ArrayList<>();
        Checkbox toggle = ToggleSwitches.build(true, (active, onConfirm, onCancel) -> prompted.add(active), active -> { });

        setValueFromClient(toggle, false);

        assertThat(prompted).containsExactly(false);
    }

    @Test
    void clientValueChange_onConfirm_invokesOnConfirmedToggleWithNewValue() {
        List<Boolean> confirmed = new ArrayList<>();
        Checkbox toggle = ToggleSwitches.build(true,
                (active, onConfirm, onCancel) -> onConfirm.run(),
                confirmed::add);

        setValueFromClient(toggle, false);

        assertThat(confirmed).containsExactly(false);
    }

    @Test
    void clientValueChange_onCancel_revertsWithoutInvokingOnConfirmedToggle() {
        List<Boolean> confirmed = new ArrayList<>();
        Checkbox toggle = ToggleSwitches.build(true,
                (active, onConfirm, onCancel) -> onCancel.run(),
                confirmed::add);

        setValueFromClient(toggle, false);

        assertThat(confirmed).isEmpty();
        assertThat(toggle.getValue()).isTrue();
    }

    @Test
    void clientValueChange_onCancel_revertDoesNotReopenConfirmation() {
        List<Boolean> prompted = new ArrayList<>();
        Checkbox toggle = ToggleSwitches.build(true,
                (active, onConfirm, onCancel) -> {
                    prompted.add(active);
                    onCancel.run();
                },
                active -> { });

        setValueFromClient(toggle, false);

        assertThat(prompted).hasSize(1);
    }

    // --- helpers ---

    /**
     * Simulates a value change originating from the browser (as opposed to a
     * server-side {@code setValue} call), matching what
     * {@code HasValue.ValueChangeEvent#isFromClient()} reports for a real
     * click. {@link Checkbox} exposes no public API to fire a "from client"
     * event, so this dispatches the same {@link AbstractField.ComponentValueChangeEvent}
     * Vaadin fires internally when the client reports a change, with
     * {@code fromClient = true}.
     */
    private static void setValueFromClient(Checkbox toggle, boolean value) {
        boolean oldValue = toggle.getValue();
        toggle.getElement().setProperty("checked", value);
        ComponentUtil.fireEvent(toggle,
                new AbstractField.ComponentValueChangeEvent<>(toggle, toggle, oldValue, true));
    }
}
