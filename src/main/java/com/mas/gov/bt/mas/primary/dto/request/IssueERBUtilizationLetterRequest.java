package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IssueERBUtilizationLetterRequest {

    @NotNull(message = "Restoration application ID is required")
    private Long restorationApplicationId;

    @NotBlank(message = "ERB utilization letter document ID is required")
    private String erbUtilizationLetterDocId;
}
