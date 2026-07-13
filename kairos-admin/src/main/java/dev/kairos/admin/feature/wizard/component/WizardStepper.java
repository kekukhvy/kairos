package dev.kairos.admin.feature.wizard.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import dev.kairos.admin.feature.wizard.WizardStep;
import dev.kairos.admin.feature.wizard.WizardText;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;

/**
 * Top progress stepper for {@code SetupWizard}: one labelled tab per
 * {@link WizardStep}, the current step highlighted with a coloured progress
 * line, upcoming steps greyed. Strictly presentational and driven entirely by
 * the current step passed to the constructor — it exposes no click handling,
 * matching the "tabs are not clickable" acceptance criterion.
 */
public class WizardStepper extends HorizontalLayout {

    public WizardStepper(WizardStep current) {
        setPadding(false);
        setWidthFull();
        StyleConfig.create().gap(Tokens.SPACE_M).applyTo(this);

        for (WizardStep step : WizardStep.values()) {
            VerticalLayout tab = buildTab(step, current);
            add(tab);
            setFlexGrow(1, tab);
        }
    }

    private VerticalLayout buildTab(WizardStep step, WizardStep current) {
        boolean isCurrent = step == current;
        boolean isPast = step.ordinal() < current.ordinal();

        Span label = buildLabel(step, isCurrent, isPast);

        Div line = StyleConfig.create()
                .fullWidth()
                .height(Tokens.STEPPER_LINE_HEIGHT)
                .background(isCurrent || isPast ? Tokens.COLOR_PRIMARY : Tokens.COLOR_CONTRAST_10)
                .borderRadius(Tokens.RADIUS_S)
                .applyTo(new Div());

        VerticalLayout tab = new VerticalLayout(label, line);
        tab.setPadding(false);
        tab.setSpacing(false);
        StyleConfig.create().gap(Tokens.SPACE_XS).cursor(Tokens.CURSOR_DEFAULT).applyTo(tab);
        return tab;
    }

    private Span buildLabel(WizardStep step, boolean isCurrent, boolean isPast) {
        StyleConfig style = StyleConfig.create()
                .fontSize(Tokens.FONT_S)
                .color(isCurrent || isPast ? Tokens.TEXT_BODY : Tokens.TEXT_SECONDARY);
        if (isCurrent) {
            style.fontWeight(Tokens.FONT_WEIGHT_SEMIBOLD);
        }
        return style.applyTo(new Span(labelFor(step)));
    }

    private String labelFor(WizardStep step) {
        return switch (step) {
            case TASK -> WizardText.STEP_TASK;
            case DESTINATION -> WizardText.STEP_DESTINATION;
            case SCHEDULE -> WizardText.STEP_SCHEDULE;
        };
    }
}
