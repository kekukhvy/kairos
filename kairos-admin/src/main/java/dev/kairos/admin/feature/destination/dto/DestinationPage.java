package dev.kairos.admin.feature.destination.dto;

import dev.kairos.admin.feature.task.dto.TaskDto;

import java.util.List;

/**
 * Paged response envelope for the destination list endpoint.
 *
 * @param items  the destinations in this page; never {@code null}
 * @param limit  maximum number of items the API was asked to return
 * @param offset zero-based index of the first item in this page
 */
public record DestinationPage(
        List<DestinationDTO> items,
        int limit,
        int offset
) {
}