package dev.kairos.admin.feature.destination.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import dev.kairos.admin.feature.destination.DestinationText;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.feature.destination.dto.UpdateDestinationRequest;
import dev.kairos.admin.shared.form.FieldValidation;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Dialogs;
import dev.kairos.admin.shared.ui.Fields;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.util.DateTimes;
import dev.kairos.admin.shared.util.JsonText;
import dev.kairos.admin.shared.util.Strings;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.Consumer;

/**
 * Modal dialog that shows a single destination and lets an operator switch it
 * into edit mode or delete it. Opens read-only; clicking <em>Edit</em> makes
 * the editable fields writable and swaps the footer to reveal <em>Save</em>.
 * Only {@code config} may change — id and type are immutable after creation.
 * A <em>Delete</em> button is always visible in the footer; clicking it shows
 * a confirmation dialog before invoking the {@code onDelete} callback.
 */
public class DestinationDetails extends Dialog {

    private final JsonMapper jsonMapper;
    private final DestinationDTO destination;
    private final Consumer<UpdateDestinationRequest> onSave;
    private final Consumer<DestinationDTO> onDelete;

    private final TextField destinationId = Fields.text(DestinationText.FIELD_ID);
    private final TextField destinationType = Fields.text(DestinationText.FIELD_TYPE);
    private final TextField createdAt = Fields.text(DestinationText.DETAIL_CREATED_AT);
    private final TextArea config = Fields.textArea(DestinationText.FIELD_CONFIG);

    private final Button deleteButton;
    private final Button editButton;
    private final Button saveButton;
    private final Button closeButton;

    private DestinationDetails(JsonMapper jsonMapper, DestinationDTO destination,
                              Consumer<UpdateDestinationRequest> onSave,
                              Consumer<DestinationDTO> onDelete) {
        this.jsonMapper = jsonMapper;
        this.destination = destination;
        this.onSave = onSave;
        this.onDelete = onDelete;

        this.deleteButton = buildDeleteButton();
        this.editButton = Buttons.secondary(DestinationText.BTN_EDIT, e -> enterEditMode());
        this.saveButton = Buttons.primary(UiText.BTN_SAVE, e -> save());
        this.closeButton = Buttons.tertiary(UiText.BTN_CLOSE, e -> close());

        setHeaderTitle(DestinationText.DETAILS_TITLE);
        setWidth(Tokens.DIALOG_WIDTH_M);

        prefill(destination);
        add(buildForm());
        getFooter().add(deleteButton, closeButton, editButton, saveButton);
        enterViewMode();
    }

    private Button buildDeleteButton() {
        Button button = Buttons.iconDanger(VaadinIcon.TRASH.create(),
                UiText.ACTION_DELETE, e -> confirmDelete());
        return StyleConfig.create().marginInlineEnd(Tokens.AUTO).applyTo(button);
    }

    /**
     * Creates a details dialog for the given destination.
     *
     * @param jsonMapper  used to render and re-parse the config JSON
     * @param destination the destination to display
     * @param onSave      called with the update request when the operator saves edits
     * @param onDelete    called with the destination when the operator confirms deletion
     * @return a new dialog instance, ready to be {@link #open() opened}
     */
    public static DestinationDetails of(JsonMapper jsonMapper, DestinationDTO destination,
                                        Consumer<UpdateDestinationRequest> onSave,
                                        Consumer<DestinationDTO> onDelete) {
        return new DestinationDetails(jsonMapper, destination, onSave, onDelete);
    }

    private FormLayout buildForm() {
        destinationId.setReadOnly(true);
        destinationType.setReadOnly(true);
        createdAt.setReadOnly(true);

        FormLayout layout = new FormLayout(destinationId, destinationType, createdAt, config);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep(Tokens.FORM_BREAKPOINT_ZERO, Tokens.FORM_COLUMNS));
        layout.setColspan(config, Tokens.FORM_COLSPAN_FULL);
        return layout;
    }

    private void prefill(DestinationDTO destination) {
        destinationId.setValue(Strings.nullToEmpty(destination.destinationId()));
        destinationType.setValue(Strings.nullToEmpty(destination.destinationType()));
        createdAt.setValue(Strings.nullToEmpty(DateTimes.forDisplay(destination.createdAt())));
        config.setValue(JsonText.forDisplay(jsonMapper, destination.config()));
    }

    private void enterViewMode() {
        config.setReadOnly(true);
        editButton.setVisible(true);
        saveButton.setVisible(false);
    }

    private void enterEditMode() {
        config.setReadOnly(false);
        editButton.setVisible(false);
        saveButton.setVisible(true);
    }

    private void confirmDelete() {
        Dialogs.confirmDelete(DestinationText.CONFIRM_DELETE_TITLE,
                DestinationText.CONFIRM_DELETE_TEXT, UiText.ACTION_DELETE, this::delete);
    }

    private void delete() {
        onDelete.accept(destination);
        close();
    }

    private void save() {
        if (!FieldValidation.require(config, UiText.VALIDATION_REQUIRED)) {
            return;
        }

        FieldValidation.JsonResult result = FieldValidation.parseJson(
                config, jsonMapper, UiText.VALIDATION_INVALID_JSON);
        if (!result.valid()) {
            return;
        }

        onSave.accept(new UpdateDestinationRequest(result.value()));
        close();
    }
}
