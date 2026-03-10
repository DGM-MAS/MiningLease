package com.mas.gov.bt.mas.primary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MineRestorationProgressReportResponse {

    private Long id;
    private String restorationApplicationNumber;
    private Integer progressReportNumber;

    // Background
    private String nameOfMine;
    private String leaseAreaAcres;
    private String nameOfLessee;
    private String locationImageDocId;

    // Progress details
    private LocalDate startDateOfMineRestoration;
    private LocalDate dateOfProgressReport;
    private String activityDescription;
    private String financialProgress;
    private String physicalProgress;
    private String pictorialEvidenceDocId;

    // RC/MI verification
    private Long assignedRcUserId;
    private String verificationReportDocId;
    private LocalDateTime verificationSubmittedAt;
    private String verificationRemarks;

    // ME review
    private String meRemarks;
    private LocalDateTime meReviewedAt;

    private String status;
    private LocalDateTime createdOn;
}
