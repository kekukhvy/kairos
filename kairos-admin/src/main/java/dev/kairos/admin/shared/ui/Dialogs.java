package dev.kairos.admin.shared.ui;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import dev.kairos.admin.shared.style.Tokens;

/**
 * Dialog factory for recurring confirmation flows, so features build a
 * destructive-confirm dialog through one place instead of repeating the same
 * header/text/theme/listener wiring.
 */
public final class Dialogs {

    private Dialogs() {
    }

    /**
     * Opens a cancelable confirmation dialog for a destructive action. The
     * confirm button uses the danger theme and runs {@code onConfirm} when
     * pressed.
     *
     * @param title       dialog header
     * @param text        explanatory body text
     * @param confirmText label of the confirm button
     * @param onConfirm   action to run when the user confirms
     */
    public static void confirmDelete(String title, String text, String confirmText, Runnable onConfirm) {
        ConfirmDialog dialog = newDialog(title, text, confirmText);
        dialog.setConfirmButtonTheme(Tokens.THEME_DANGER_CONFIRM);
        dialog.addConfirmListener(e -> onConfirm.run());
        dialog.open();
    }

    /**
     * Builds and opens a cancelable confirmation dialog for a non-destructive
     * state change (e.g. toggling a row's active switch). Runs
     * {@code onConfirm} when the user confirms, or {@code onCancel} when the
     * user cancels — exactly one of the two, at most once.
     *
     * @param title       dialog header
     * @param text        explanatory body text
     * @param confirmText label of the confirm button
     * @param onConfirm   action to run when the user confirms
     * @param onCancel    action to run when the user cancels
     * @return the dialog, already opened
     */
    public static ConfirmDialog confirmToggle(String title, String text, String confirmText,
                                              Runnable onConfirm, Runnable onCancel) {
        ConfirmDialog dialog = newDialog(title, text, confirmText);
        dialog.addConfirmListener(e -> onConfirm.run());
        dialog.addCancelListener(e -> onCancel.run());
        dialog.open();
        return dialog;
    }

    /**
     * Creates a cancelable {@link ConfirmDialog} with its header, body text and
     * confirm-button label set — the wiring both {@link #confirmDelete} and
     * {@link #confirmToggle} share, before each adds its own theme and listeners.
     */
    private static ConfirmDialog newDialog(String title, String text, String confirmText) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(title);
        dialog.setText(text);
        dialog.setCancelable(true);
        dialog.setConfirmText(confirmText);
        return dialog;
    }
}
