package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RenewalNotesheetMlaRequest {

    private String applicationNo;
    private String noteSheetDocId;
    private String mlaDocId;
    private String remarks;
    private boolean erbRegularizationRequired;
    private BigDecimal payableAmount;
}
