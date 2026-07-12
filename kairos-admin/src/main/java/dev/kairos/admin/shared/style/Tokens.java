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
    public static final String COLOR_PRIMARY_10 = "var(--lumo-primary-color-10pct)";
    public static final String COLOR_WARNING = "var(--lumo-warning-text-color)";

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

    /** Min width for an action button whose label must not wrap or clip. */
    public static final String BUTTON_MIN_WIDTH = "6rem";

    // --- typography families ---
    public static final String FONT_MONOSPACE = "var(--lumo-font-family-monospace, monospace)";

    /** CSS {@code auto} keyword, e.g. an auto margin that pushes siblings apart. */
    public static final String AUTO = "auto";

    /** CSS {@code block} display keyword. */
    public static final String DISPLAY_BLOCK = "block";

    /** CSS {@code grid} display keyword. */
    public static final String DISPLAY_GRID = "grid";

    /** Two equal-width grid columns, e.g. the cron next-runs preview. */
    public static final String GRID_TWO_COLUMNS = "1fr 1fr";

    /** Five grid rows for a column-first two-column preview (10 items). */
    public static final String GRID_FIVE_ROWS = "repeat(5, auto)";

    /** CSS {@code column} grid-auto-flow keyword (fill top-to-bottom first). */
    public static final String GRID_FLOW_COLUMN = "column";

    // --- Apple-style extensions (not part of Lumo) ---
    public static final String GLASS_BLUR = "var(--kairos-glass-blur)";
    public static final String SURFACE_TRANSLUCENT = "var(--kairos-surface-translucent)";
    public static final String GRADIENT_PRIMARY = "var(--kairos-gradient-primary)";
    public static final String GRADIENT_SURFACE = "var(--kairos-gradient-surface)";
    public static final String TRANSITION = "var(--kairos-transition)";

    /** Vaadin theme string for a destructive confirmation button (red, primary weight). */
    public static final String THEME_DANGER_CONFIRM = "error primary";

    public static final int FORM_COLUMNS = 2;
    public static final int FORM_COLSPAN_FULL = FORM_COLUMNS;

    /** Vaadin responsive-step breakpoint meaning "apply from 0px up" (always). */
    public static final String FORM_BREAKPOINT_ZERO = "0";

    /** Vaadin theme string for a success-coloured badge (green). */
    public static final String THEME_BADGE_SUCCESS = "badge success";

    /** Vaadin theme string for a contrast-coloured badge (neutral, e.g. the "Preview" label). */
    public static final String THEME_BADGE_CONTRAST = "badge contrast";

    // --- dashboard card grid ---
    /** Responsive auto-fitting card grid: as many columns as fit, min card width. */
    public static final String GRID_CARDS_STATS = "repeat(auto-fit, minmax(220px, 1fr))";

    /** Large display number, e.g. the headline value on a dashboard stat card. */
    public static final String FONT_DISPLAY = "2.25rem";

    /** Circular icon badge size on a card. */
    public static final String ICON_BADGE_SIZE = "2.75rem";

    /** Card icon glyph size. */
    public static final String ICON_SIZE_M = "1.375rem";

    /** CSS {@code center} keyword, used for {@code align-items} / {@code justify-content}. */
    public static final String CENTER = "center";

    /** Pulls a tertiary (link-style) button flush with the card's left text edge. */
    public static final String NEGATIVE_INSET = "-0.4rem";

    /** Pointer cursor for interactive (clickable) surfaces. */
    public static final String CURSOR_POINTER = "pointer";

    /** CSS class applied to a {@code Checkbox} rendered as an Apple-style toggle switch. */
    public static final String TOGGLE_SWITCH_CLASS = "kairos-toggle-switch";
}
