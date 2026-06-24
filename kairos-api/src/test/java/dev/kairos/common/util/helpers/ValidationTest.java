package dev.kairos.common.util.helpers;

import dev.kairos.common.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationTest {

    private static final String FIELD_NAME = "testField";
    private static final int MAX_LENGTH = 10;
    private static final String VALID_TEXT = "hello";
    private static final String EXACT_MAX_LENGTH_TEXT = "1234567890";
    private static final String OVER_MAX_LENGTH_TEXT = "12345678901";

    // --- requireText ---

    @Test
    void requireText_withValidValue_returnsValue() {
        String result = Validation.requireText(VALID_TEXT, FIELD_NAME, MAX_LENGTH);

        assertEquals(VALID_TEXT, result);
    }

    @Test
    void requireText_withNullValue_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Validation.requireText(null, FIELD_NAME, MAX_LENGTH));
    }

    @Test
    void requireText_withEmptyString_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Validation.requireText("", FIELD_NAME, MAX_LENGTH));
    }

    @Test
    void requireText_withBlankString_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Validation.requireText("   ", FIELD_NAME, MAX_LENGTH));
    }

    @Test
    void requireText_withExactMaxLengthValue_returnsValue() {
        String result = Validation.requireText(EXACT_MAX_LENGTH_TEXT, FIELD_NAME, MAX_LENGTH);

        assertEquals(EXACT_MAX_LENGTH_TEXT, result);
    }

    @Test
    void requireText_withValueExceedingMaxLength_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Validation.requireText(OVER_MAX_LENGTH_TEXT, FIELD_NAME, MAX_LENGTH));
    }

    // --- requirePositive ---

    @Test
    void requirePositive_withPositiveValue_returnsValue() {
        int result = Validation.requirePositive(1, FIELD_NAME);

        assertEquals(1, result);
    }

    @Test
    void requirePositive_withLargePositiveValue_returnsValue() {
        int result = Validation.requirePositive(Integer.MAX_VALUE, FIELD_NAME);

        assertEquals(Integer.MAX_VALUE, result);
    }

    @Test
    void requirePositive_withZero_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Validation.requirePositive(0, FIELD_NAME));
    }

    @Test
    void requirePositive_withNegativeValue_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Validation.requirePositive(-1, FIELD_NAME));
    }

    @Test
    void requirePositive_withMinIntValue_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> Validation.requirePositive(Integer.MIN_VALUE, FIELD_NAME));
    }
}
