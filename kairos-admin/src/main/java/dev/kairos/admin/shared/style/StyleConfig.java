package dev.kairos.admin.shared.style;

import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.dom.Style;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent builder for inline component styles.
 * Centralizes CSS so views and components never touch getStyle() directly.
 *
 * <pre>
 * StyleConfig.create()
 *     .padding(Tokens.SPACE_M)
 *     .gap(Tokens.SPACE_S)
 *     .background(Tokens.COLOR_BASE)
 *     .borderRadius(Tokens.RADIUS_M)
 *     .fullWidth()
 *     .applyTo(layout);
 * </pre>
 */
public final class StyleConfig {

    private final Map<String, String> properties = new LinkedHashMap<>();

    private StyleConfig() {
    }

    public static StyleConfig create() {
        return new StyleConfig();
    }

    // --- spacing ---
    public StyleConfig padding(String value) {
        return set("padding", value);
    }

    public StyleConfig paddingTop(String value) {
        return set("padding-top", value);
    }

    public StyleConfig paddingBottom(String value) {
        return set("padding-bottom", value);
    }

    public StyleConfig margin(String value) {
        return set("margin", value);
    }

    public StyleConfig gap(String value) {
        return set("gap", value);
    }

    // --- sizing ---
    public StyleConfig width(String value) {
        return set("width", value);
    }

    public StyleConfig height(String value) {
        return set("height", value);
    }

    public StyleConfig maxWidth(String value) {
        return set("max-width", value);
    }

    public StyleConfig fullWidth() {
        return set("width", "100%");
    }

    public StyleConfig fullHeight() {
        return set("height", "100%");
    }

    // --- flex layout ---
    public StyleConfig display(String value) {
        return set("display", value);
    }

    public StyleConfig flexDirection(String value) {
        return set("flex-direction", value);
    }

    public StyleConfig alignItems(String value) {
        return set("align-items", value);
    }

    public StyleConfig justifyContent(String value) {
        return set("justify-content", value);
    }

    // --- surface / color ---
    public StyleConfig background(String value) {
        return set("background", value);
    }

    public StyleConfig color(String value) {
        return set("color", value);
    }

    public StyleConfig border(String value) {
        return set("border", value);
    }

    public StyleConfig borderRadius(String value) {
        return set("border-radius", value);
    }

    public StyleConfig boxShadow(String value) {
        return set("box-shadow", value);
    }

    // --- typography ---
    public StyleConfig fontSize(String value) {
        return set("font-size", value);
    }

    public StyleConfig fontWeight(String value) {
        return set("font-weight", value);
    }

    public StyleConfig marginInlineStart(String value) {
        return set("margin-inline-start", value);
    }

    public StyleConfig marginInlineEnd(String value) {
        return set("margin-inline-end", value);
    }

    // --- generic escape hatch ---
    public StyleConfig set(String property, String value) {
        properties.put(property, value);
        return this;
    }

    // --- terminal operations ---

    /**
     * Applies the collected styles and returns the same component for chaining.
     */
    public <T extends HasStyle> T applyTo(T component) {
        Style style = component.getStyle();
        properties.forEach(style::set);
        return component;
    }

    /**
     * Applies the same style set to several components at once.
     */
    public void applyToAll(HasStyle... components) {
        for (HasStyle component : components) {
            Style style = component.getStyle();
            properties.forEach(style::set);
        }
    }
}