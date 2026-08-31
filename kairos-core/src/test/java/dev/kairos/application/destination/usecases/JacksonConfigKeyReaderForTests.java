package dev.kairos.application.destination.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.common.util.helpers.JsonConverter;
import dev.kairos.domain.destination.ConfigKeyReader;

import java.util.Set;

/**
 * Test-only {@link ConfigKeyReader}, backed by the same Jackson-driven
 * {@link JsonConverter#topLevelKeys} used by the real infrastructure
 * implementation (see {@code dev.kairos.infrastructure.destination.JacksonConfigKeyReader}
 * in {@code kairos-api}), so use-case tests exercise real JSON parsing
 * behavior without the application layer itself depending on Jackson.
 */
final class JacksonConfigKeyReaderForTests implements ConfigKeyReader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Set<String> topLevelKeys(String config) {
        return JsonConverter.topLevelKeys(config, objectMapper);
    }
}
