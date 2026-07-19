package com.mas.gov.bt.mas.primary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssuePermitRequest {

    @NotBlank(message = "Issue permit file ID is required")
    private String issuePermitFileId;
}