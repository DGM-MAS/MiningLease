package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MineRestorationVerificationReportRequest {

    @NotNull(message = "Progress report ID is required")
    private Long progressReportId;

    // Verification report document upload ID
    @NotNull(message = "Verification report document is required")
    private String verificationReportDocId;

    private String remarks;
}
