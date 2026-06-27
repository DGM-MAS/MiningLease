package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class MiningLeaseFMFSRequest {

    private String applicationNo;
    private String fmfsDocId;
    private String ecFileId;
    private String ecNumber;
    private Date ecExpiryDate;

}
