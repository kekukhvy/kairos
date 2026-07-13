package dev.kairos.admin.feature.schedule.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import dev.kairos.admin.feature.schedule.CronText;
import dev.kairos.admin.shared.style.Css;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Fields;
import dev.kairos.admin.shared.ui.Notifications;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.util.Strings;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

/**
 * Visual builder for a 6-field Spring cron expression. Opened from the cron row
 * built by {@link ScheduleWhenFields#cronRow} — used by both {@link ScheduleForm}
 * and the wizard's schedule step — it offers a Quick Start preset picker, six
 * per-part {@link ComboBox}es (custom values allowed), a live generated
 * expression with copy, a human-readable description, a next-executions preview,
 * inline validation and advisory warnings. It holds no cron logic — all parsing,
 * validation, description and warning detection is delegated to
 * {@link CronPreview}; on Apply the generated expression is handed to the
 * supplied {@link Consumer}. Prefills from the current value, falling back to the
 * default (Daily at 2 AM) for unparseable input.
 */
public class CronBuilderDialog extends Dialog {

    private static final DateTimeFormatter RUN_TIME =
            DateTimeFormatter.ofPattern(CronText.NEXT_RUNS_TIME_PATTERN);

    private final Consumer<String> onApply;

    private final ComboBox<String> presets = Fields.combo(CronText.QUICK_START_LABEL, CronPresets.names());
    private final ComboBox<String> seconds =
            Fields.comboCustom(CronText.FIELD_SECONDS, CronText.HELPER_SECONDS, CronText.OPTIONS_SECONDS);
    private final ComboBox<String> minutes =
            Fields.comboCustom(CronText.FIELD_MINUTES, CronText.HELPER_MINUTES, CronText.OPTIONS_MINUTES);
    private final ComboBox<String> hours =
            Fields.comboCustom(CronText.FIELD_HOURS, CronText.HELPER_HOURS, CronText.OPTIONS_HOURS);
    private final ComboBox<String> dayOfMonth =
            Fields.comboCustom(CronText.FIELD_DAY_OF_MONTH, CronText.HELPER_DAY_OF_MONTH, CronText.OPTIONS_DAY_OF_MONTH);
    private final ComboBox<String> month =
            Fields.comboCustom(CronText.FIELD_MONTH, CronText.HELPER_MONTH, CronText.OPTIONS_MONTH);
    private final ComboBox<String> dayOfWeek =
            Fields.comboCustom(CronText.FIELD_DAY_OF_WEEK, CronText.HELPER_DAY_OF_WEEK, CronText.OPTIONS_DAY_OF_WEEK);
    private final List<ComboBox<String>> fieldBoxes =
            List.of(seconds, minutes, hours, dayOfMonth, month, dayOfWeek);

    private final Span generated = new Span();
    private final Span description = new Span();
    private final Div warnings = new Div();
    private final Div nextRuns = new Div();
    private final Button apply = Buttons.primary(CronText.BTN_APPLY, e -> apply());

    private CronBuilderDialog(String initial, Consumer<String> onApply) {
        this.onApply = onApply;
        setHeaderTitle(CronText.TITLE);
        setWidth(Tokens.DIALOG_WIDTH_L);

        setupPresetListener();
        setupFieldListeners();
        add(buildContent());
        getFooter().add(buildReset(), buildCancel(), apply);

        populate(prefillOrDefault(initial));
        selectMatchingPreset();
        rebuild();
    }

    /**
     * Sets the Quick Start selection to the preset matching the current fields,
     * or the first preset when none matches — so the picker always shows a value
     * without clobbering the prefilled fields (re-applying a matching preset is
     * idempotent).
     */
    private void selectMatchingPreset() {
        String expression = currentFields().expression();
        String match = CronPresets.names().stream()
                .filter(name -> expression.equals(CronPresets.expressionFor(name)))
                .findFirst()
                .orElse(CronPresets.names().get(0));
        presets.setValue(match);
    }

