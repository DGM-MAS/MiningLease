package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MineRestorationProgressReportRequest {

    @NotBlank(message = "Restoration application number is required")
    private String restorationApplicationNumber;

    // Background (auto-pulled on server but can be overridden)
    private String locationImageDocId;

    // Progress details
    private LocalDate startDateOfMineRestoration;
    private LocalDate dateOfProgressReport;

    private String activityDescription;
    private String financialProgress;
    private String physicalProgress;
    private String pictorialEvidenceDocId;

    // "DRAFT" or "SUBMITTED"
    private String status;
}
