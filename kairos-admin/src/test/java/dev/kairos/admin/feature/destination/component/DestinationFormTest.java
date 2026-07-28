package dev.kairos.admin.feature.destination.component;

import dev.kairos.admin.feature.destination.DestinationText;
import dev.kairos.admin.feature.destination.dto.CreateDestinationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@code DestinationForm}'s type-driven config template prefill: picking
 * a type fills the still-blank/untouched config text area with that type's
 * JSON template from the shared schema
 * ({@code dev.kairos.common.destination.DestinationConfigSchema}); once the
 * operator edits the text, switching type no longer clobbers it.
 */
class DestinationFormTest {

    private JsonMapper jsonMapper;
    private List<CreateDestinationRequest> captured;
    private DestinationForm form;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().build();
        captured = new ArrayList<>();
        Consumer<CreateDestinationRequest> onCreate = captured::add;
        form = DestinationForm.forCreate(jsonMapper, onCreate);
    }

    @Test
    void selectingKafka_prefillsKafkaTemplate() {
        form.destinationType().setValue(DestinationText.TYPE_KAFKA);

        assertThat(form.config().getValue()).isEqualTo("{\"topic\": \"\"}");
    }

    @Test
    void selectingSqs_prefillsSqsTemplate() {
        form.destinationType().setValue(DestinationText.TYPE_SQS);

        assertThat(form.config().getValue()).isEqualTo("{\"queueUrl\": \"\"}");
    }

    @Test
    void selectingWebhook_prefillsWebhookTemplate() {
        form.destinationType().setValue(DestinationText.TYPE_WEBHOOK);

        assertThat(form.config().getValue()).isEqualTo("{\"url\": \"\"}");
    }

    @Test
    void selectingRabbitmq_prefillsRabbitmqTemplate() {
        form.destinationType().setValue(DestinationText.TYPE_RABBITMQ);

        assertThat(form.config().getValue()).isEqualTo("{\"exchange\": \"\", \"routingKey\": \"\"}");
    }

    @Test
    void switchingType_configStillHoldsPreviousTemplate_replacesWithNewTemplate() {
        form.destinationType().setValue(DestinationText.TYPE_KAFKA);

        form.destinationType().setValue(DestinationText.TYPE_SQS);

        assertThat(form.config().getValue()).isEqualTo("{\"queueUrl\": \"\"}");
    }

    @Test
    void switchingType_configEditedByUser_doesNotOverwriteUserInput() {
        form.destinationType().setValue(DestinationText.TYPE_KAFKA);
        form.config().setValue("{\"topic\": \"payments\"}");

        form.destinationType().setValue(DestinationText.TYPE_SQS);

        assertThat(form.config().getValue()).isEqualTo("{\"topic\": \"payments\"}");
    }

    @Test
    void switchingType_configBlank_prefillsNewTemplateEvenAfterClearing() {
        form.destinationType().setValue(DestinationText.TYPE_KAFKA);
        form.config().setValue("");

        form.destinationType().setValue(DestinationText.TYPE_SQS);

        assertThat(form.config().getValue()).isEqualTo("{\"queueUrl\": \"\"}");
    }
}
