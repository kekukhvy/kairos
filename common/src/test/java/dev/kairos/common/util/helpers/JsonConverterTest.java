package dev.kairos.common.util.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.common.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static dev.kairos.common.util.helpers.JsonConverter.topLevelKeys;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link JsonConverter#topLevelKeys(String, ObjectMapper)},
 * the bridge between a raw config JSON string and
 * {@code dev.kairos.common.destination.DestinationConfigSchema#validate}.
 */
class JsonConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void topLevelKeys_withJsonObject_returnsFieldNames() {
        Set<String> keys = topLevelKeys("{\"topic\":\"payments\",\"key\":\"k1\"}", objectMapper);

        assertEquals(Set.of("topic", "key"), keys);
    }

    @Test
    void topLevelKeys_withEmptyObject_returnsEmptySet() {
        Set<String> keys = topLevelKeys("{}", objectMapper);

        assertEquals(Set.of(), keys);
    }

    @Test
    void topLevelKeys_withNullPayload_throwsValidationException() {
        assertThrows(ValidationException.class, () -> topLevelKeys(null, objectMapper));
    }

    @Test
    void topLevelKeys_withBlankPayload_throwsValidationException() {
        assertThrows(ValidationException.class, () -> topLevelKeys("   ", objectMapper));
    }

    @Test
    void topLevelKeys_withJsonArray_throwsValidationException() {
        assertThrows(ValidationException.class, () -> topLevelKeys("[\"topic\"]", objectMapper));
    }

    @Test
    void topLevelKeys_withJsonScalar_throwsValidationException() {
        assertThrows(ValidationException.class, () -> topLevelKeys("\"topic\"", objectMapper));
    }

    @Test
    void topLevelKeys_withMalformedJson_throwsValidationException() {
        assertThrows(ValidationException.class, () -> topLevelKeys("{not json", objectMapper));
    }
}
