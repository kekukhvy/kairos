package dev.kairos.admin.feature.destination;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import dev.kairos.admin.feature.destination.component.DestinationForm;
import dev.kairos.admin.feature.destination.component.DestinationGrid;
import dev.kairos.admin.feature.destination.dto.CreateDestinationRequest;
import dev.kairos.admin.shared.layout.MainLayout;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Notifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Admin UI view that lists all destinations registered in Kairos and lets an
 * operator create new ones. Mounted at the {@value DestinationRoutes#DESTINATIONS}
 * route inside {@link MainLayout}.
 */
@Route(value = DestinationRoutes.DESTINATIONS, layout = MainLayout.class)
@PageTitle(DestinationRoutes.PAGE_TITLE)
public class DestinationView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(DestinationView.class);

    private final JsonMapper jsonMapper;
    private final DestinationService destinationService;
    private final DestinationGrid grid = new DestinationGrid();

    /**
     * Constructs the view. Vaadin and Spring call this once per UI session.
     * The constructor builds the toolbar and grid, then performs an initial
     * load so the table is populated immediately on navigation.
     *
     * @param jsonMapper           used when opening the create form to parse config JSON
     * @param destinationService   application-layer service for listing and creating destinations
     */
    public DestinationView(JsonMapper jsonMapper, DestinationService destinationService) {
        this.jsonMapper = jsonMapper;
        this.destinationService = destinationService;

        setSizeFull();
        setSpacing(false);
        setPadding(false);

        StyleConfig.create()
                .padding(Tokens.SPACE_L)
                .gap(Tokens.SPACE_M)
                .applyTo(this);

        add(buildToolbar(), grid);
        refresh();
    }

    private void refresh() {
        grid.setItems(destinationService.list());
    }

    private HorizontalLayout buildToolbar() {
        H2 title = createTitle();

        Button newDestination = Buttons.primary(DestinationText.NEW_DESTINATION, e -> openForm());

        HorizontalLayout toolbar = new HorizontalLayout(title, newDestination);
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);
        toolbar.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
        return toolbar;
    }

    private void openForm() {
        DestinationForm.forCreate(jsonMapper, this::create).open();
    }

    private void create(CreateDestinationRequest request) {
        execute(() -> destinationService.create(request),
                DestinationText.NOTIFY_CREATED, DestinationText.NOTIFY_CREATE_FAILED);
    }

    private void execute(Runnable action, String successMessage, String failureMessage) {
        try {
            action.run();
            Notifications.success(successMessage);
            refresh();
        } catch (RestClientResponseException ex) {
            logger.error("{} — API responded {}: {}", failureMessage, ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            Notifications.error(failureMessage);
        } catch (RuntimeException ex) {
            logger.error("{} — request failed", failureMessage, ex);
            Notifications.error(failureMessage);
        }
    }

    private H2 createTitle() {
        H2 title = new H2(DestinationText.TITLE);
        return StyleConfig.create()
                .fontSize(Tokens.FONT_XL)
                .applyTo(title);
    }
}
