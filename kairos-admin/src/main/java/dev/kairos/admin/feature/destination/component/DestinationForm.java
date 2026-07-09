package dev.kairos.admin.feature.destination.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import dev.kairos.admin.feature.destination.DestinationText;
import dev.kairos.admin.feature.destination.dto.CreateDestinationRequest;
import dev.kairos.admin.shared.form.FieldValidation;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Fields;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.util.Strings;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.Consumer;

/**
 * Modal dialog for creating a new destination. Validates the required fields
 * and parses the free-form config as JSON before handing a
 * {@link CreateDestinationRequest} to the supplied callback.
 */
public class DestinationForm extends Dialog {

    private final JsonMapper jsonMapper;
    private final Consumer<CreateDestinationRequest> onCreate;

    private final TextField destinationId = Fields.text(DestinationText.FIELD_ID);
    private final Select<String> destinationType = Fields.select(
            DestinationText.FIELD_TYPE,
            DestinationText.TYPE_KAFKA,
            DestinationText.TYPE_SQS,
            DestinationText.TYPE_WEBHOOK,
            DestinationText.TYPE_RABBITMQ);
    private final TextArea config = Fields.textArea(DestinationText.FIELD_CONFIG);

    private DestinationForm(JsonMapper jsonMapper, Consumer<CreateDestinationRequest> onCreate) {
        this.jsonMapper = jsonMapper;
        this.onCreate = onCreate;

        setHeaderTitle(DestinationText.NEW_DESTINATION_TITLE);
        setWidth(Tokens.DIALOG_WIDTH_M);

        add(buildForm());
        getFooter().add(buildCancel(), buildSave());
    }

    /**
     * Creates a {@code DestinationForm} wired for the create flow. The dialog
     * validates required fields and parses the config field as JSON; once both
     * checks pass, it calls {@code onCreate} with the assembled request and
     * closes itself.
     *
     * @param jsonMapper used to parse and validate the free-form config JSON
     * @param onCreate   called once per successful submission with the populated request
     * @return a new dialog instance, ready to be {@link #open() opened}
     */
    public static DestinationForm forCreate(JsonMapper jsonMapper, Consumer<CreateDestinationRequest> onCreate) {
        return new DestinationForm(jsonMapper, onCreate);
    }

    private FormLayout buildForm() {
        FormLayout layout = new FormLayout(destinationId, destinationType, config);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep(Tokens.FORM_BREAKPOINT_ZERO, Tokens.FORM_COLUMNS));
        layout.setColspan(config, Tokens.FORM_COLSPAN_FULL);
        return layout;
    }

    private Button buildSave() {
        return Buttons.primary(UiText.BTN_SAVE, e -> save());
    }

    private Button buildCancel() {
        return Buttons.tertiary(UiText.BTN_CANCEL, e -> close());
    }

    private void save() {
        if (!validate()) {
            return;
        }

        FieldValidation.JsonResult result = FieldValidation.parseJson(
                config, jsonMapper, UiText.VALIDATION_INVALID_JSON);
        if (!result.valid()) {
            return;
        }

        onCreate.accept(new CreateDestinationRequest(
                Strings.trimToNull(destinationId.getValue()),
                destinationType.getValue(),
                result.value()
        ));
        close();
    }

    private boolean validate() {
        boolean ok = FieldValidation.require(destinationId, UiText.VALIDATION_REQUIRED);
        ok &= FieldValidation.requirePresent(destinationType, destinationType, UiText.VALIDATION_REQUIRED);
        ok &= FieldValidation.require(config, UiText.VALIDATION_REQUIRED);
        return ok;
    }
}
