package dev.kairos.common.destination;

import dev.kairos.common.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the single source of truth of per-type destination config
 * shape: required/optional key names and key-presence validation.
 */
class DestinationConfigSchemaTest {

    private static final String KEY_TOPIC = "topic";
    private static final String KEY_KEY = "key";
    private static final String KEY_HEADERS = "headers";
    private static final String KEY_QUEUE_URL = "queueUrl";
    private static final String KEY_MESSAGE_GROUP_ID = "messageGroupId";
    private static final String KEY_URL = "url";
    private static final String KEY_METHOD = "method";
    private static final String KEY_EXCHANGE = "exchange";
    private static final String KEY_ROUTING_KEY = "routingKey";

    // --- requiredKeys ---

    @Test
    void requiredKeys_kafka_isTopic() {
        assertEquals(Set.of(KEY_TOPIC), DestinationConfigSchema.requiredKeys(DestinationType.KAFKA));
    }

    @Test
    void requiredKeys_sqs_isQueueUrl() {
        assertEquals(Set.of(KEY_QUEUE_URL), DestinationConfigSchema.requiredKeys(DestinationType.SQS));
    }

    @Test
    void requiredKeys_webhook_isUrl() {
        assertEquals(Set.of(KEY_URL), DestinationConfigSchema.requiredKeys(DestinationType.WEBHOOK));
    }

    @Test
    void requiredKeys_rabbitmq_isExchangeAndRoutingKey() {
        assertEquals(Set.of(KEY_EXCHANGE, KEY_ROUTING_KEY), DestinationConfigSchema.requiredKeys(DestinationType.RABBITMQ));
    }

    // --- optionalKeys ---

    @Test
    void optionalKeys_kafka_isKeyAndHeaders() {
        assertEquals(Set.of(KEY_KEY, KEY_HEADERS), DestinationConfigSchema.optionalKeys(DestinationType.KAFKA));
    }

    @Test
    void optionalKeys_sqs_isMessageGroupId() {
        assertEquals(Set.of(KEY_MESSAGE_GROUP_ID), DestinationConfigSchema.optionalKeys(DestinationType.SQS));
    }

    @Test
    void optionalKeys_webhook_isMethodAndHeaders() {
        assertEquals(Set.of(KEY_METHOD, KEY_HEADERS), DestinationConfigSchema.optionalKeys(DestinationType.WEBHOOK));
    }

    @Test
    void optionalKeys_rabbitmq_isHeaders() {
        assertEquals(Set.of(KEY_HEADERS), DestinationConfigSchema.optionalKeys(DestinationType.RABBITMQ));
    }

    // --- validate: happy path ---

    @Test
    void validate_kafka_withRequiredKeyPresent_doesNotThrow() {
        assertDoesNotThrow(() -> DestinationConfigSchema.validate(DestinationType.KAFKA, Set.of(KEY_TOPIC)));
    }

    @Test
    void validate_kafka_withRequiredAndOptionalKeys_doesNotThrow() {
        assertDoesNotThrow(() -> DestinationConfigSchema.validate(
                DestinationType.KAFKA, Set.of(KEY_TOPIC, KEY_KEY, KEY_HEADERS)));
    }

    @Test
    void validate_rabbitmq_withBothRequiredKeys_doesNotThrow() {
        assertDoesNotThrow(() -> DestinationConfigSchema.validate(
                DestinationType.RABBITMQ, Set.of(KEY_EXCHANGE, KEY_ROUTING_KEY)));
    }

    @Test
    void validate_withExtraUnknownKey_doesNotThrow() {
        assertDoesNotThrow(() -> DestinationConfigSchema.validate(
                DestinationType.KAFKA, Set.of(KEY_TOPIC, "customParam")));
    }

    // --- validate: missing required keys ---

    @Test
    void validate_kafka_withoutTopic_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> DestinationConfigSchema.validate(DestinationType.KAFKA, Set.of()));
    }

    @Test
    void validate_kafka_withoutTopic_messageNamesMissingKey() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> DestinationConfigSchema.validate(DestinationType.KAFKA, Set.of()));

        assertTrue(ex.getMessage().contains(KEY_TOPIC));
    }

    @Test
    void validate_rabbitmq_withOnlyExchange_messageNamesMissingRoutingKeyOnly() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> DestinationConfigSchema.validate(DestinationType.RABBITMQ, Set.of(KEY_EXCHANGE)));

        assertTrue(ex.getMessage().contains(KEY_ROUTING_KEY));
        assertTrue(!ex.getMessage().contains(KEY_EXCHANGE + ",") && !ex.getMessage().endsWith(KEY_EXCHANGE));
    }

    @Test
    void validate_rabbitmq_withNoKeysPresent_messageNamesAllMissingRequiredKeys() {
        ValidationException ex = assertThrows(ValidationException.class,
                () -> DestinationConfigSchema.validate(DestinationType.RABBITMQ, Set.of()));

        assertTrue(ex.getMessage().contains(KEY_EXCHANGE));
        assertTrue(ex.getMessage().contains(KEY_ROUTING_KEY));
    }

    // --- template ---

    @Test
    void template_kafka_isJsonObjectWithTopicKey() {
        assertEquals("{\"topic\": \"\"}", DestinationConfigSchema.template(DestinationType.KAFKA));
    }

    @Test
    void template_rabbitmq_containsBothRequiredKeysInOrder() {
        assertEquals("{\"exchange\": \"\", \"routingKey\": \"\"}", DestinationConfigSchema.template(DestinationType.RABBITMQ));
    }

    /**
     * {@code template()} renders keys in the schema's iteration order, so every
     * required-key set must preserve declaration order — {@code Set.of(...)}
     * randomises it per JVM run, which would make the templates non-deterministic
     * the moment a type declares a second required key.
     */
    @Test
    void requiredKeys_everyType_preservesDeclarationOrder() {
        for (DestinationType type : DestinationType.values()) {
            assertTrue(DestinationConfigSchema.requiredKeys(type) instanceof LinkedHashSet,
                    "required keys for " + type + " must be an ordered set, not Set.of(...)");
        }
    }

    /** Every declared type has a schema entry — guards the static completeness check. */
    @Test
    void everyDestinationType_hasRequiredAndOptionalKeys() {
        for (DestinationType type : DestinationType.values()) {
            assertNotNull(DestinationConfigSchema.requiredKeys(type), "requiredKeys missing for " + type);
            assertNotNull(DestinationConfigSchema.optionalKeys(type), "optionalKeys missing for " + type);
        }
    }
}
