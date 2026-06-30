package dev.kairos.admin.shared.ui;

import com.vaadin.flow.component.html.Span;
import dev.kairos.admin.shared.style.Tokens;

/**
 * Badge factory. Centralizes Lumo badge themes so features never set the
 * {@code theme="badge ..."} attribute by hand.
 */
public final class Badges {

    private Badges() {
    }

    public static Span success(String text) {
        return badge(text, Tokens.THEME_BADGE_SUCCESS);
    }

    public static Span neutral(String text) {
        return badge(text, Tokens.THEME_BADGE_CONTRAST);
    }

    private static Span badge(String text, String theme) {
        Span span = new Span(text);
        for (String part : theme.split(" ")) {
            span.getElement().getThemeList().add(part);
        }
        return span;
    }
}