    /**
     * Opens a builder seeded from {@code initial} (falling back to the default
     * when it is not a parseable 6-field expression); {@code onApply} receives
     * the generated expression when the user applies.
     *
     * @param initial the CRON field's current value, may be blank or invalid
     * @param onApply called with the generated expression on Apply
     * @return a ready-to-open dialog
     */
    public static CronBuilderDialog open(String initial, Consumer<String> onApply) {
        return new CronBuilderDialog(initial, onApply);
    }

    private void setupPresetListener() {
        presets.setHelperText(CronText.QUICK_START_HELPER);
        presets.setWidthFull();
        presets.addValueChangeListener(e -> applyPreset(e.getValue()));
    }

    private void setupFieldListeners() {
        for (ComboBox<String> box : fieldBoxes) {
            box.addValueChangeListener(e -> rebuild());
            box.addCustomValueSetListener(e -> {
                box.setValue(e.getDetail());
                rebuild();
            });
        }
    }

    private VerticalLayout buildContent() {
        VerticalLayout content = new VerticalLayout(
                presets, buildFieldsLayout(), buildSummaryRow(),
                warnings,
                labelled(CronText.NEXT_RUNS_LABEL, nextRuns));
        content.setPadding(false);
        content.setSpacing(true);
        generated.getStyle().set(Css.FONT_FAMILY, Tokens.FONT_MONOSPACE);
        warnings.getStyle().set(Css.COLOR, Tokens.COLOR_WARNING);
        return content;
    }

    /** Generated expression and its human-readable description, side by side. */
    private HorizontalLayout buildSummaryRow() {
        HorizontalLayout generatedRow = buildGeneratedRow();
        Div descriptionBlock = labelled(CronText.DESCRIPTION_LABEL, description);
        HorizontalLayout row = new HorizontalLayout(generatedRow, descriptionBlock);
        row.setWidthFull();
        row.setFlexGrow(1, generatedRow, descriptionBlock);
        return row;
    }

