package dev.kairos.admin.feature.destination;

import com.vaadin.flow.component.textfield.TextArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the shared "type selection prefills config template" logic
 * used by both {@code DestinationForm} and {@code DestinationStep}.
 */
class ConfigTemplatePrefillTest {

    private TextArea configField;
    private ConfigTemplatePrefill prefill;

    @BeforeEach
    void setUp() {
        configField = new TextArea();
        prefill = new ConfigTemplatePrefill(configField);
    }

    @Test
    void onTypeSelected_blankField_appliesTemplate() {
        prefill.onTypeSelected("KAFKA");

        assertThat(configField.getValue()).isEqualTo("{\"topic\": \"\"}");
    }

    @Test
    void onTypeSelected_nullType_doesNothing() {
        configField.setValue("existing");

        prefill.onTypeSelected(null);

        assertThat(configField.getValue()).isEqualTo("existing");
    }

    @Test
    void onTypeSelected_fieldStillHoldsPreviousTemplate_replacesIt() {
        prefill.onTypeSelected("KAFKA");

        prefill.onTypeSelected("SQS");

        assertThat(configField.getValue()).isEqualTo("{\"queueUrl\": \"\"}");
    }

    @Test
    void onTypeSelected_fieldEditedByUser_doesNotOverwrite() {
        prefill.onTypeSelected("KAFKA");
        configField.setValue("{\"topic\": \"payments\"}");

        prefill.onTypeSelected("SQS");

        assertThat(configField.getValue()).isEqualTo("{\"topic\": \"payments\"}");
    }

    @Test
    void onTypeSelected_fieldClearedAfterTemplateApplied_reappliesNewTemplate() {
        prefill.onTypeSelected("KAFKA");
        configField.setValue("");

        prefill.onTypeSelected("SQS");

        assertThat(configField.getValue()).isEqualTo("{\"queueUrl\": \"\"}");
    }
}
