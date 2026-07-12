package dev.kairos.common.util.helpers;

import dev.kairos.common.exceptions.ValidationException;

/**
 * Small, framework-free validation guards shared across the domain. Each guard
 * returns the validated value (so it can be used inline in assignments) and
 * throws {@link ValidationException} on failure.
 */
public final class Validation {

    private Validation() {
        // utility class — not instantiable
    }

    /**
     * Requires a non-null, non-blank string no longer than {@code maxLength}.
     *
     * @param value     the string to validate
     * @param field     name of the field, used in the error message
     * @param maxLength maximum allowed length (inclusive)
     * @return {@code value}, unchanged, when valid
     * @throws ValidationException if {@code value} is null, blank, or longer than {@code maxLength}
     */
    public static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
        if (value.length() > maxLength) {
            throw new ValidationException(
                    field + " must be at most " + maxLength + " characters");
        }
        return value;
    }


    /**
     * Requires a non-null, non-blank string (no length limit).
     *
     * @param value the string to validate
     * @param field name of the field, used in the error message
     * @return {@code value}, unchanged, when valid
     * @throws ValidationException if {@code value} is null or blank
     */
    public static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }

        return value;
    }

    /**
     * Requires a strictly positive number.
     *
     * @param field name of the field, used in the error message
     */
    public static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new ValidationException(field + " must be greater than 0");
        }
        return value;
    }
}