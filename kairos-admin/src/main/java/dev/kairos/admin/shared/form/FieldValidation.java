package dev.kairos.admin.shared.form;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.textfield.TextArea;
import dev.kairos.admin.shared.util.Strings;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reusable field-level validation. Error messages are passed in by the caller
 * so feature-specific text never leaks into shared code.
 *
 * <pre>
 * boolean ok = FieldValidation.require(name, MyText.REQUIRED);
 * ok &amp;= FieldValidation.require(service, MyText.REQUIRED);
 * </pre>
 */
public final class FieldValidation {

    private FieldValidation() {
    }

    /**
     * Outcome of {@link #parseJson}: whether the field held valid JSON and, when
     * it did, the parsed value ({@code null} for a blank field).
     */
    public record JsonResult(boolean valid, Object value) {
    }

    /**
     * Parses a text area's content as JSON. A blank field is valid and yields a
     * {@code null} value. On malformed JSON the field is marked invalid with
     * {@code errorMessage} and the result is {@link JsonResult#valid() invalid}.
     *
     * @param field        the text area whose value is parsed
     * @param mapper       Jackson mapper used to deserialise the raw JSON string
     * @param errorMessage error message set on the field when the JSON is malformed
     * @return a {@link JsonResult} indicating validity and, on success, the parsed value
     */
    public static JsonResult parseJson(TextArea field, JsonMapper mapper, String errorMessage) {
        String raw = field.getValue();
        if (Strings.isBlank(raw)) {
            field.setInvalid(false);
            return new JsonResult(true, null);
        }
        try {
            Object parsed = mapper.readValue(raw, Object.class);
            field.setInvalid(false);
            return new JsonResult(true, parsed);
        } catch (JacksonException ex) {
            field.setInvalid(true);
            field.setErrorMessage(errorMessage);
            return new JsonResult(false, null);
        }
    }

    /**
     * Marks a text field invalid when it is empty or blank.
     *
     * @return {@code true} when the field holds a non-blank value
     */
    public static <F extends HasValue<?, String> & HasValidation> boolean require(
            F field, String errorMessage) {
        return check(field, !Strings.isBlank(field.getValue()), errorMessage);
    }

    /**
     * Marks any field invalid when it currently has no value.
     *
     * @return {@code true} when the field holds a value
     */
    public static boolean requirePresent(
            HasValue<?, ?> field, HasValidation feedback, String errorMessage) {
        return check(feedback, !field.isEmpty(), errorMessage);
    }

    private static boolean check(HasValidation field, boolean valid, String errorMessage) {
        field.setInvalid(!valid);
        if (!valid) {
            field.setErrorMessage(errorMessage);
        }
        return valid;
    }
}
