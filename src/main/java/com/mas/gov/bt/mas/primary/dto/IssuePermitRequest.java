package com.mas.gov.bt.mas.primary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssuePermitRequest {

    @NotBlank(message = "Issue permit file ID is required")
    private String issuePermitFileId;

    @NotNull(message = "Permit validity (valid to) is required")
    private LocalDate permitValidTo;
}