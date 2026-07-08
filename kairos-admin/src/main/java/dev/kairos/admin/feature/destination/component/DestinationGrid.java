package dev.kairos.admin.feature.destination.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.ListDataProvider;
import dev.kairos.admin.feature.destination.DestinationText;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.shared.util.DateTimes;
import dev.kairos.admin.shared.util.JsonText;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Read-only grid that displays destination records in the admin UI.
 * Columns are defined explicitly (auto-detection disabled) and sized to
 * their content via {@code setAutoWidth(true)}.
 */
public class DestinationGrid extends Grid<DestinationDTO> {

    private final ListDataProvider<DestinationDTO> dataProvider = new ListDataProvider<>(new ArrayList<>());

    private Consumer<DestinationDTO> onView = destination -> {
    };

    public DestinationGrid(JsonMapper jsonMapper) {
        super(DestinationDTO.class, false);

        addColumn(DestinationDTO::destinationId)
                .setHeader(DestinationText.COL_DESTINATION_ID)
                .setAutoWidth(true)
                .setSortable(true);

        addColumn(DestinationDTO::destinationType)
                .setHeader(DestinationText.COL_DESTINATION_TYPE)
                .setAutoWidth(true)
                .setSortable(true);

        addColumn(destination -> JsonText.forDisplay(jsonMapper, destination.config()))
                .setHeader(DestinationText.COL_DESTINATION_CONFIG)
                .setAutoWidth(true);

        addColumn(destination -> DateTimes.forDisplay(destination.createdAt()))
                .setHeader(DestinationText.COL_CREATED_AT)
                .setAutoWidth(true)
                .setComparator(DestinationDTO::createdAt);

        addItemDoubleClickListener(event -> onView.accept(event.getItem()));

        setItems(dataProvider);
        setSizeFull();
    }

    /** Sets the callback invoked when a row is opened for view/edit. */
    public void setOnView(Consumer<DestinationDTO> onView) {
        this.onView = onView;
    }

    /** Replaces the rows shown, preserving any active filter. */
    public void setRows(Collection<DestinationDTO> destinations) {
        dataProvider.getItems().clear();
        dataProvider.getItems().addAll(destinations);
        dataProvider.refreshAll();
    }

    /** Applies a client-side filter predicate; {@code null} shows all rows. */
    public void setFilter(Predicate<DestinationDTO> predicate) {
        dataProvider.setFilter(predicate == null ? null : predicate::test);
    }
}
