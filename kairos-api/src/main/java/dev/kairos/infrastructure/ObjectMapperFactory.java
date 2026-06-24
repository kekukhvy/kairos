package dev.kairos.infrastructure;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Produces a shared, fully configured {@link ObjectMapper}. One instance is
 * created at startup and injected wherever JSON serialization is needed.
 *
 * <p>Settings:
 * <ul>
 *   <li>{@code JavaTimeModule} — serializes {@link java.time.Instant} as ISO-8601
 *       string instead of an array of fields.</li>
 *   <li>{@code WRITE_DATES_AS_TIMESTAMPS = false} — human-readable timestamps.</li>
 *   <li>{@code FAIL_ON_UNKNOWN_PROPERTIES = false} — forward-compatible requests;
 *       extra fields from newer clients are silently ignored.</li>
 * </ul>
 */
public final class ObjectMapperFactory {

    private ObjectMapperFactory() {
    }

    public static ObjectMapper create() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}