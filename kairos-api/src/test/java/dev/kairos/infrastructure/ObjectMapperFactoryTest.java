package dev.kairos.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ObjectMapperFactory}. Verifies the three settings
 * documented in the factory's Javadoc:
 * <ol>
 *   <li>{@code JavaTimeModule} — {@link Instant} serializes as an ISO-8601
 *       string, not a numeric array.</li>
 *   <li>{@code WRITE_DATES_AS_TIMESTAMPS = false} — timestamps are
 *       human-readable strings, not epoch milliseconds.</li>
 *   <li>{@code FAIL_ON_UNKNOWN_PROPERTIES = false} — extra fields in JSON
 *       are silently ignored during deserialization.</li>
 * </ol>
 */
class ObjectMapperFactoryTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-01-01T00:00:00Z");
    private static final String EXPECTED_ISO_8601 = "2026-01-01T00:00:00Z";
    private static final String INSTANT_JSON_KEY = "ts";
    private static final String KNOWN_FIELD = "name";
    private static final String KNOWN_VALUE = "kairos";

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = ObjectMapperFactory.create();
    }

    // ── Instant serialization ────────────────────────────────────────────────

    @Test
    void create_instantSerializesAsIso8601String() throws Exception {
        record Wrapper(Instant ts) {}
        Wrapper wrapper = new Wrapper(FIXED_INSTANT);

        JsonNode node = objectMapper.valueToTree(wrapper);

        assertTrue(node.get(INSTANT_JSON_KEY).isTextual(),
                "Instant must serialize as a text node, not a numeric/array node");
    }

    @Test
    void create_instantSerializesWithExpectedValue() throws Exception {
        record Wrapper(Instant ts) {}
        Wrapper wrapper = new Wrapper(FIXED_INSTANT);

        JsonNode node = objectMapper.valueToTree(wrapper);

        assertEquals(EXPECTED_ISO_8601, node.get(INSTANT_JSON_KEY).asText());
    }

    @Test
    void create_instantDeserializesFromIso8601String() throws Exception {
        String json = "{\"ts\":\"" + EXPECTED_ISO_8601 + "\"}";
        record Wrapper(Instant ts) {}

        Wrapper result = objectMapper.readValue(json, Wrapper.class);

        assertEquals(FIXED_INSTANT, result.ts());
    }

    // ── FAIL_ON_UNKNOWN_PROPERTIES = false ───────────────────────────────────

    @Test
    void create_unknownPropertyInJson_isIgnoredWithoutException() throws Exception {
        String json = """
                {
                  "name": "%s",
                  "unknownField": "should be ignored",
                  "anotherUnknown": 42
                }
                """.formatted(KNOWN_VALUE);
        record KnownOnly(String name) {}

        KnownOnly result = assertDoesNotThrow(
                () -> objectMapper.readValue(json, KnownOnly.class),
                "Unknown properties must not cause a deserialization exception");

        assertEquals(KNOWN_VALUE, result.name());
    }
}
