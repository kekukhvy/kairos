package dev.kairos.admin.shared.ui;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

/**
 * Button factory. Features build buttons through here instead of constructing
 * {@link Button} and calling {@code addThemeVariants} by hand, so variant and
 * styling stay consistent across the UI.
 */
public final class Buttons {

    private Buttons() {
    }

    /** Filled accent button for the primary action of a view or dialog. */
    public static Button primary(String text, ComponentEventListener<ClickEvent<Button>> onClick) {
        return build(text, onClick, ButtonVariant.LUMO_PRIMARY);
    }

    /** Neutral button for secondary actions (e.g. the default outlined style). */
    public static Button secondary(String text, ComponentEventListener<ClickEvent<Button>> onClick) {
        return build(text, onClick);
    }

    /** Low-emphasis text button (e.g. Cancel). */
    public static Button tertiary(String text, ComponentEventListener<ClickEvent<Button>> onClick) {
        return build(text, onClick, ButtonVariant.LUMO_TERTIARY);
    }

    /** Destructive action button. */
    public static Button danger(String text, ComponentEventListener<ClickEvent<Button>> onClick) {
        return build(text, onClick, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
    }

    private static Button build(
            String text,
            ComponentEventListener<ClickEvent<Button>> onClick,
            ButtonVariant... variants) {
        Button button = new Button(text, onClick);
        if (variants.length > 0) {
            button.addThemeVariants(variants);
        }
        return button;
    }
}
