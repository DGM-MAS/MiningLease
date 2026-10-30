package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MineRestorationMRPRequest {

    // Provided when updating an existing draft
    private Long restorationApplicationId;

    // The existing mining lease application number to link to
    @NotBlank(message = "Mining lease application number is required")
    private String miningLeaseApplicationNumber;

    // "MINE_CLOSURE" or "TERMINATION_SURRENDER"
    @NotBlank(message = "Restoration type is required")
    private String restorationType;

    // MRP document upload ID
    @NotBlank(message = "MRP document is required")
    private String mrpDocId;

    // "DRAFT" or "SUBMITTED"
    private String status;
}
