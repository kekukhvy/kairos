package dev.kairos.admin.feature.destination;

import dev.kairos.admin.feature.destination.dto.DestinationDTO;
import dev.kairos.admin.feature.destination.dto.DestinationPage;
import dev.kairos.admin.shared.client.ApiProperties;
import dev.kairos.admin.shared.client.KairosApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fetches destination data from the kairos-api on behalf of the admin UI.
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
}
