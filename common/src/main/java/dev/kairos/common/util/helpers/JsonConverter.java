package dev.kairos.common.util.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.common.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class JsonConverter {

    private static final Logger logger = LoggerFactory.getLogger(JsonConverter.class);

    /**
     * Serializes a {@link JsonNode} (from the request body) back to a JSON
     * string for storage in the domain. A null or JSON null node becomes null.
     */
    public static String jsonToString(JsonNode node, ObjectMapper objectMapper) throws ValidationException {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new ValidationException("payload is not valid JSON: " + e.getMessage());
        }
    }

    /**
     * Converts the stored JSON string back to a {@link JsonNode} for embedding in
     * the response. Falls back to a text node if the string is somehow not valid
     * JSON (guard against corrupt stored data).
     */
    public static JsonNode parseJson(String payload, ObjectMapper objectMapper) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            logger.warn("Stored payload is not valid JSON; falling back to raw text node: {}", e.getMessage());
            return objectMapper.getNodeFactory().textNode(payload);
        }
    }

    /**
     * Parses a JSON string and returns its top-level field names, for use with
     * {@code dev.kairos.common.destination.DestinationConfigSchema#validate}.
     *
     * @param payload      raw JSON string (expected to be a JSON object)
     * @param objectMapper mapper used to parse {@code payload}
     * @return the set of top-level key names present in {@code payload}
     * @throws ValidationException if {@code payload} is not valid JSON, or is
     *                             valid JSON but not a JSON object (e.g. an array or scalar)
     */
    public static Set<String> topLevelKeys(String payload, ObjectMapper objectMapper) {
        if (payload == null || payload.isBlank()) {
            throw new ValidationException("config is required");
        }
        JsonNode node;
        try {
            node = objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            // Message stays opaque — matching the JacksonException handler — so a
            // parse error never echoes a fragment of the submitted payload back.
            logger.debug("Rejected config: not valid JSON: {}", e.getMessage());
            throw new ValidationException("config is not valid JSON");
        }
        if (node == null || !node.isObject()) {
            throw new ValidationException("config must be a JSON object");
        }
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(node.fieldNames(), 0), false)
                .collect(Collectors.toUnmodifiableSet());
    }
}
