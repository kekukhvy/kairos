package dev.kairos.admin.shared.util;

/**
 * Small string helpers shared across features.
 */
public final class Strings {

    private Strings() {
    }

    /**
     * Returns the trimmed value, or {@code null} when it is {@code null} or blank.
     */
    public static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Case-insensitive containment check that treats a {@code null} value as
     * "no match". {@code term} is expected to be already lower-cased.
     */
    public static boolean containsIgnoreCase(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
    }
}
