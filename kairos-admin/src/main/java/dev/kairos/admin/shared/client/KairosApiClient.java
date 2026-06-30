package dev.kairos.admin.shared.client;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Component
public class KairosApiClient {

    private final RestClient restClient;

    public KairosApiClient(ApiProperties apiProperties) {
        this.restClient = RestClient.builder()
                .baseUrl(apiProperties.baseUrl())
                .requestFactory(requestFactory(apiProperties))
                .build();
    }

    private static JdkClientHttpRequestFactory requestFactory(ApiProperties apiProperties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(apiProperties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(apiProperties.readTimeout());
        return factory;
    }

    public RestClient rest() {
        return this.restClient;
    }
}