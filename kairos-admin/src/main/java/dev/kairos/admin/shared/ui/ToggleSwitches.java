package dev.kairos.admin.shared.ui;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.checkbox.Checkbox;
import dev.kairos.admin.shared.style.Tokens;

import java.util.function.Consumer;

/**
 * Factory for the Apple-style toggle switch rendered in the Active column of
 * {@code TaskGrid} and {@code ScheduleGrid}. A single {@link Checkbox},
 * styled as a switch via {@link Tokens#TOGGLE_SWITCH_CLASS}, asks for
 * confirmation before a user-initiated toggle takes effect and reverts to
 * its previous state if the user cancels.
 */
public final class ToggleSwitches {

    private ToggleSwitches() {
    }

    /**
     * A confirmation prompt for a pending toggle. Implementations show a
     * dialog asking about {@code pendingValue} and must call exactly one of
     * {@code onConfirm} or {@code onCancel} once the user responds.
     */
    @FunctionalInterface
    public interface ConfirmationPrompt {
        void ask(boolean pendingValue, Runnable onConfirm, Runnable onCancel);
    }

    /**
     * Builds a switch initialised to {@code initialValue}. Value changes that
     * originate from the client (a user click) trigger {@code confirmation};
     * on confirm, {@code onConfirmedToggle} runs with the new value; on
     * cancel, the switch reverts to its previous value without prompting
     * again. Programmatic value changes (e.g. the revert itself) never
     * trigger {@code confirmation}.
     */
    public static Checkbox build(boolean initialValue, ConfirmationPrompt confirmation,
                                 Consumer<Boolean> onConfirmedToggle) {
        Checkbox toggle = new Checkbox(initialValue);
        toggle.addClassName(Tokens.TOGGLE_SWITCH_CLASS);
        toggle.addValueChangeListener(event -> onValueChange(toggle, event, confirmation, onConfirmedToggle));
        return toggle;
    }

    private static void onValueChange(Checkbox toggle, HasValue.ValueChangeEvent<Boolean> event,
                                      ConfirmationPrompt confirmation, Consumer<Boolean> onConfirmedToggle) {
        if (!event.isFromClient()) {
            return;
        }

        boolean previousValue = event.getOldValue();
        boolean pendingValue = event.getValue();
        confirmation.ask(pendingValue,
                () -> onConfirmedToggle.accept(pendingValue),
                () -> toggle.setValue(previousValue));
    }
}
