package com.mas.gov.bt.mas.primary.integration;

import com.mas.gov.bt.mas.primary.dto.CitizenRegisterRequest;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class CitizenRegistrationClient {

    private final RestTemplate restTemplate;

    @Value("${app.masters.citizen-register-url}")
    private String citizenRegisterUrl;

    /**
     * Calls master's citizen self-registration endpoint.
     * Returns true if newly created, false if the citizen already existed (409 Conflict).
     * Any other failure throws, so the caller's transaction rolls back.
     */
    public boolean registerCitizen(CitizenRegisterRequest request) {
        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(citizenRegisterUrl, request, String.class);
            log.info("Citizen registration succeeded for {} ({})", request.getEmail(), response.getStatusCode());
            return true;
        } catch (HttpClientErrorException.Conflict ex) {
            log.info("Citizen {} already registered, treating as existing user.", request.getEmail());
            return false;
        } catch (HttpClientErrorException ex) {
            log.error("Master rejected citizen registration for {}: {} - {}",
                    request.getEmail(), ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new BusinessException(ErrorCodes.DATA_INTEGRITY_VIOLATION,
                    "Citizen registration failed: " + ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            log.error("Timeout/connection error calling master for {}", request.getEmail(), ex);
            throw new BusinessException(ErrorCodes.SERVICE_UNAVAILABLE,
                    "Unable to reach registration service. Please try again.");
        }
    }
}