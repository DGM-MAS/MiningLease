package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MiningLeasePAFCRequest {

    private String applicationNo;
    private String paDocId;
    private String fcDocId;
    private String publicClearanceDocId;
}
