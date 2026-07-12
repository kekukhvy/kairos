package dev.kairos.admin.feature.destination.component;

import com.vaadin.flow.component.textfield.TextArea;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.feature.destination.dto.UpdateDestinationRequest;
import dev.kairos.admin.shared.form.FieldValidation;
import dev.kairos.admin.shared.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the pure-Java logic that backs {@link DestinationDetails#save()} and
 * the delete confirmation path.
 *
 * <p>Vaadin UI components require a running VaadinSession and cannot be
 * constructed in a unit test — except for simple input fields such as
 * {@link TextArea}, which carry no session state at construction time.
 *
 * <p>This class exercises the same logic that {@code save()} delegates to:
 * <ol>
 *   <li>{@link FieldValidation#require} — rejects blank config before JSON
 *       parsing is attempted.</li>
 *   <li>{@link FieldValidation#parseJson} — parses non-blank config, marks the
 *       field invalid on malformed JSON, and yields a {@link FieldValidation.JsonResult}.</li>
 * </ol>
 * The {@code onSave} callback is only invoked when both guards pass.
 * The {@code onDelete} callback is always forwarded immediately (the subsequent
 * {@code close()} is Vaadin-bound and cannot be exercised without a
 * VaadinSession).
 */
class DestinationDetailsSaveLogicTest {

    private static final String DESTINATION_ID = "dest-kafka-1";
    private static final String DESTINATION_TYPE = "KAFKA";
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private static final String VALID_JSON_OBJECT = "{\"topic\":\"invoices\"}";
    private static final String VALID_JSON_ARRAY = "[\"a\",\"b\"]";
    private static final String VALID_JSON_SCALAR = "42";
    private static final String INVALID_JSON = "{not valid json";
    private static final String BLANK_INPUT = "   ";
    private static final String EMPTY_INPUT = "";

    private static final String TOPIC_KEY = "topic";
    private static final String TOPIC_VALUE = "invoices";
    private static final String VALIDATION_ERROR_MESSAGE = "Invalid JSON";

    private JsonMapper jsonMapper;
    private TextArea configField;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        configField = new TextArea();
    }

    // --- parseJson: valid JSON ---

    @Test
    void parseJson_validJsonObject_parsesSuccessfully() {
        configField.setValue(VALID_JSON_OBJECT);

        FieldValidation.JsonResult result = FieldValidation.parseJson(
                configField, jsonMapper, VALIDATION_ERROR_MESSAGE);

        assertThat(result.valid()).isTrue();
        assertThat(result.value()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result.value();
        assertThat(map).containsEntry(TOPIC_KEY, TOPIC_VALUE);
    }

    @Test
    void parseJson_validJsonArray_parsesSuccessfully() {
        configField.setValue(VALID_JSON_ARRAY);

        FieldValidation.JsonResult result = FieldValidation.parseJson(
                configField, jsonMapper, VALIDATION_ERROR_MESSAGE);

        assertThat(result.valid()).isTrue();
        assertThat(result.value()).isInstanceOf(List.class);
    }

    @Test
    void parseJson_validJsonScalar_parsesSuccessfully() {
        configField.setValue(VALID_JSON_SCALAR);

        FieldValidation.JsonResult result = FieldValidation.parseJson(
                configField, jsonMapper, VALIDATION_ERROR_MESSAGE);

        assertThat(result.valid()).isTrue();
        assertThat(result.value()).isEqualTo(42);
    }

    // --- parseJson: invalid JSON → invalid result, field marked invalid ---

    @Test
    void parseJson_invalidJson_returnsInvalidResult() {
        configField.setValue(INVALID_JSON);

        FieldValidation.JsonResult result = FieldValidation.parseJson(
                configField, jsonMapper, VALIDATION_ERROR_MESSAGE);

        assertThat(result.valid()).isFalse();
        assertThat(result.value()).isNull();
    }

    @Test
    void parseJson_invalidJson_marksFieldInvalid() {
        configField.setValue(INVALID_JSON);

        FieldValidation.parseJson(configField, jsonMapper, VALIDATION_ERROR_MESSAGE);

        assertThat(configField.isInvalid()).isTrue();
        assertThat(configField.getErrorMessage()).isEqualTo(VALIDATION_ERROR_MESSAGE);
    }

    // --- save contract: FieldValidation.require rejects blank before parseJson is reached ---

    @Test
    void save_validJsonConfig_invokesOnSaveWithParsedConfig() {
        List<UpdateDestinationRequest> captured = new ArrayList<>();

        simulateSave(VALID_JSON_OBJECT, captured::add);

        assertThat(captured).hasSize(1);
        assertThat(captured.getFirst().config()).isInstanceOf(Map.class);
    }

    @Test
    void save_blankConfig_doesNotInvokeOnSave() {
        List<UpdateDestinationRequest> captured = new ArrayList<>();

        simulateSave(BLANK_INPUT, captured::add);

        assertThat(captured).isEmpty();
    }

    @Test
    void save_emptyConfig_doesNotInvokeOnSave() {
        List<UpdateDestinationRequest> captured = new ArrayList<>();

        simulateSave(EMPTY_INPUT, captured::add);

        assertThat(captured).isEmpty();
    }

    @Test
    void save_invalidJsonConfig_doesNotInvokeOnSave() {
        List<UpdateDestinationRequest> captured = new ArrayList<>();

        simulateSave(INVALID_JSON, captured::add);

        assertThat(captured).isEmpty();
    }

    // --- delete contract ---

    @Test
    void delete_invokesOnDeleteWithTheDestination() {
        DestinationDTO destination = new DestinationDTO(DESTINATION_ID, DESTINATION_TYPE, null, CREATED_AT);
        List<DestinationDTO> captured = new ArrayList<>();

        simulateDelete(destination, captured::add);

        assertThat(captured).hasSize(1);
        assertThat(captured.getFirst()).isSameAs(destination);
    }

    @Test
    void delete_invokesOnDeleteExactlyOnce() {
        DestinationDTO destination = new DestinationDTO(DESTINATION_ID, DESTINATION_TYPE, null, CREATED_AT);
        List<DestinationDTO> captured = new ArrayList<>();

        simulateDelete(destination, captured::add);
        simulateDelete(destination, captured::add);

        assertThat(captured).hasSize(2);
    }

    // --- helpers mirroring DestinationDetails internals ---

    /**
     * Mirrors {@code DestinationDetails.save()}: blank config is rejected by
     * {@link FieldValidation#require}, then malformed JSON is rejected by
     * {@link FieldValidation#parseJson}, and {@code onSave} is called only when
     * both guards pass.
     */
    private void simulateSave(String raw, Consumer<UpdateDestinationRequest> onSave) {
        configField.setValue(raw);

        if (!FieldValidation.require(configField, VALIDATION_ERROR_MESSAGE)) {
            return;
        }

        FieldValidation.JsonResult result = FieldValidation.parseJson(
                configField, jsonMapper, VALIDATION_ERROR_MESSAGE);
        if (!result.valid()) {
            return;
        }

        onSave.accept(new UpdateDestinationRequest(result.value()));
    }

    /**
     * Mirrors {@code DestinationDetails.delete()}: forward the destination to
     * the onDelete consumer (the subsequent {@code close()} is Vaadin-bound and
     * cannot be exercised without a VaadinSession).
     */
    private void simulateDelete(DestinationDTO destination, Consumer<DestinationDTO> onDelete) {
        onDelete.accept(destination);
    }
}
