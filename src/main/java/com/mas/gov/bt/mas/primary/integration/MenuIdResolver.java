package com.mas.gov.bt.mas.primary.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single source of truth for a notification's sidebar serviceId. Every workflow
 * service used to hardcode its own guess at permissions.id (MENU_ID_* constants) —
 * that's the root cause behind "sidebar dot most inconsistent": ~30 independent
 * guesses across 6 repos, with nothing keeping them in sync with the live
 * permissions table. This resolves each menu_path against masters
 * (MenuResolutionController) once at startup and caches it for the JVM's
 * lifetime; if masters is unreachable or the path no longer exists, it falls
 * back to the caller-supplied literal and logs loudly instead of failing silently.
 */
@Component
@Slf4j
public class MenuIdResolver {

    private final RestTemplate restTemplate;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Value("${app.menu-resolution.api-url}")
    private String menuResolutionApiUrl;

    public MenuIdResolver(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String resolve(String menuPath, String fallback) {
        String cached = cache.get(menuPath);
        if (cached != null) {
            return cached;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Key", internalApiKey);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(Map.of("menuPaths", List.of(menuPath)), headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    menuResolutionApiUrl + "/resolve", HttpMethod.POST, entity, Map.class);

            Object idValue = extractResolvedId(response.getBody(), menuPath);
            if (idValue != null) {
                String resolvedId = String.valueOf(idValue);
                cache.put(menuPath, resolvedId);
                return resolvedId;
            }

            log.error("Menu path {} not found in live permissions table — falling back to " +
                    "hardcoded id {}. If this path was recently renamed/reordered, the sidebar dot " +
                    "for this notification will point at the wrong menu item.", menuPath, fallback);
        } catch (Exception e) {
            log.error("Failed to resolve menu path {} against masters — falling back to " +
                    "hardcoded id {}. Reason: {}", menuPath, fallback, e.getMessage());
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private Object extractResolvedId(Map<?, ?> body, String menuPath) {
        if (body == null) return null;
        Object data = body.get("data");
        if (!(data instanceof Map)) return null;
        Object resolved = ((Map<?, ?>) data).get("resolved");
        if (!(resolved instanceof Map)) return null;
        return ((Map<String, Object>) resolved).get(menuPath);
    }
}
