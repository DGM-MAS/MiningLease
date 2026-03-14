package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IssueERBReleaseLetterRequest {

    @NotNull(message = "Restoration application ID is required")
    private Long restorationApplicationId;

    @NotBlank(message = "ERB release letter document ID is required")
    private String erbReleaseLetterDocId;
}
