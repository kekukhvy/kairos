package dev.kairos.application.destination.usecases;

import dev.kairos.common.destination.DestinationType;
import dev.kairos.common.exceptions.ValidationException;
import dev.kairos.domain.destination.ConfigKeyReader;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static dev.kairos.application.destination.usecases.DestinationConfigValidator.validateConfigSchema;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link DestinationConfigValidator}, driven entirely through
 * the {@link ConfigKeyReader} port — no Jackson/JSON library involved, proving
 * the validator itself has no framework dependency.
 */
class DestinationConfigValidatorTest {

    private static final String CONFIG = "{\"topic\":\"payments\"}";

    @Test
    void validateConfigSchema_withAllRequiredKeysPresent_doesNotThrow() {
        ConfigKeyReader reader = config -> Set.of("topic");

        assertDoesNotThrow(() -> validateConfigSchema(DestinationType.KAFKA, CONFIG, reader));
    }

    @Test
    void validateConfigSchema_withMissingRequiredKey_throwsValidationException() {
        ConfigKeyReader reader = config -> Set.of();

        assertThrows(ValidationException.class,
                () -> validateConfigSchema(DestinationType.KAFKA, CONFIG, reader));
    }

    @Test
    void validateConfigSchema_withMissingRequiredKey_messageNamesMissingKey() {
        ConfigKeyReader reader = config -> Set.of();

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validateConfigSchema(DestinationType.KAFKA, CONFIG, reader));

        assertTrue(ex.getMessage().contains("topic"));
    }

    @Test
    void validateConfigSchema_delegatesConfigStringToReader() {
        String[] receivedConfig = new String[1];
        ConfigKeyReader reader = config -> {
            receivedConfig[0] = config;
            return Set.of("topic");
        };

        validateConfigSchema(DestinationType.KAFKA, CONFIG, reader);

        assertTrue(CONFIG.equals(receivedConfig[0]));
    }

    @Test
    void validateConfigSchema_whenReaderRejectsConfig_propagatesValidationException() {
        ConfigKeyReader reader = config -> {
            throw new ValidationException("config is not valid JSON");
        };

        assertThrows(ValidationException.class,
                () -> validateConfigSchema(DestinationType.KAFKA, CONFIG, reader));
    }
}
