package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewMiningLeaseApplicationME {
    private Long id;
    private String status;
    private String remarks;
    private String fmfsStatus;
    private String approvedArea;
    private String approvedErb;
    private String approvedLeasePeriod;
    private String approvedMineral;
}
