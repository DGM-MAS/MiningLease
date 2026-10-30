package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewMineRestorationProgressRequest {

    @NotNull(message = "Progress report ID is required")
    private Long progressReportId;

    // "REVIEWED" or "COMPLETION_REQUESTED"
    @NotNull(message = "Decision is required")
    private String decision;

    private String remarks;
}
