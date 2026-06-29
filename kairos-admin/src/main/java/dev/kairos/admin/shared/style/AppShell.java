package dev.kairos.admin.shared.style;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Loads the Apple-style Kairos look. Lumo is imported first, then our overrides
 * ({@code META-INF/resources/styles.css}) so they win on specificity. The visual
 * language is implemented purely by overriding Lumo design tokens, so existing
 * {@link StyleConfig}/{@link Tokens} code inherits it without changes.
 */
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(AppShell.STYLES)
public class AppShell implements AppShellConfigurator {

    public static final String STYLES = "styles.css";
}
