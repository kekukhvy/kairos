package dev.kairos.admin.shared.form;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FieldValidation#parseJson} and
 * {@link FieldValidation#requireJson}.
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
    private static final String REQUIRED_MESSAGE = "Required";

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

    // --- requireJson: mandatory JSON, blank is rejected ---

    @Test
    void requireJson_blankField_returnsInvalidResult() {
        field.setValue(BLANK_VALUE);

        FieldValidation.JsonResult result =
                FieldValidation.requireJson(field, jsonMapper, REQUIRED_MESSAGE, ERROR_MESSAGE);

        assertThat(result.valid()).isFalse();
        assertThat(result.value()).isNull();
    }

    /**
     * The bug this method exists to prevent: {@code require} marks the blank
     * field invalid, and a following {@code parseJson} would clear that flag
     * (blank parses as valid JSON), leaving the field silently error-free.
     */
    @Test
    void requireJson_blankField_leavesFieldMarkedInvalidWithRequiredMessage() {
        field.setValue(BLANK_VALUE);

        FieldValidation.requireJson(field, jsonMapper, REQUIRED_MESSAGE, ERROR_MESSAGE);

        assertThat(field.isInvalid()).isTrue();
        assertThat(field.getErrorMessage()).isEqualTo(REQUIRED_MESSAGE);
    }

    @Test
    void requireJson_emptyField_leavesFieldMarkedInvalid() {
        field.setValue(EMPTY_VALUE);

        FieldValidation.requireJson(field, jsonMapper, REQUIRED_MESSAGE, ERROR_MESSAGE);

        assertThat(field.isInvalid()).isTrue();
        assertThat(field.getErrorMessage()).isEqualTo(REQUIRED_MESSAGE);
    }

    @Test
    void requireJson_malformedJson_marksFieldInvalidWithJsonMessage() {
        field.setValue(INVALID_JSON);

        FieldValidation.JsonResult result =
                FieldValidation.requireJson(field, jsonMapper, REQUIRED_MESSAGE, ERROR_MESSAGE);

        assertThat(result.valid()).isFalse();
        assertThat(field.isInvalid()).isTrue();
        assertThat(field.getErrorMessage()).isEqualTo(ERROR_MESSAGE);
    }

    @Test
    void requireJson_validJson_returnsParsedValueAndClearsError() {
        field.setValue(VALID_JSON);

        FieldValidation.JsonResult result =
                FieldValidation.requireJson(field, jsonMapper, REQUIRED_MESSAGE, ERROR_MESSAGE);

        assertThat(result.valid()).isTrue();
        assertThat(result.value()).isNotNull();
        assertThat(field.isInvalid()).isFalse();
    }

    // --- uniqueServiceName ---

    private static final String DUPLICATE_MESSAGE = "A task with this service and name already exists.";

    @Test
    void uniqueServiceName_matchingKeyInTakenSet_marksNameFieldInvalid() {
        TextField service = new TextField();
        TextField name = new TextField();
        service.setValue("billing");
        name.setValue("invoice-sync");
        Set<String> taken = Set.of(FieldValidation.serviceNameKey("billing", "invoice-sync"));

        boolean unique = FieldValidation.uniqueServiceName(service, name, taken, DUPLICATE_MESSAGE);

        assertThat(unique).isFalse();
        assertThat(name.isInvalid()).isTrue();
        assertThat(name.getErrorMessage()).isEqualTo(DUPLICATE_MESSAGE);
    }

    @Test
    void uniqueServiceName_keyNotInTakenSet_returnsTrueAndClearsInvalidState() {
        TextField service = new TextField();
        TextField name = new TextField();
        service.setValue("billing");
        name.setValue("invoice-sync");
        name.setInvalid(true);
        Set<String> taken = Set.of(FieldValidation.serviceNameKey("shipping", "other-task"));

        boolean unique = FieldValidation.uniqueServiceName(service, name, taken, DUPLICATE_MESSAGE);

        assertThat(unique).isTrue();
        assertThat(name.isInvalid()).isFalse();
    }

    @Test
    void uniqueServiceName_blankServiceOrName_returnsTrueWithoutFlagging() {
        TextField service = new TextField();
        TextField name = new TextField();
        service.setValue("");
        name.setValue("invoice-sync");
        Set<String> taken = Set.of(FieldValidation.serviceNameKey("billing", "invoice-sync"));

        boolean unique = FieldValidation.uniqueServiceName(service, name, taken, DUPLICATE_MESSAGE);

        assertThat(unique).isTrue();
        assertThat(name.isInvalid()).isFalse();
    }
}
