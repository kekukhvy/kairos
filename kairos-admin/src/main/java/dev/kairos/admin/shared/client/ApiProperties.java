package dev.kairos.admin.shared.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("kairos.api")
public record ApiProperties(String baseUrl,
                            String taskEndpoint,
                            String destinationEndpoint,
                            String scheduleEndpoint,
                            Duration connectTimeout,
                            Duration readTimeout) {

}