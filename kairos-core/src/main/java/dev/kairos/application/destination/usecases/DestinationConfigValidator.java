package dev.kairos.application.destination.usecases;

import dev.kairos.common.destination.DestinationConfigSchema;
import dev.kairos.common.destination.DestinationType;
import dev.kairos.domain.destination.ConfigKeyReader;

import java.util.Set;

/**
 * Validates a raw destination {@code config} JSON string against the per-type
 * {@link DestinationConfigSchema}: extracts the config's top-level key set via
 * the {@link ConfigKeyReader} port and checks that every key the type requires
 * is present.
 *
 * <p>Framework-free: JSON parsing is delegated to {@link ConfigKeyReader},
 * whose implementation lives in the infrastructure layer, so this class (and
 * the rest of the application layer) never imports a JSON library directly.
 */
final class DestinationConfigValidator {

    private DestinationConfigValidator() {
        // utility class — not instantiable
    }

    /**
     * @throws dev.kairos.common.exceptions.ValidationException if {@code config} is
     *         not a JSON object, or omits a key required for {@code type}
     */
    static void validateConfigSchema(DestinationType type, String config, ConfigKeyReader configKeyReader) {
        Set<String> presentKeys = configKeyReader.topLevelKeys(config);
        DestinationConfigSchema.validate(type, presentKeys);
    }
}
