package dev.kairos.admin.shared.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KairosApiClient {

    private final RestClient restClient;

    public KairosApiClient(ApiProperties apiProperties) {
        this.restClient = RestClient.builder()
                .baseUrl(apiProperties.baseUrl())
                .build();
    }

    public RestClient rest() {
        return this.restClient;
    }
}