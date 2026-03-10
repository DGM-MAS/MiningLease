package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MineRestorationMRPResubmitRequest {

    @NotNull(message = "Restoration application ID is required")
    private Long restorationApplicationId;

    @NotBlank(message = "Revised MRP document is required")
    private String mrpDocId;
}
