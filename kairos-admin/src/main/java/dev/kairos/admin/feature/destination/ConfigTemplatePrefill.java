package dev.kairos.admin.feature.destination;

import com.vaadin.flow.component.textfield.TextArea;
import dev.kairos.admin.shared.util.Strings;
import dev.kairos.common.destination.DestinationConfigSchema;
import dev.kairos.common.destination.DestinationType;

/**
 * Shared logic behind "picking a destination type prefills the config text
 * area with that type's JSON template" — used by both {@code DestinationForm}
 * (create-destination dialog) and {@code DestinationStep} (setup wizard).
 *
 * <p>Tracks the template it last applied so a still-untouched field can be
 * replaced when the type changes, while a field the operator has edited is
 * left alone. Always reaches into {@link DestinationConfigSchema} — the
 * single source of truth for per-type config shape — rather than
 * re-declaring key names here.
 */
public final class ConfigTemplatePrefill {

    private final TextArea configField;
    private String lastAppliedTemplate;

    public ConfigTemplatePrefill(TextArea configField) {
        this.configField = configField;
    }

    /**
     * Applies the template for {@code selectedType} to the tracked field when
     * it is blank or still holds the previously applied template; does
     * nothing when {@code selectedType} is {@code null} or the field holds
     * operator-edited content.
     */
    public void onTypeSelected(String selectedType) {
        if (selectedType == null || !isUntouched()) {
            return;
        }
        lastAppliedTemplate = DestinationConfigSchema.template(DestinationType.valueOf(selectedType));
        configField.setValue(lastAppliedTemplate);
    }

    private boolean isUntouched() {
        String current = configField.getValue();
        return Strings.isBlank(current) || current.equals(lastAppliedTemplate);
    }
}
