package com.mas.gov.bt.mas.primary.dto.payment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCallbackDTO {
    private String applicationNo;
    private String transactionId;
    private String status;
}
