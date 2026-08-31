package dev.kairos.infrastructure.destination;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kairos.common.util.helpers.JsonConverter;
import dev.kairos.domain.destination.ConfigKeyReader;

import java.util.Objects;
import java.util.Set;

/**
 * Jackson-backed {@link ConfigKeyReader}: the only place in the API layer's
 * destination wiring that touches JSON parsing directly, so the application
 * layer (see {@code dev.kairos.application.destination.usecases}) can depend
 * on the {@link ConfigKeyReader} port instead of Jackson.
 */
public final class JacksonConfigKeyReader implements ConfigKeyReader {

    private final ObjectMapper objectMapper;

    public JacksonConfigKeyReader(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "'objectMapper' must not be null.");
    }

    @Override
    public Set<String> topLevelKeys(String config) {
        return JsonConverter.topLevelKeys(config, objectMapper);
    }
}
