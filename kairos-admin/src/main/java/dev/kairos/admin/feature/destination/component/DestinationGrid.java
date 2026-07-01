package dev.kairos.admin.feature.destination.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.ListDataProvider;
import dev.kairos.admin.feature.destination.DestinationText;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.shared.util.DateTimes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Read-only grid that displays destination records in the admin UI.
 * Columns are defined explicitly (auto-detection disabled) and sized to
 * their content via {@code setAutoWidth(true)}.
 */
public class DestinationGrid extends Grid<DestinationDTO> {

    private final ListDataProvider<DestinationDTO> dataProvider = new ListDataProvider<>(new ArrayList<>());

    public DestinationGrid() {
        super(DestinationDTO.class, false);

        addColumn(DestinationDTO::destinationId)
                .setHeader(DestinationText.COL_DESTINATION_ID)
                .setAutoWidth(true)
                .setSortable(true);

        addColumn(DestinationDTO::destinationType)
                .setHeader(DestinationText.COL_DESTINATION_TYPE)
                .setAutoWidth(true)
                .setSortable(true);

        addColumn(DestinationDTO::config)
                .setHeader(DestinationText.COL_DESTINATION_CONFIG)
                .setAutoWidth(true);

        addColumn(destination -> DateTimes.forDisplay(destination.createdAt()))
                .setHeader(DestinationText.COL_CREATED_AT)
                .setAutoWidth(true)
                .setComparator(DestinationDTO::createdAt);

        setItems(dataProvider);
        setSizeFull();
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
