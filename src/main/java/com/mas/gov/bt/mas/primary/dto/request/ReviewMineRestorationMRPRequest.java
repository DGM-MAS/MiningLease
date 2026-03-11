package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewMineRestorationMRPRequest {

    @NotNull(message = "Restoration application ID is required")
    private Long restorationApplicationId;

    // "APPROVED" or "REVISION_REQUESTED" or "REJECTED"
    @NotNull(message = "Decision is required")
    private String decision;

    private String remarks;
}
