package dev.kairos.admin.shared.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Apple-style frosted filter bar: a translucent, softly rounded strip that
 * hosts a search field plus optional feature-specific filter inputs and a
 * clear-all action. Purely a layout/interaction shell — it owns no filtering
 * logic; callers register a listener that re-applies the grid data provider
 * whenever any field changes.
 */
public final class FilterBar extends HorizontalLayout {

    private static final String STYLE_CLASS = "kairos-filter-bar";

    private final TextField search = buildSearch();
    private final List<HasValue<?, ?>> resettable = new ArrayList<>();
    private final Button clear;
    private Runnable onChange = () -> {
    };

    private FilterBar() {
        addClassName(STYLE_CLASS);
        setWidthFull();
        setSpacing(true);
        setAlignItems(Alignment.END);
        setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        clear = buildClear();
        resettable.add(search);

        add(search);
        setFlexGrow(1, search);
    }

    /** Creates a filter bar with just a search field. */
    public static FilterBar create() {
        return new FilterBar();
    }

    /**
     * Registers an extra filter field. Its value is reset by the clear action
     * and its changes fire {@link #onChange}. The field is inserted before the
     * clear button so the layout stays: search … filters … clear.
     */
    public FilterBar withFilter(Component field) {
        if (field instanceof HasValue<?, ?> valued) {
            resettable.add(valued);
            valued.addValueChangeListener(e -> onChange.run());
        }
        add(field);
        return this;
    }

    /** Sets the listener invoked whenever any filter value changes. */
    public FilterBar onChange(Runnable listener) {
        this.onChange = listener;
        return this;
    }

    /** Current search text, trimmed and lower-cased for case-insensitive matching. */
    public String searchTerm() {
        String value = search.getValue();
        return value == null ? "" : value.trim().toLowerCase();
    }

    /** Finishes assembly by appending the clear-all button. Call last. */
    public FilterBar build() {
        add(clear);
        return this;
    }

    private TextField buildSearch() {
        TextField field = new TextField();
        field.setPlaceholder(UiText.FILTER_SEARCH_PLACEHOLDER);
        field.setClearButtonVisible(true);
        field.setPrefixComponent(VaadinIcon.SEARCH.create());
        field.setValueChangeMode(ValueChangeMode.LAZY);
        field.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        field.addValueChangeListener(e -> onChange.run());
        return field;
    }

    private Button buildClear() {
        Button button = Buttons.tertiary(UiText.FILTER_CLEAR, e -> reset());
        button.setIcon(VaadinIcon.CLOSE_SMALL.create());
        return button;
    }

    private void reset() {
        resettable.forEach(HasValue::clear);
        onChange.run();
    }
}
