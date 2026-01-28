package com.mas.gov.bt.mas.primary.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.email")
@Data
public class EmailProperties {

    private String fromAddress = "noreply@mas.gov.bt";
    private String fromName = "MAS Admin";
    private String supportEmail = "support@mas.gov.bt";

    // Email template settings
    private boolean enabled = true;
    private int retryAttempts = 3;
}
