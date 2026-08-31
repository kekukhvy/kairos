package dev.kairos.application.destination.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.common.destination.DestinationConfigSchema;
import dev.kairos.common.destination.DestinationType;

import java.util.Set;

import static dev.kairos.common.util.helpers.JsonConverter.topLevelKeys;

/**
 * Validates a raw destination {@code config} JSON string against the per-type
 * {@link DestinationConfigSchema}: parses the config into its top-level key set
 * and checks that every key the type requires is present.
 *
 * <p>Kept in the application layer (not domain) because it uses Jackson to
 * parse JSON — the domain stays framework-free and only ever sees the
 * resulting validated config string.
 */
final class DestinationConfigValidator {

    private DestinationConfigValidator() {
        // utility class — not instantiable
    }

    /**
     * @throws dev.kairos.common.exceptions.ValidationException if {@code config} is
     *         not a JSON object, or omits a key required for {@code type}
     */
    static void validateConfigSchema(DestinationType type, String config, ObjectMapper objectMapper) {
        Set<String> presentKeys = topLevelKeys(config, objectMapper);
        DestinationConfigSchema.validate(type, presentKeys);
    }
}
