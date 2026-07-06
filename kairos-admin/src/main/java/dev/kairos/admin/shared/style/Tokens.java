package dev.kairos.admin.shared.style;

/**
 * Design tokens mapped to Lumo CSS custom properties.
 */
public final class Tokens {

    private Tokens() {
    }

    // --- spacing ---
    public static final String SPACE_XS = "var(--lumo-space-xs)";
    public static final String SPACE_S = "var(--lumo-space-s)";
    public static final String SPACE_M = "var(--lumo-space-m)";
    public static final String SPACE_L = "var(--lumo-space-l)";
    public static final String SPACE_XL = "var(--lumo-space-xl)";

    // --- radius ---
    public static final String RADIUS_S = "var(--lumo-border-radius-s)";
    public static final String RADIUS_M = "var(--lumo-border-radius-m)";
    public static final String RADIUS_L = "var(--lumo-border-radius-l)";

    // --- surface / color ---
    public static final String COLOR_PRIMARY = "var(--lumo-primary-color)";
    public static final String COLOR_BASE = "var(--lumo-base-color)";
    public static final String COLOR_CONTRAST_5 = "var(--lumo-contrast-5pct)";
    public static final String COLOR_CONTRAST_10 = "var(--lumo-contrast-10pct)";
    public static final String COLOR_ERROR = "var(--lumo-error-color)";
    public static final String COLOR_SUCCESS = "var(--lumo-success-color)";

    // --- text ---
    public static final String TEXT_BODY = "var(--lumo-body-text-color)";
    public static final String TEXT_SECONDARY = "var(--lumo-secondary-text-color)";

    // --- typography ---
    public static final String FONT_S = "var(--lumo-font-size-s)";
    public static final String FONT_M = "var(--lumo-font-size-m)";
    public static final String FONT_L = "var(--lumo-font-size-l)";
    public static final String FONT_XL = "var(--lumo-font-size-xl)";

    // --- shadow ---
    public static final String SHADOW_S = "var(--lumo-box-shadow-s)";
    public static final String SHADOW_M = "var(--lumo-box-shadow-m)";
    public static final String SHADOW_L = "var(--lumo-box-shadow-l)";

    // --- composed helpers ---
    public static final String BORDER_LIGHT = "1px solid var(--lumo-contrast-10pct)";

    // --- font weight ---
    public static final String FONT_WEIGHT_SEMIBOLD = "600";
    public static final String FONT_WEIGHT_BOLD = "700";

    // --- sizing ---
    public static final String DIALOG_WIDTH_M = "480px";
    public static final String DIALOG_WIDTH_L = "900px";

    /** CSS {@code auto} keyword, e.g. an auto margin that pushes siblings apart. */
    public static final String AUTO = "auto";

    // --- Apple-style extensions (not part of Lumo) ---
    public static final String GLASS_BLUR = "var(--kairos-glass-blur)";
    public static final String SURFACE_TRANSLUCENT = "var(--kairos-surface-translucent)";
    public static final String GRADIENT_PRIMARY = "var(--kairos-gradient-primary)";
    public static final String GRADIENT_SURFACE = "var(--kairos-gradient-surface)";
    public static final String TRANSITION = "var(--kairos-transition)";

    public static final String THEME_DANGER_CONFIRM = "error primary";

    public static final int FORM_COLUMNS = 2;
    public static final int FORM_COLSPAN_FULL = FORM_COLUMNS;

    public static final String THEME_BADGE_SUCCESS = "badge success";
    public static final String THEME_BADGE_CONTRAST = "badge contrast";
}