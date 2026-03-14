package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MiningLeaseMLARequest {
    private String applicationNo;
    private String mlaDocId;
    private Long workOrderDocId;
}
