package dev.kairos.admin.shared.form;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValidation;
import dev.kairos.admin.shared.util.Strings;

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
