package dev.kairos.admin.feature.destination;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import dev.kairos.admin.feature.destination.component.DestinationForm;
import dev.kairos.admin.feature.destination.component.DestinationGrid;
import dev.kairos.admin.feature.destination.dto.CreateDestinationRequest;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.shared.layout.MainLayout;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;
import dev.kairos.admin.shared.ui.Buttons;
import dev.kairos.admin.shared.ui.Fields;
import dev.kairos.admin.shared.ui.FilterBar;
import dev.kairos.admin.shared.ui.Notifications;
import dev.kairos.admin.shared.ui.UiText;
import dev.kairos.admin.shared.util.JsonText;
import dev.kairos.admin.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.Predicate;

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
    private final FilterBar filterBar;
    private final Select<String> typeFilter;

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
        this.typeFilter = buildTypeFilter();
        this.filterBar = buildFilterBar();

        setSizeFull();
        setSpacing(false);
        setPadding(false);

        StyleConfig.create()
                .padding(Tokens.SPACE_L)
                .gap(Tokens.SPACE_M)
                .applyTo(this);

        add(buildToolbar(), filterBar, grid);
        refresh();
    }

    private void refresh() {
        grid.setRows(destinationService.list());
        applyFilter();
    }

    private FilterBar buildFilterBar() {
        return FilterBar.create()
                .onChange(this::applyFilter)
                .withFilter(typeFilter)
                .build();
    }

    private Select<String> buildTypeFilter() {
        Select<String> select = Fields.select(DestinationText.FILTER_TYPE,
                DestinationText.TYPE_KAFKA, DestinationText.TYPE_SQS,
                DestinationText.TYPE_WEBHOOK, DestinationText.TYPE_RABBITMQ);
        select.setEmptySelectionAllowed(true);
        select.setEmptySelectionCaption(UiText.FILTER_ALL);
        select.setPlaceholder(UiText.FILTER_ALL);
        return select;
    }

    private void applyFilter() {
        grid.setFilter(buildPredicate());
    }

    private Predicate<DestinationDTO> buildPredicate() {
        String term = filterBar.searchTerm();
        String type = typeFilter.getValue();
        return destination -> matchesType(destination, type) && matchesTerm(destination, term);
    }

    private boolean matchesType(DestinationDTO destination, String type) {
        return Strings.isBlank(type) || type.equalsIgnoreCase(destination.destinationType());
    }

    private boolean matchesTerm(DestinationDTO destination, String term) {
        if (term.isEmpty()) {
            return true;
        }
        return contains(destination.destinationId(), term)
                || contains(destination.destinationType(), term)
                || contains(JsonText.forDisplay(jsonMapper, destination.config()), term);
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
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
