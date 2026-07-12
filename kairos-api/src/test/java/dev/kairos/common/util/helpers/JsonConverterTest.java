package dev.kairos.common.util.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.common.exceptions.ValidationException;
import dev.kairos.infrastructure.ObjectMapperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonConverter#jsonToString}. Covers all three branches:
 * <ol>
 *   <li>null node → null</li>
 *   <li>JSON-null node → null</li>
 *   <li>valid JSON node → serialized JSON string</li>
 * </ol>
 * The fourth branch (processing error → {@link ValidationException}) cannot be
 * triggered with a real ObjectMapper under normal conditions because any
 * well-formed JsonNode serializes successfully. This is a deliberate defensive
 * guard documented in the source; its absence from tests is noted.
 */
class JsonConverterTest {

    private static final String EXPECTED_JSON_STRING = "{\"key\":\"value\"}";
    private static final String KEY_FIELD = "key";
    private static final String KEY_VALUE = "value";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = ObjectMapperFactory.create();
    }

    // ── null node → null ─────────────────────────────────────────────────────

    @Test
    void jsonToString_withNullNode_returnsNull() {
        String result = JsonConverter.jsonToString(null, objectMapper);

        assertNull(result);
    }

    // ── JSON-null node → null ────────────────────────────────────────────────

    @Test
    void jsonToString_withJsonNullNode_returnsNull() {
        JsonNode nullNode = objectMapper.nullNode();

        String result = JsonConverter.jsonToString(nullNode, objectMapper);

        assertNull(result);
    }

    // ── valid node → JSON string ─────────────────────────────────────────────

    @Test
    void jsonToString_withObjectNode_returnsJsonString() throws Exception {
        JsonNode node = objectMapper.readTree(EXPECTED_JSON_STRING);

        String result = JsonConverter.jsonToString(node, objectMapper);

        assertNotNull(result);
        assertEquals(EXPECTED_JSON_STRING, result);
    }

    @Test
    void jsonToString_withObjectNode_resultIsParseable() throws Exception {
        JsonNode node = objectMapper.readTree(EXPECTED_JSON_STRING);

        String result = JsonConverter.jsonToString(node, objectMapper);

        JsonNode reparsed = objectMapper.readTree(result);
        assertEquals(KEY_VALUE, reparsed.get(KEY_FIELD).asText());
    }

    @Test
    void jsonToString_withTextNode_returnsQuotedString() {
        JsonNode textNode = objectMapper.getNodeFactory().textNode("hello");

        String result = JsonConverter.jsonToString(textNode, objectMapper);

        assertEquals("\"hello\"", result);
    }

    @Test
    void jsonToString_withNumberNode_returnsNumberString() {
        JsonNode numberNode = objectMapper.getNodeFactory().numberNode(42);

        String result = JsonConverter.jsonToString(numberNode, objectMapper);

        assertEquals("42", result);
    }

    @Test
    void jsonToString_withArrayNode_returnsJsonArrayString() throws Exception {
        JsonNode arrayNode = objectMapper.readTree("[1,2,3]");

        String result = JsonConverter.jsonToString(arrayNode, objectMapper);

        assertEquals("[1,2,3]", result);
    }
}
