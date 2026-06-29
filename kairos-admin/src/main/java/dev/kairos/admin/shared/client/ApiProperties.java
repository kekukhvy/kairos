package dev.kairos.admin.shared.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kairos.api")
public record ApiProperties(String baseUrl,
                            String taskEndpoint) {

}