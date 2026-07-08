package dev.kairos.admin.shared.ui;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.Collection;

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

    /** Drop-down field pre-populated with a fixed, known set of {@code items}. */
    @SafeVarargs
    public static <T> Select<T> select(String label, T... items) {
        Select<T> field = new Select<>();
        field.setLabel(label);
        field.setItems(items);
        return field;
    }

    /**
     * Searchable drop-down (type-ahead) backed by a dynamic {@code items}
     * collection. Suited to lists that grow over time, e.g. destination ids.
     */
    public static <T> ComboBox<T> combo(String label, Collection<T> items) {
        ComboBox<T> field = new ComboBox<>(label);
        field.setItems(items);
        field.setClearButtonVisible(true);
        return field;
    }
}
