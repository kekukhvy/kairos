package dev.kairos.admin.shared.ui;

/**
 * Shared UI string constants used by cross-feature components (e.g.
 * {@link FilterBar}). Feature-specific labels stay in their own feature's
 * text class; only strings reused across features belong here.
 */
public final class UiText {

    private UiText() {
    }

    public static final String FILTER_SEARCH_PLACEHOLDER = "Search";
    public static final String FILTER_CLEAR = "Clear";
    public static final String FILTER_ALL = "All";

    // --- generic validation messages ---
    public static final String VALIDATION_REQUIRED = "Required";
    public static final String VALIDATION_INVALID_JSON = "Invalid JSON";

    // --- generic action labels ---
    public static final String BTN_SAVE = "Save";
    public static final String BTN_CANCEL = "Cancel";
    public static final String BTN_CLOSE = "Close";
    public static final String ACTION_DELETE = "Delete";
}
