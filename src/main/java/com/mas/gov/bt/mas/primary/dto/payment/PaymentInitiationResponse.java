package com.mas.gov.bt.mas.primary.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentInitiationResponse {
    private String message;
    private PaymentData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentData {
        private String redirectUrl;
        private String refNo;
        private String paymentAdviceNo;
        private String paymentStatus;
    }

    public String getRedirectUrl() {
        return data != null ? data.getRedirectUrl() : null;
    }
}
