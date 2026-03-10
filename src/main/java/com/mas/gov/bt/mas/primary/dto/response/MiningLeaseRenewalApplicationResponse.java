package com.mas.gov.bt.mas.primary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiningLeaseRenewalApplicationResponse {

    private Long id;
    private String applicationNumber;

    // Applicant Details
    private String applicantName;
    private String applicantContact;
    private String applicantEmail;
    private String applicantCid;
    private String applicantType;
    private String postalAddress;
    private String telephoneNo;

    // Lease Period
    private LocalDate leaseEndDate;
    private Integer leasePeriodYears;
    private Integer proposedLeaseRenewalPeriod;

    // Location
    private String placeOfMiningActivity;
    private String dungkhag;
    private String dzongkhag;
    private String dzongkhagId;
    private String gewog;
    private Integer gewogId;
    private String nearestVillage;
    private Integer nearestVillageId;

    // Documents & Declaration
    private String depositAssessmentReportId;
    private boolean declarationStatus;
    private String fmfsDocId;
    private String fmfsId;
    private String fmfsStatus;

    // Mining Engineer Review
    private String remarksME;
    private LocalDateTime meReviewedAt;
    private String llcMineEngineerDocId;
    private String noteSheetDocId;
    private Long workOrderDocId;
    private String workOrderRemarks;
    private String mlaDocId;
    private String mlaStatus;
    private LocalDateTime mlaSignedAt;

    // Director Review
    private String remarksDirector;
    private LocalDateTime directorReviewedAt;

    // Geologist Review
    private String remarksGeologist;
    private LocalDateTime geologistReviewedAt;
    private String geologicalReportStatus;

    // Mining Chief Review
    private String remarksChief;
    private LocalDateTime chiefReviewedAt;

    // Status & Audit
    private String currentStatus;
    private Long createdBy;
    private LocalDateTime createdOn;
    private Long updatedBy;
    private LocalDateTime updatedOn;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private Boolean isActive;
}
