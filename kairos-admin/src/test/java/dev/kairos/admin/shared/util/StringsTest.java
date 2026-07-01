package dev.kairos.admin.shared.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringsTest {

    private static final String BLANK_SPACES = "  ";
    private static final String PADDED_VALUE = "  x ";
    private static final String TRIMMED_VALUE = "x";
    private static final String NON_BLANK = "x";

    // --- trimToNull ---

    @Test
    void trimToNull_nullInput_returnsNull() {
        assertThat(Strings.trimToNull(null)).isNull();
    }

    @Test
    void trimToNull_emptyString_returnsNull() {
        assertThat(Strings.trimToNull("")).isNull();
    }

    @Test
    void trimToNull_blankSpaces_returnsNull() {
        assertThat(Strings.trimToNull(BLANK_SPACES)).isNull();
    }

    @Test
    void trimToNull_paddedValue_returnsTrimmedValue() {
        assertThat(Strings.trimToNull(PADDED_VALUE)).isEqualTo(TRIMMED_VALUE);
    }

    // --- isBlank ---

    @Test
    void isBlank_nullInput_returnsTrue() {
        assertThat(Strings.isBlank(null)).isTrue();
    }

    @Test
    void isBlank_emptyString_returnsTrue() {
        assertThat(Strings.isBlank("")).isTrue();
    }

    @Test
    void isBlank_blankSpaces_returnsTrue() {
        assertThat(Strings.isBlank(BLANK_SPACES)).isTrue();
    }

    @Test
    void isBlank_nonBlankString_returnsFalse() {
        assertThat(Strings.isBlank(NON_BLANK)).isFalse();
    }
}