    private FormLayout buildFieldsLayout() {
        FormLayout layout = new FormLayout(seconds, minutes, hours, dayOfMonth, month, dayOfWeek);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep(Tokens.FORM_BREAKPOINT_ZERO, Tokens.FORM_COLUMNS));
        return layout;
    }

    private HorizontalLayout buildGeneratedRow() {
        Button copy = Buttons.icon(VaadinIcon.COPY.create(), CronText.COPY_TOOLTIP, e -> copy());
        HorizontalLayout row = new HorizontalLayout(
                labelled(CronText.GENERATED_LABEL, generated), copy);
        row.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        return row;
    }

    private Div labelled(String label, com.vaadin.flow.component.Component value) {
        Span caption = new Span(label);
        caption.getStyle().set(Css.COLOR, Tokens.TEXT_SECONDARY);
        caption.getStyle().set(Css.FONT_SIZE, Tokens.FONT_S);
        caption.getStyle().set(Css.DISPLAY, Tokens.DISPLAY_BLOCK);
        return new Div(caption, value);
    }

    private Button buildReset() {
        return Buttons.tertiary(CronText.BTN_RESET, e -> resetToDefault());
    }

    private Button buildCancel() {
        return Buttons.tertiary(UiText.BTN_CANCEL, e -> close());
    }

    private void applyPreset(String name) {
        String expression = CronPresets.expressionFor(name);
        if (expression != null) {
            populate(CronFields.parse(expression));
            rebuild();
        }
    }

    private void resetToDefault() {
        presets.clear();
        populate(CronFields.parse(CronText.DEFAULT_EXPRESSION));
        rebuild();
    }

    private CronFields prefillOrDefault(String initial) {
        CronFields parsed = CronFields.parse(initial);
        return parsed != null ? parsed : CronFields.parse(CronText.DEFAULT_EXPRESSION);
    }

    private void populate(CronFields fields) {
        seconds.setValue(fields.seconds());
        minutes.setValue(fields.minutes());
        hours.setValue(fields.hours());
        dayOfMonth.setValue(fields.dayOfMonth());
        month.setValue(fields.month());
        dayOfWeek.setValue(fields.dayOfWeek());
    }

    private CronFields currentFields() {
        return new CronFields(
                seconds.getValue(), minutes.getValue(), hours.getValue(),
                dayOfMonth.getValue(), month.getValue(), dayOfWeek.getValue());
    }

    private void rebuild() {
        CronFields fields = currentFields();
        String expression = fields.expression();
        generated.setText(expression);
        CronPreview.Validation validation = CronPreview.validate(expression);
        applyValidation(validation);
        description.setText(CronPreview.describe(fields));
        renderWarnings(fields);
        renderNextRuns(expression, validation.valid());
    }

    private void applyValidation(CronPreview.Validation validation) {
        apply.setEnabled(validation.valid());
        generated.getStyle().set(Css.COLOR,
                validation.valid() ? Tokens.TEXT_BODY : Tokens.COLOR_ERROR);
        generated.setTitle(validation.valid() ? "" : validation.message());
    }

    private void renderWarnings(CronFields fields) {
        warnings.removeAll();
        for (String warning : CronPreview.warnings(fields)) {
            Div line = new Div(new Span(warning));
            line.getStyle().set(Css.FONT_SIZE, Tokens.FONT_S);
            warnings.add(line);
        }
    }

    private void renderNextRuns(String expression, boolean valid) {
        nextRuns.removeAll();
        if (!valid) {
            nextRuns.add(new Span(CronText.NEXT_RUNS_INVALID));
            return;
        }
        List<LocalDateTime> times = CronPreview.nextExecutions(expression, CronText.NEXT_RUNS_COUNT);
        if (times.isEmpty()) {
            nextRuns.add(new Span(CronText.NEXT_RUNS_NONE));
            return;
        }
        LocalDateTime soon = LocalDateTime.now().plus(1, ChronoUnit.DAYS);
        nextRuns.add(runGrid(times, soon));
    }

    /**
     * Lays the run times out as two columns filled top-to-bottom (first five in
     * the left column, next five in the right), with a wide gap between columns.
     */
    private Div runGrid(List<LocalDateTime> times, LocalDateTime soon) {
        Div grid = new Div();
        grid.getStyle().set(Css.DISPLAY, Tokens.DISPLAY_GRID);
        grid.getStyle().set(Css.GRID_TEMPLATE_COLUMNS, Tokens.GRID_TWO_COLUMNS);
        grid.getStyle().set(Css.GRID_TEMPLATE_ROWS, Tokens.GRID_FIVE_ROWS);
        grid.getStyle().set(Css.GRID_AUTO_FLOW, Tokens.GRID_FLOW_COLUMN);
        grid.getStyle().set(Css.COLUMN_GAP, Tokens.SPACE_XL);
        grid.getStyle().set(Css.ROW_GAP, Tokens.SPACE_XS);
        times.forEach(time -> grid.add(runRow(time, soon)));
        return grid;
    }

    private Div runRow(LocalDateTime time, LocalDateTime soon) {
        Div row = new Div(new Span(time.format(RUN_TIME)));
        row.getStyle().set(Css.FONT_FAMILY, Tokens.FONT_MONOSPACE);
        row.getStyle().set(Css.PADDING, Tokens.SPACE_XS);
        if (time.isBefore(soon)) {
            row.getStyle().set(Css.BACKGROUND_COLOR, Tokens.COLOR_PRIMARY_10);
            row.getStyle().set(Css.BORDER_RADIUS, Tokens.RADIUS_S);
        }
        return row;
    }

    private void copy() {
        getUI().ifPresent(ui -> ui.getPage().executeJs(CronText.COPY_JS, generated.getText()));
        Notifications.success(CronText.COPY_SUCCESS);
    }

    private void apply() {
        String expression = generated.getText();
        if (Strings.isBlank(expression) || !CronPreview.validate(expression).valid()) {
            return;
        }
        onApply.accept(expression);
        close();
    }
}
