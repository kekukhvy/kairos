package dev.kairos.common.destination;

import dev.kairos.common.exceptions.ValidationException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Single source of truth for the shape of a destination's {@code config}:
 * which JSON object keys are required vs. optional for each
 * {@link DestinationType}.
 *
 * <p>Framework-free by design (no JSON parsing here): callers parse the raw
 * config JSON into its top-level key set and pass that {@link Set} to
 * {@link #validate(DestinationType, Set)}. This lets both {@code kairos-api}
 * (Jackson 2) and {@code kairos-admin} (Jackson 3) share the exact same key
 * names and template strings without either depending on the other or
 * re-declaring the keys.
 *
 * <p>Only key <em>presence</em> is enforced — values are never inspected —
 * and keys beyond required ∪ optional are accepted untouched (adapters may
 * read custom params later).
 */
public final class DestinationConfigSchema {

    private static final String KEY_TOPIC = "topic";
    private static final String KEY_KEY = "key";
    private static final String KEY_HEADERS = "headers";

    private static final String KEY_QUEUE_URL = "queueUrl";
    private static final String KEY_MESSAGE_GROUP_ID = "messageGroupId";

    private static final String KEY_URL = "url";
    private static final String KEY_METHOD = "method";

    private static final String KEY_EXCHANGE = "exchange";
    private static final String KEY_ROUTING_KEY = "routingKey";

    // Every entry is an ordered set: template() renders keys in iteration order,
    // and Set.of(...) does not guarantee one (it randomises per JVM run).
    private static final Map<DestinationType, Set<String>> REQUIRED_KEYS = Map.of(
            DestinationType.KAFKA, orderedSet(KEY_TOPIC),
            DestinationType.SQS, orderedSet(KEY_QUEUE_URL),
            DestinationType.WEBHOOK, orderedSet(KEY_URL),
            DestinationType.RABBITMQ, orderedSet(KEY_EXCHANGE, KEY_ROUTING_KEY)
    );

    private static final Map<DestinationType, Set<String>> OPTIONAL_KEYS = Map.of(
            DestinationType.KAFKA, orderedSet(KEY_KEY, KEY_HEADERS),
            DestinationType.SQS, orderedSet(KEY_MESSAGE_GROUP_ID),
            DestinationType.WEBHOOK, orderedSet(KEY_METHOD, KEY_HEADERS),
            DestinationType.RABBITMQ, orderedSet(KEY_HEADERS)
    );

    static {
        // A new DestinationType without a schema entry would otherwise surface as
        // an NPE deep inside validate()/template() — fail at class-load instead.
        for (DestinationType type : DestinationType.values()) {
            if (!REQUIRED_KEYS.containsKey(type) || !OPTIONAL_KEYS.containsKey(type)) {
                throw new IllegalStateException("no config schema declared for destination type " + type);
            }
        }
    }

    private DestinationConfigSchema() {
        // utility class — not instantiable
    }

    /** The set of key names a {@code config} for {@code type} must contain. */
    public static Set<String> requiredKeys(DestinationType type) {
        return REQUIRED_KEYS.get(type);
    }

    /** The set of key names a {@code config} for {@code type} may optionally contain. */
    public static Set<String> optionalKeys(DestinationType type) {
        return OPTIONAL_KEYS.get(type);
    }

    /**
     * Validates that every required key for {@code type} is present in
     * {@code presentKeys}. Extra keys beyond required ∪ optional are allowed.
     *
     * @param type        the destination type whose schema to validate against
     * @param presentKeys the top-level key names found in the submitted config
     * @throws ValidationException naming ALL missing required keys, when at least one is absent
     */
    public static void validate(DestinationType type, Set<String> presentKeys) {
        Set<String> missing = requiredKeys(type).stream()
                .filter(key -> !presentKeys.contains(key))
                .collect(Collectors.toCollection(TreeSet::new));

        if (!missing.isEmpty()) {
            throw new ValidationException(
                    "config is missing required key(s): " + String.join(", ", missing));
        }
    }

    /**
     * Builds the JSON template for {@code type}: an object with one empty-string
     * entry per required key, in declaration order. The template itself is a
     * valid config (only key presence is checked, not value content).
     */
    public static String template(DestinationType type) {
        return requiredKeys(type).stream()
                .map(key -> "\"" + key + "\": \"\"")
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static Set<String> orderedSet(String... keys) {
        return new LinkedHashSet<>(List.of(keys));
    }
}
