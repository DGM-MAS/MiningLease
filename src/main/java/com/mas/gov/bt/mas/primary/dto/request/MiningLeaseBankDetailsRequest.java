package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class MiningLeaseBankDetailsRequest {

    private String applicationNo;
    private Long bankGuarantorDocId;
    private BigDecimal upfrontPaymentAmount;
}
