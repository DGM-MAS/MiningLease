package com.mas.gov.bt.mas.primary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MineRestorationResponse {

    private Long id;
    private String applicationNumber;
    private String miningLeaseApplicationNumber;
    private String restorationType;

    // Promoter details
    private Long applicantUserId;
    private String applicantName;
    private String applicantEmail;
    private String applicantContact;

    // Mine details
    private String nameOfMine;
    private String leaseAreaAcres;
    private String dzongkhag;
    private String gewog;
    private LocalDate leaseEndDate;

    // MRP
    private String mrpDocId;
    private LocalDateTime mrpSubmittedAt;

    // Work Order
    private String workOrderDocId;
    private LocalDateTime workOrderIssuedAt;

    // ME review
    private Long assignedMeUserId;
    private String remarksME;
    private LocalDateTime meReviewedAt;

    // ERB
    private String erbDecision;
    private LocalDateTime erbDecidedAt;
    private String erbRemarks;
    private String erbReleaseLetterDocId;
    private LocalDateTime erbReleaseLetterIssuedAt;

    // Status
    private String currentStatus;
    private String currentStatusDisplayName;
    private String rejectionReason;

    // Audit
    private Long createdBy;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
}
