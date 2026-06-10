package com.mas.gov.bt.mas.primary.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AppConfig {

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Explicit timeouts — a hung BIRMS/DataHub call must never pin a thread forever
        RestTemplate restTemplate = builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .build();
        // Masters guards its service-to-service notification endpoints with this
        // key (InternalApiKeyFilter); scoped by path so it never leaks to BIRMS/DataHub
        restTemplate.getInterceptors().add((request, body, execution) -> {
            if (request.getURI().getPath().startsWith("/api/notifications")) {
                request.getHeaders().set("X-Internal-Key", internalApiKey);
            }
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}
