package dev.kairos.domain.destination;

import java.util.Set;

/**
 * Port for extracting the top-level key set out of a destination's raw
 * {@code config} JSON string. Kept framework-free so the application layer
 * can validate a config's shape against
 * {@code dev.kairos.common.destination.DestinationConfigSchema} without
 * knowing which JSON library parses it — the implementation (e.g.
 * Jackson-backed) lives in the infrastructure layer.
 */
public interface ConfigKeyReader {

    /**
     * @param config raw JSON string (expected to be a JSON object)
     * @return the set of top-level key names present in {@code config}
     * @throws dev.kairos.common.exceptions.ValidationException if {@code config}
     *         is not valid JSON, or is valid JSON but not a JSON object
     */
    Set<String> topLevelKeys(String config);
}
