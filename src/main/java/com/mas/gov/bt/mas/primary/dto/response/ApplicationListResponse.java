package com.mas.gov.bt.mas.primary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Response DTO for application list item.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationListResponse {

    private Long id;
    private String applicationNumber;
    private Long applicantUserId;

    // Applicant Information
    private String applicantType;
    private String applicationType;
    private String applicantCid;
    private String applicantName;
    private String applicantContact;
    private String applicantEmail;
    private String postalAddress;
    private String telephoneNo;
    private String licenseNo;
    private String businessLicenseNo;
    private String companyName;

    // Mining Details
    private String typeOfMines;
    private String typeOfMineralsProducts;
    private String requiredInvestment;
    private String sourceOfFinance;
    private String technicalCompetenceExperience;
    private String workforceRequirementRecruitment;
    private String proposedLeasePeriod;

    // Land Details
    private String srf;
    private String landPrivate;
    private String totalLand;
    private String placeOfMiningActivity;

    // Location Details
    private String dungkhag;
    private String dzongkhagName;
    private String gewog;
    private String nearestVillage;

    // Status
    private String currentStatus;
    private String currentStatusDisplayName;

    // Timestamps
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;

    // Lease Period
    private LocalDate leaseStartDate;
    private LocalDate leaseEndDate;
    private Integer leasePeriodYears;

    // Fees
    private Boolean applicationFeesRequired;

    // MPCD Review
    private String remarksMPCD;
    private Long mpcdFileUploadIdPA;

    // Geologist Review
    private String remarksGeologist;
    private String geologicalReportStatus;

    // Mining Engineer Review
    private String remarksME;
    private String fmfsStatus;
    private String eCStatus;
    private Date eCExpiryDate;
    private String approvedArea;
    private String approvedErb;
    private String approvedLeasePeriod;
    private String approvedMineral;
    private String llcDocId;
    private String notesheetDocId;
    private String mlaDocId;
    private String mlaStatus;
    private LocalDateTime mlaSignedAt;

    // Mining Chief Review
    private String remarksChief;

    // Director Review
    private String remarksDirector;
    private String mlaSignedDocId;

    // Stage Timestamps
    private LocalDateTime mpcdReviewedAt;
    private LocalDateTime geologistReviewedAt;
    private LocalDateTime meReviewedAt;
    private LocalDateTime chiefReviewedAt;
    private LocalDateTime directorReviewedAt;

    // Document IDs
    private String pfsDocId;
    private String locationMapDocId;
    private String financialCapabilityDocId;
    private String explorationReportDocId;
    private String consentLetterDocId;
    private String geologicalReportDocId;
    private String fmfsDocId;

    private Boolean isActive;

    private BigDecimal applicationFeesAmount;

    // Audit fields
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    private Long bankGuarantorDocId;

    private BigDecimal upfrontPaymentAmount;

    private Long workOrderDocId;

    private String workOrderRemarks;
}
