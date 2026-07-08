package dev.kairos.common.util.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.common.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
