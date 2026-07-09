package dev.kairos.admin.shared.style;

/**
 * CSS property-name constants for use with
 * {@link com.vaadin.flow.dom.Style#set(String, String)}, so components pass
 * named properties instead of scattering raw CSS strings. Values still come from
 * {@link Tokens}.
 */
public final class Css {

    private Css() {
    }

    public static final String FONT_FAMILY = "font-family";
    public static final String FONT_SIZE = "font-size";
    public static final String COLOR = "color";
    public static final String DISPLAY = "display";
    public static final String PADDING = "padding";
    public static final String BACKGROUND_COLOR = "background-color";
    public static final String BORDER_RADIUS = "border-radius";
    public static final String GRID_TEMPLATE_COLUMNS = "grid-template-columns";
    public static final String GRID_TEMPLATE_ROWS = "grid-template-rows";
    public static final String GRID_AUTO_FLOW = "grid-auto-flow";
    public static final String COLUMN_GAP = "column-gap";
    public static final String ROW_GAP = "row-gap";
}
