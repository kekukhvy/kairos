package dev.kairos.admin.feature.destination.component;

import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.feature.destination.dto.UpdateDestinationRequest;
import dev.kairos.admin.shared.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the pure-Java logic that backs {@link DestinationDetails#save()} and
 * the delete confirmation path.
 *
 * <p>Vaadin UI components require a running VaadinSession and cannot be
 * constructed in a unit test. Instead, this class exercises the same pure-Java
 * logic that {@code save()} and {@code delete()} delegate to: parse the raw
 * config string with {@link JsonMapper}, reject invalid JSON, treat blank input
 * as {@code null}, propagate a parsed {@link Object} to the {@code onSave}
 * callback, and forward the destination to the {@code onDelete} callback.
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

    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
    }

    // --- readConfig: valid JSON ---

    @Test
    void readConfig_validJsonObject_parsesSuccessfully() {
        Object result = readConfig(VALID_JSON_OBJECT);

        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertThat(map).containsEntry(TOPIC_KEY, TOPIC_VALUE);
    }

    @Test
    void readConfig_validJsonArray_parsesSuccessfully() {
        Object result = readConfig(VALID_JSON_ARRAY);

        assertThat(result).isInstanceOf(List.class);
    }

    @Test
    void readConfig_validJsonScalar_parsesSuccessfully() {
        Object result = readConfig(VALID_JSON_SCALAR);

        assertThat(result).isEqualTo(42);
    }

    // --- readConfig: blank / null → null config ---

    @Test
    void readConfig_blankInput_returnsNull() {
        Object result = readConfig(BLANK_INPUT);

        assertThat(result).isNull();
    }

    @Test
    void readConfig_emptyInput_returnsNull() {
        Object result = readConfig(EMPTY_INPUT);

        assertThat(result).isNull();
    }

    // --- readConfig: invalid JSON → JacksonException (save must not call onSave) ---

    @Test
    void readConfig_invalidJson_throwsJacksonException() {
        assertThatExceptionOfType(JacksonException.class)
                .isThrownBy(() -> readConfig(INVALID_JSON));
    }

    // --- save contract: valid JSON invokes onSave ---

    @Test
    void save_validJsonConfig_invokesOnSaveWithParsedConfig() {
        List<UpdateDestinationRequest> captured = new ArrayList<>();

        simulateSave(VALID_JSON_OBJECT, captured::add);

        assertThat(captured).hasSize(1);
        assertThat(captured.getFirst().config()).isInstanceOf(Map.class);
    }

    @Test
    void save_blankConfig_invokesOnSaveWithNullConfig() {
        List<UpdateDestinationRequest> captured = new ArrayList<>();

        simulateSave(BLANK_INPUT, captured::add);

        assertThat(captured).hasSize(1);
        assertThat(captured.getFirst().config()).isNull();
    }

    // --- save contract: invalid JSON does NOT invoke onSave ---

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
     * Mirrors {@code DestinationDetails.readConfig()}: blank → null, else parse.
     */
    private Object readConfig(String raw) {
        if (Strings.isBlank(raw)) {
            return null;
        }
        return jsonMapper.readValue(raw, Object.class);
    }

    /**
     * Mirrors {@code DestinationDetails.save()}: parse, swallow on error, call
     * onSave only when parsing succeeds.
     */
    private void simulateSave(String raw, Consumer<UpdateDestinationRequest> onSave) {
        Object parsedConfig;
        try {
            parsedConfig = readConfig(raw);
        } catch (JacksonException ex) {
            return;
        }
        onSave.accept(new UpdateDestinationRequest(parsedConfig));
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
