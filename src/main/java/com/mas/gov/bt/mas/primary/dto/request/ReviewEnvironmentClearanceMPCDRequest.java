package com.mas.gov.bt.mas.primary.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEnvironmentClearanceMPCDRequest {

    private Long renewalId;

    private Long mpcdSiteReportFileId;

    private String additionalRemarks;

    private Boolean approveApplication;

    private Long iomFileId;

    private Boolean submitIOM;
}