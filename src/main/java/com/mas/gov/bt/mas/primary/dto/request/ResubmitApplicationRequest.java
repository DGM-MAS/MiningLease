package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResubmitApplicationRequest {

    private FileUploadRequest fileUploadRequest;

    private MiningLeaseApplicationRequest miningLeaseApplicationRequest;

    // getters and setters
}