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
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(title);
        dialog.setText(text);
        dialog.setCancelable(true);
        dialog.setConfirmText(confirmText);
        dialog.setConfirmButtonTheme(Tokens.THEME_DANGER_CONFIRM);
        dialog.addConfirmListener(e -> onConfirm.run());
        dialog.open();
    }
}
