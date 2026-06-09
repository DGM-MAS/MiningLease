package com.mas.gov.bt.mas.primary.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualMiningEntryResponseDTO {

    private Long id;
    private String applicationNo;
    private String activityType;
    private String status;
    private Boolean isManualEntry;

    // Applicant / Company
    private String applicantType;
    private String applicantCid;
    private String applicantName;
    private String applicantContact;
    private String applicantEmail;
    private String postalAddress;
    private String telephoneNo;
    private String licenseNo;
    private String businessLicenseNo;
    private String companyRegistrationNo;
    private String companyName;
    private String companyType;

    // Location
    private String dzongkhag;
    private String gewog;
    private String nearestVillage;
    private String placeOfActivity;
    private String dungkhag;

    // Mining / Quarry — Details
    private String typeOfMines;
    private String typeOfMinerals;
    private String requiredInvestment;
    private String sourceOfFinance;
    private String technicalCompetenceExperience;
    private String workforceRequirementRecruitment;
    private String proposedLeasePeriod;
    private String srf;
    private String landPrivate;
    private String totalLand;

    // Mining / Quarry — Approved
    private String approvedArea;
    private String approvedErb;
    private String approvedLeasePeriod;
    private String approvedMineral;
    private LocalDate leaseStartDate;
    private LocalDate leaseEndDate;
    private Integer leasePeriodYears;
    private BigDecimal upfrontPaymentAmount;
    private String fmfsStatus;
    private String fmfsId;
    private String ecStatus;
    private LocalDate ecExpiryDate;
    private String mlaStatus;
    private String geologicalReportStatus;

    // Mining / Quarry — Documents
    private String pfsDocId;
    private String locationMapDocId;
    private String financialCapabilityDocId;
    private String explorationReportDocId;
    private String consentLetterDocId;
    private String geologicalReportDocId;
    private String fmfsDocId;
    private String llcDocId;
    private String notesheetDocId;
    private String mlaDocId;
    private Long fileUploadIdGr;
    private Long fileUploadIdKmz;
    private String fileUploadIdPA;
    private String fileUploadIdFC;
    private String fileUploadIdPublicClearance;
    private Long mpcdFileUploadIdPA;
    private Long mpcdFileUploadIdMa;
    private Long signedPFSId;
    private Long bankGuarantorDocId;
    private Long workOrderDocId;

    // Surface Collection / Stock Lifting — Details
    private String typeOfActivity;
    private String typeOfMaterials;
    private String collectionSite;
    private Double proposedAreaSrf;
    private Double proposedAreaStateLand;
    private Double proposedAreaPrivate;
    private Double proposedAreaRow;
    private String permitNo;
    private String ecNo;
    private String securityClearanceValidity;
    private String taxClearanceValidity;
    private Boolean isStateOwned;
    private String eligibleForExport;
    private Boolean isRpBased;

    // STOCK LIFTING FIELDS ----------------
    private String applicationFileId;
    private String rcReportFileId;
    private String iomFileId;
    private String permitFileId;

    // Surface Collection / Stock Lifting — Documents
    private String attachmentMapFileId;
    private String recommendationLetterFileId;
    private String scConsentLetterFileId;
    private String fcFileId;
    private String ieeFileId;
    private String empFileId;
    private String admApprovalFileId;
    private String undertakingFileId;
    private String bgFileId;
    private String mpcdReportFileId;
    private String miReportFileId;
    private String meReportFileId;
    private String scEcFileId;

    // General
    private String details;
    private Long promoterId;

    // Additional attachments
    private List<String> fileIds;

    // Audit
    private Long createdBy;
    private LocalDateTime createdOn;
}
