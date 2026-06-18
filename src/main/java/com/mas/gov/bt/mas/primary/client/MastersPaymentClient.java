package com.mas.gov.bt.mas.primary.client;

import com.mas.gov.bt.mas.primary.dto.payment.PaymentInitiationRequest;
import com.mas.gov.bt.mas.primary.dto.payment.PaymentInitiationResponse;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Slf4j
public class MastersPaymentClient {

    private final RestTemplate restTemplate;

    @Value("${app.masters.base-url}")
    private String mastersBaseUrl;

    public MastersPaymentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public PaymentInitiationResponse initiate(PaymentInitiationRequest request) {
        String url = mastersBaseUrl + "/api/payments";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String authHeader = attrs.getRequest().getHeader("Authorization");
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }
        }

        try {
            ResponseEntity<PaymentInitiationResponse> response =
                    restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(request, headers), PaymentInitiationResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null
                    && response.getBody().getData() != null) {
                log.info("Payment initiated for application {}", request.getApplicationId());
                return response.getBody();
            }
            log.warn("Payment API returned non-success for application {}: {}", request.getApplicationId(), response.getStatusCode());
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "Payment initiation failed. Please try again.");
        } catch (HttpStatusCodeException e) {
            log.error("Payment API error for application {}: status={}", request.getApplicationId(), e.getStatusCode());
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "Payment initiation failed. Please try again.");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to reach payment API for application {}: {}", request.getApplicationId(), e.getMessage());
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "Payment service is unavailable. Please try again.");
        }
    }
}
