package dev.kairos.admin.shared.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonTextTest {

    private static final String EMPTY = "";
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String EXPECTED_JSON = "{\"key\":\"value\"}";

    private JsonMapper realMapper;

    @Mock
    private JsonMapper mockMapper;

    @BeforeEach
    void setUp() {
        realMapper = JsonMapper.builder().build();
    }

    @Test
    void forDisplay_nullPayload_returnsEmptyString() {
        String result = JsonText.forDisplay(realMapper, null);

        assertThat(result).isEqualTo(EMPTY);
    }

    @Test
    void forDisplay_simpleMap_returnsValidJson() {
        Map<String, String> payload = Map.of(KEY, VALUE);

        String result = JsonText.forDisplay(realMapper, payload);

        assertThat(result).isEqualTo(EXPECTED_JSON);
    }

    @Test
    void forDisplay_simpleMap_resultIsRoundTrippable() throws Exception {
        Map<String, String> payload = Map.of(KEY, VALUE);

        String json = JsonText.forDisplay(realMapper, payload);
        @SuppressWarnings("unchecked")
        Map<String, Object> roundTripped = realMapper.readValue(json, Map.class);

        assertThat(roundTripped).containsEntry(KEY, VALUE);
    }

    @Test
    void forDisplay_serializationFailure_doesNotThrow() throws Exception {
        Object payload = new Object();
        when(mockMapper.writeValueAsString(any())).thenThrow(jacksonException());

        String result = JsonText.forDisplay(mockMapper, payload);

        assertThat(result).isNotNull();
    }

    @Test
    void forDisplay_serializationFailure_returnsStringValueOfPayload() throws Exception {
        Object payload = new Object() {
            @Override
            public String toString() {
                return "fallback-representation";
            }
        };
        when(mockMapper.writeValueAsString(any())).thenThrow(jacksonException());

        String result = JsonText.forDisplay(mockMapper, payload);

        assertThat(result).isEqualTo(String.valueOf(payload));
    }

    private static StreamWriteException jacksonException() {
        return new StreamWriteException(null, "simulated serialization failure");
    }
}
