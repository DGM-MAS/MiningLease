package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
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
public class ManualMiningEntryRequestDTO {

    // MINING_LEASE | QUARRY_LEASE | SURFACE_COLLECTION | STOCK_LIFTING
    @NotBlank(message = "Activity type is required")
    private String activityType;

    // -------------------------------------------------------
    // Applicant / Company Details  (all types)
    // -------------------------------------------------------
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

    // -------------------------------------------------------
    // Location (all types)
    // -------------------------------------------------------
    private String dzongkhag;
    private String gewog;
    private String nearestVillage;
    private String placeOfActivity;
    private String dungkhag;

    // -------------------------------------------------------
    // Mining Lease / Quarry Lease — Identity
    // -------------------------------------------------------
    private String nameOfMine;       // ML only
    private String nameOfQuarry;     // QL only
    private String ecFileId;         // ML + QL — newer EC requirement
    private String ecNumber;         // ML + QL — newer EC requirement
    private String mlaSignedDocId;   // QL only

    // -------------------------------------------------------
    // Mining Lease / Quarry Lease — Mine Details
    // -------------------------------------------------------
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

    // -------------------------------------------------------
    // Mining Lease / Quarry Lease — Approved / Final Details
    // -------------------------------------------------------
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

    // -------------------------------------------------------
    // Mining Lease / Quarry Lease — Documents
    // -------------------------------------------------------
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

    // -------------------------------------------------------
    // Surface Collection / Stock Lifting — Activity Details
    // -------------------------------------------------------
    // STOCK LIFTING FIELDS ----------------
    private String applicationFileId;
    private String rcReportFileId;
    private String iomFileId;
    private String permitFileId;

    // STOCK LIFTING
    // Newly added as per the client requirement
    private LocalDateTime validityDate;

    private String quantity;
    // New columns added
    // --------------------------------------
    // comma-separated: Surface collection, Dredging, Stock lifting, Migration works
    private String typeOfActivity;
    // comma-separated: Stones, Sand, Boulder, Minerals
    private String typeOfMaterials;
    // comma-separated: Land surface, Riverbeds/Riverbanks, Road Right of Way, Land Developments, Others
    private String collectionSite;
    private Double proposedAreaSrf;
    private Double proposedAreaStateLand;
    private Double proposedAreaPrivate;
    private Double proposedAreaRow;
    private String permitNo;
    private String ecNo;
    private LocalDate ecValidUpto;
    private String securityClearanceValidity;
    private String taxClearanceValidity;
    private Boolean isStateOwned;
    private String eligibleForExport;
    private Boolean isRpBased;

    // -------------------------------------------------------
    // Surface Collection / Stock Lifting — Documents
    // -------------------------------------------------------
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

    // -------------------------------------------------------
    // General
    // -------------------------------------------------------
    private String details;
    private Long promoterId;

    // Additional file attachments (generic)
    private List<String> fileIds;
}
