package com.mas.gov.bt.mas.primary.dto.payment;

import lombok.Data;

import java.util.List;

@Data
public class PaymentInitiationRequest {
    private String applicationId;
    private String applicationType;
    private String taxPayerName;
    private String taxPayerDocumentNo;
    private String taxPayerNo;
    private String platform;
    private String onPaidStatus;
    private String callbackUrl;
    private List<PaymentItemRequest> paymentItems;

    @Data
    public static class PaymentItemRequest {
        private String feeType;
        private String serviceCode;
        private String description;
        private Integer quantity;
    }
}
