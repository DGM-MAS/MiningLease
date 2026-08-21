package com.mas.gov.bt.mas.primary.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for masters' service-to-service application-status SMS endpoint
 * (/api/sms/internal/application-status). Masters resolves the citizen's
 * phone number itself from userId, so callers here only pass identifiers.
 * Mirrors this repo's WorkflowAssignmentClient convention: internal-API-key
 * header set explicitly per-request, and {@code app.masters.base-url} (the
 * production-configured masters URL already used by WorkflowAssignmentClient
 * in this service).
 *
 * Fire-and-forget by design (mirrors NotificationClient) — an SMS gateway
 * outage must never block a workflow transition.
 */
@Component
@Slf4j
public class SmsClient {

    private final RestTemplate restTemplate;

    @Value("${app.masters.base-url}")
    private String mastersBaseUrl;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public SmsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Async
    public void sendApplicationStatusSms(Long userId, String applicationId, String stage) {
        if (userId == null) {
            return;
        }
        String url = mastersBaseUrl + "/api/sms/internal/application-status";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Key", internalApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("applicationId", applicationId);
        body.put("stage", stage);

        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        } catch (HttpStatusCodeException e) {
            log.warn("Application status SMS failed for userId={}, applicationId={}, stage={}: status={}, body={}",
                    userId, applicationId, stage, e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("Application status SMS failed for userId={}, applicationId={}, stage={}: {}",
                    userId, applicationId, stage, e.getMessage());
        }
    }
}
