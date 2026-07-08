package dev.kairos.admin.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Renders task payloads as display strings for read-only UI fields.
 * Serialization never propagates: a malformed payload falls back to its
 * {@code toString()} so a dialog still opens instead of failing to construct.
 */
public final class JsonText {

    private static final Logger logger = LoggerFactory.getLogger(JsonText.class);

    private static final String EMPTY = "";

    private JsonText() {
    }

    /**
     * Serializes {@code payload} to a JSON string for display, or {@code ""}
     * when it is {@code null}. On a serialization error the raw
     * {@code String.valueOf(payload)} is returned and the failure is logged.
     */
    public static String forDisplay(JsonMapper jsonMapper, Object payload) {
        if (payload == null) {
            return EMPTY;
        }
        try {
            return jsonMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            logger.warn("Failed to serialize payload for display", ex);
            return String.valueOf(payload);
        }
    }
}
