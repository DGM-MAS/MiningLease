package com.mas.gov.bt.mas.primary.config;

import org.springframework.context.annotation.Configuration;

/**
 * CORS is handled exclusively by the API Gateway (globalcors in gateway application.yml).
 * Downstream services must NOT add their own CORS headers — doing so causes
 * duplicate Access-Control-Allow-Origin headers which browsers reject.
 */
@Configuration
public class CorsConfig {
    // No CORS beans — gateway owns CORS for all browser-facing traffic.
}
