package dev.kairos.admin.shared.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

/**
 * Field factory with shared preconfiguration, so features create inputs
 * consistently instead of repeating the same setup.
 */
public final class Fields {

    private Fields() {
    }

    public static TextField text(String label) {
        TextField field = new TextField(label);
        field.setClearButtonVisible(true);
        return field;
    }

    public static TextArea textArea(String label) {
        TextArea field = new TextArea(label);
        field.setClearButtonVisible(true);
        return field;
    }

    public static IntegerField integer(String label) {
        IntegerField field = new IntegerField(label);
        field.setClearButtonVisible(true);
        field.setStepButtonsVisible(true);
        return field;
    }

    public static Checkbox checkbox(String label, boolean value) {
        return new Checkbox(label, value);
    }
}
