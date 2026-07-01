package dev.kairos.admin.feature.destination.component;

import com.vaadin.flow.component.grid.Grid;
import dev.kairos.admin.feature.destination.DestinationText;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.shared.util.DateTimes;

/**
 * Read-only grid that displays destination records in the admin UI.
 * Columns are defined explicitly (auto-detection disabled) and sized to
 * their content via {@code setAutoWidth(true)}.
 */
public class DestinationGrid extends Grid<DestinationDTO> {

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

        setSizeFull();
    }
}
