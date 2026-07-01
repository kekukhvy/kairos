package dev.kairos.admin.feature.destination;

import dev.kairos.admin.feature.destination.dto.CreateDestinationRequest;
import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.feature.destination.dto.DestinationPage;
import dev.kairos.admin.feature.destination.dto.UpdateDestinationRequest;
import dev.kairos.admin.shared.client.ApiProperties;
import dev.kairos.admin.shared.client.KairosApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Proxies destination CRUD operations to the kairos-api on behalf of the admin UI.
 * Covers listing, creating, updating, and deleting destinations.
 */
@Service
public class DestinationService {

    private static final Logger logger = LoggerFactory.getLogger(DestinationService.class);

    private final KairosApiClient client;
    private final ApiProperties apiProperties;

    public DestinationService(KairosApiClient client, ApiProperties apiProperties) {
        this.client = client;
        this.apiProperties = apiProperties;
    }

    /**
     * Returns all destinations from the kairos-api.
     * When the response body is absent (e.g. the server returns no content),
     * an empty list is returned rather than propagating a null.
     *
     * @return the list of destinations; never {@code null}
     */
    public List<DestinationDTO> list() {
        logger.debug("Fetching destinations from {}", apiProperties.destinationEndpoint());

        DestinationPage page = client.rest()
                .get()
                .uri(apiProperties.destinationEndpoint())
                .retrieve()
                .body(DestinationPage.class);

        if (page == null) {
            logger.warn("Destination list response body is null — returning empty list");
            return List.of();
        }

        logger.debug("Fetched {} destination(s)", page.items().size());
        return page.items();
    }

    /**
     * Creates a new destination via the kairos-api.
     *
     * @param request the destination to create
     * @return the created destination as returned by the API
     */
    public DestinationDTO create(CreateDestinationRequest request) {
        logger.debug("Creating destination {} of type {}", request.destinationId(), request.destinationType());

        return client.rest()
                .post()
                .uri(apiProperties.destinationEndpoint())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(DestinationDTO.class);
    }

    /**
     * Updates an existing destination's config via the kairos-api.
     * Only the {@code config} may change; id and type are immutable.
     *
     * @param destinationId the id of the destination to update
     * @param request       the new config to apply
     * @return the updated destination as returned by the API
     */
    public DestinationDTO update(String destinationId, UpdateDestinationRequest request) {
        logger.debug("Updating destination {}", destinationId);

        return client.rest()
                .put()
                .uri(apiProperties.destinationEndpoint() + "/{id}", destinationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(DestinationDTO.class);
    }

    /**
     * Deletes a destination via the kairos-api. The API rejects deletion with
     * 409 while any task still references the destination.
     *
     * @param destinationId the id of the destination to delete
     */
    public void delete(String destinationId) {
        logger.debug("Deleting destination {}", destinationId);

        client.rest()
                .delete()
                .uri(apiProperties.destinationEndpoint() + "/{id}", destinationId)
                .retrieve()
                .toBodilessEntity();
    }
}
