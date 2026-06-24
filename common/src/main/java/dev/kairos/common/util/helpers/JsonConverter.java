package dev.kairos.common.util.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.common.exceptions.ValidationException;

public final class JsonConverter {

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
}
