package dev.kairos.admin.feature.destination;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import dev.kairos.admin.feature.destination.component.DestinationGrid;
import dev.kairos.admin.feature.task.TaskText;
import dev.kairos.admin.shared.layout.MainLayout;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;

/**
 * Admin UI view that lists all destinations registered in Kairos.
 * Mounted at the {@value DestinationRoutes#DESTINATIONS} route inside
 * {@link MainLayout} and populated by a single {@link DestinationService#list()}
 * call on construction.
 */
@Route(value = DestinationRoutes.DESTINATIONS, layout = MainLayout.class)
@PageTitle(DestinationRoutes.PAGE_TITLE)
public class DestinationView extends VerticalLayout {

    private final DestinationGrid grid = new DestinationGrid();
    private final DestinationService destinationService;

    public DestinationView(DestinationService destinationService) {
        this.destinationService = destinationService;

        setSizeFull();
        setSpacing(false);
        setPadding(false);

        StyleConfig.create()
                .padding(Tokens.SPACE_L)
                .gap(Tokens.SPACE_M)
                .applyTo(this);

        add(buildToolbar(), grid);
        refreshGrid();
    }

    private void refreshGrid() {
        grid.setItems(destinationService.list());
    }

    private HorizontalLayout buildToolbar() {
        H2 title = createTitle();

        Button newTask = Buttons.primary(DestinationText.NEW_DESTINATION, e -> openForm());

        HorizontalLayout toolbar = new HorizontalLayout(title, newTask);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        return toolbar;
    }

    private void openForm() {

    }

    private H2 createTitle() {
        H2 title = new H2(DestinationText.TITLE);
        return StyleConfig.create()
                .fontSize(Tokens.FONT_XL)
                .applyTo(title);
    }
}
