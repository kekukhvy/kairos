package dev.kairos.admin.shared.form;

import com.vaadin.flow.component.textfield.TextArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FieldValidation#parseJson}.
 *
 * <p>{@link TextArea} can be constructed without a running VaadinSession (it
 * carries no server-side push or session state at construction time), so these
 * tests run as plain JUnit tests with no Spring context.
 */
class FieldValidationTest {

    private static final String VALID_JSON = "{\"key\":\"value\"}";
    private static final String INVALID_JSON = "{not valid json";
    private static final String BLANK_VALUE = "   ";
    private static final String EMPTY_VALUE = "";
    private static final String ERROR_MESSAGE = "Invalid JSON";

    private JsonMapper jsonMapper;
    private TextArea field;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        field = new TextArea();
    }

    // --- blank field ---

    @Test
    void parseJson_blankField_returnsValidResult() {
        field.setValue(BLANK_VALUE);

        FieldValidation.JsonResult result = FieldValidation.parseJson(field, jsonMapper, ERROR_MESSAGE);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void parseJson_blankField_returnsNullValue() {
        field.setValue(BLANK_VALUE);

        FieldValidation.JsonResult result = FieldValidation.parseJson(field, jsonMapper, ERROR_MESSAGE);

        assertThat(result.value()).isNull();
    }

    @Test
    void parseJson_blankField_clearsFieldInvalidState() {
        field.setValue(BLANK_VALUE);
        field.setInvalid(true);

        FieldValidation.parseJson(field, jsonMapper, ERROR_MESSAGE);

        assertThat(field.isInvalid()).isFalse();
    }

    @Test
    void parseJson_emptyField_returnsValidResultWithNullValue() {
        field.setValue(EMPTY_VALUE);

        FieldValidation.JsonResult result = FieldValidation.parseJson(field, jsonMapper, ERROR_MESSAGE);

        assertThat(result.valid()).isTrue();
        assertThat(result.value()).isNull();
    }

    // --- valid JSON ---

    @Test
    void parseJson_validJson_returnsValidResult() {
        field.setValue(VALID_JSON);

        FieldValidation.JsonResult result = FieldValidation.parseJson(field, jsonMapper, ERROR_MESSAGE);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void parseJson_validJson_returnsNonNullParsedValue() {
        field.setValue(VALID_JSON);

        FieldValidation.JsonResult result = FieldValidation.parseJson(field, jsonMapper, ERROR_MESSAGE);

        assertThat(result.value()).isNotNull();
    }

    @Test
    void parseJson_validJson_clearsFieldInvalidState() {
        field.setValue(VALID_JSON);
        field.setInvalid(true);

        FieldValidation.parseJson(field, jsonMapper, ERROR_MESSAGE);

        assertThat(field.isInvalid()).isFalse();
    }

    // --- malformed JSON ---

    @Test
    void parseJson_malformedJson_returnsInvalidResult() {
        field.setValue(INVALID_JSON);

        FieldValidation.JsonResult result = FieldValidation.parseJson(field, jsonMapper, ERROR_MESSAGE);

        assertThat(result.valid()).isFalse();
    }

    @Test
    void parseJson_malformedJson_returnsNullValue() {
        field.setValue(INVALID_JSON);

        FieldValidation.JsonResult result = FieldValidation.parseJson(field, jsonMapper, ERROR_MESSAGE);

        assertThat(result.value()).isNull();
    }

    @Test
    void parseJson_malformedJson_marksFieldInvalid() {
        field.setValue(INVALID_JSON);

        FieldValidation.parseJson(field, jsonMapper, ERROR_MESSAGE);

        assertThat(field.isInvalid()).isTrue();
    }

    @Test
    void parseJson_malformedJson_setsErrorMessageOnField() {
        field.setValue(INVALID_JSON);

        FieldValidation.parseJson(field, jsonMapper, ERROR_MESSAGE);

        assertThat(field.getErrorMessage()).isEqualTo(ERROR_MESSAGE);
    }
}
