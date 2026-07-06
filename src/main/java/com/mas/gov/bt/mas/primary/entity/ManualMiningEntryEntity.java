package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_manual_mining_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualMiningEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_no", unique = true, nullable = false)
    private String applicationNo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_master_id")
    private ApplicationMaster applicationMaster;

    // MINING_LEASE | QUARRY_LEASE | SURFACE_COLLECTION | STOCK_LIFTING
    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;

    @Column(name = "status")
    private String status;

    @Column(name = "is_manual_entry", nullable = false)
    private Boolean isManualEntry;

    // -------------------------------------------------------
    // Applicant / Company Details  (all types)
    // -------------------------------------------------------
    @Column(name = "applicant_type", length = 50)
    private String applicantType;

    // Not always a citizen ID — license/company registrants have no CID and
    // are identified here by license/company registration number instead.
    @Column(name = "applicant_cid", length = 50)
    private String applicantCid;

    @Column(name = "applicant_name", length = 255)
    private String applicantName;

    @Column(name = "applicant_contact", length = 20)
    private String applicantContact;

    @Column(name = "applicant_email", length = 255)
    private String applicantEmail;

    @Column(name = "postal_address", length = 500)
    private String postalAddress;

    @Column(name = "telephone_no", length = 20)
    private String telephoneNo;

    @Column(name = "license_no", length = 50)
    private String licenseNo;

    @Column(name = "business_license_no", length = 50)
    private String businessLicenseNo;

    @Column(name = "company_registration_no", length = 255)
    private String companyRegistrationNo;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "company_type", length = 255)
    private String companyType;

    // -------------------------------------------------------
    // Location (all types)
    // -------------------------------------------------------
    @Column(name = "dzongkhag", length = 100)
    private String dzongkhag;

    @Column(name = "gewog", length = 100)
    private String gewog;

    @Column(name = "nearest_village", length = 100)
    private String nearestVillage;

    @Column(name = "place_of_activity", length = 255)
    private String placeOfActivity;

    @Column(name = "dungkhag", length = 100)
    private String dungkhag;

    // -------------------------------------------------------
    // Mining Lease / Quarry Lease — Mine Details
    // -------------------------------------------------------
    @Column(name = "type_of_mines", length = 100)
    private String typeOfMines;

    @Column(name = "type_of_minerals", length = 255)
    private String typeOfMinerals;

    @Column(name = "required_investment", length = 100)
    private String requiredInvestment;

    @Column(name = "source_of_finance", length = 255)
    private String sourceOfFinance;

    @Column(name = "technical_competence_experience", columnDefinition = "TEXT")
    private String technicalCompetenceExperience;

    @Column(name = "workforce_requirement_recruitment", columnDefinition = "TEXT")
    private String workforceRequirementRecruitment;

    @Column(name = "proposed_lease_period", length = 50)
    private String proposedLeasePeriod;

    @Column(name = "srf", length = 100)
    private String srf;

    @Column(name = "land_private", length = 100)
    private String landPrivate;

    @Column(name = "total_land", length = 100)
    private String totalLand;

    // -------------------------------------------------------
    // Mining Lease / Quarry Lease — Approved / Final Details
    // -------------------------------------------------------
    @Column(name = "approved_area", length = 100)
    private String approvedArea;

    @Column(name = "approved_erb", length = 100)
    private String approvedErb;

    @Column(name = "approved_lease_period", length = 50)
    private String approvedLeasePeriod;

    @Column(name = "approved_mineral", length = 255)
    private String approvedMineral;

    @Column(name = "lease_start_date")
    private LocalDate leaseStartDate;

    @Column(name = "lease_end_date")
    private LocalDate leaseEndDate;

    @Column(name = "lease_period_years")
    private Integer leasePeriodYears;

    @Column(name = "upfront_payment_amount", precision = 19, scale = 2)
    private BigDecimal upfrontPaymentAmount;

    @Column(name = "fmfs_status", length = 30)
    private String fmfsStatus;

    @Column(name = "fmfs_id")
    private String fmfsId;

    @Column(name = "ec_status", length = 30)
    private String ecStatus;

    @Column(name = "ec_expiry_date")
    private LocalDate ecExpiryDate;

    @Column(name = "mla_status", length = 30)
    private String mlaStatus;

    @Column(name = "geological_report_status", length = 30)
    private String geologicalReportStatus;

    // -------------------------------------------------------
    // Mining Lease / Quarry Lease — Documents
    // -------------------------------------------------------
    @Column(name = "pfs_doc_id", length = 100)
    private String pfsDocId;

    @Column(name = "location_map_doc_id", length = 100)
    private String locationMapDocId;

    @Column(name = "financial_capability_doc_id", length = 100)
    private String financialCapabilityDocId;

    @Column(name = "exploration_report_doc_id", length = 100)
    private String explorationReportDocId;

    @Column(name = "consent_letter_doc_id", length = 100)
    private String consentLetterDocId;

    @Column(name = "geological_report_doc_id", length = 100)
    private String geologicalReportDocId;

    @Column(name = "fmfs_doc_id", length = 100)
    private String fmfsDocId;

    @Column(name = "llc_doc_id", length = 100)
    private String llcDocId;

    @Column(name = "notesheet_doc_id", length = 100)
    private String notesheetDocId;

    @Column(name = "mla_doc_id", length = 100)
    private String mlaDocId;

    @Column(name = "file_upload_id_gr")
    private Long fileUploadIdGr;

    @Column(name = "file_upload_id_kmz")
    private Long fileUploadIdKmz;

    @Column(name = "file_upload_id_pa", length = 255)
    private String fileUploadIdPA;

    @Column(name = "file_upload_id_fc", length = 255)
    private String fileUploadIdFC;

    @Column(name = "file_upload_id_public_clearance", length = 255)
    private String fileUploadIdPublicClearance;

    @Column(name = "mpcd_file_upload_id_pa")
    private Long mpcdFileUploadIdPA;

    @Column(name = "mpcd_file_upload_id_ma")
    private Long mpcdFileUploadIdMa;

    @Column(name = "signed_pfs_id")
    private Long signedPFSId;

    @Column(name = "bank_guarantor_doc_id")
    private Long bankGuarantorDocId;

    @Column(name = "work_order_doc_id")
    private Long workOrderDocId;

    // -------------------------------------------------------
    // Surface Collection / Stock Lifting — Activity Details
    // -------------------------------------------------------

    // comma-separated: Surface collection, Dredging, Stock lifting, Migration works
    @Column(name = "type_of_activity", columnDefinition = "TEXT")
    private String typeOfActivity;

    // comma-separated: Stones, Sand, Boulder, Minerals
    @Column(name = "type_of_materials", columnDefinition = "TEXT")
    private String typeOfMaterials;

    // comma-separated: Land surface, Riverbeds/Riverbanks, Road Right of Way, Land Developments, Others
    @Column(name = "collection_site", columnDefinition = "TEXT")
    private String collectionSite;

    @Column(name = "proposed_area_srf")
    private Double proposedAreaSrf;

    @Column(name = "proposed_area_state_land")
    private Double proposedAreaStateLand;

    @Column(name = "proposed_area_private")
    private Double proposedAreaPrivate;

    @Column(name = "proposed_area_row")
    private Double proposedAreaRow;

    @Column(name = "permit_no", length = 100)
    private String permitNo;

    @Column(name = "ec_no", length = 100)
    private String ecNo;

    @Column(name = "security_clearance_validity", length = 100)
    private String securityClearanceValidity;

    @Column(name = "tax_clearance_validity", length = 100)
    private String taxClearanceValidity;

    @Column(name = "is_state_owned")
    private Boolean isStateOwned;

    @Column(name = "eligible_for_export", length = 10)
    private String eligibleForExport;

    // true = RP-based (eligible for export); false = Direct Allocation (domestic only)
    @Column(name = "is_rp_based")
    private Boolean isRpBased;

    // -------------------------------------------------------
    // Surface Collection / Stock Lifting — Documents
    // -------------------------------------------------------
    @Column(name = "attachment_map_file_id", length = 255)
    private String attachmentMapFileId;

    @Column(name = "recommendation_letter_file_id", length = 255)
    private String recommendationLetterFileId;

    @Column(name = "sc_consent_letter_file_id", length = 255)
    private String scConsentLetterFileId;

    @Column(name = "fc_file_id", length = 255)
    private String fcFileId;

    @Column(name = "iee_file_id", length = 255)
    private String ieeFileId;

    @Column(name = "emp_file_id", length = 255)
    private String empFileId;

    @Column(name = "adm_approval_file_id", length = 255)
    private String admApprovalFileId;

    @Column(name = "undertaking_file_id", length = 255)
    private String undertakingFileId;

    @Column(name = "bg_file_id", length = 255)
    private String bgFileId;

    @Column(name = "mpcd_report_file_id", length = 255)
    private String mpcdReportFileId;

    @Column(name = "iom_file_id", length = 255)
    private String iomFileId;

    @Column(name = "rc_report_file_id", length = 255)
    private String rcReportFileId;

    @Column(name = "mi_report_file_id", length = 255)
    private String miReportFileId;

    @Column(name = "me_report_file_id", length = 255)
    private String meReportFileId;

    @Column(name = "sc_ec_file_id", length = 255)
    private String scEcFileId;

    // -------------------------------------------------------
    // General
    // -------------------------------------------------------
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "promoter_id")
    private Long promoterId;

    // -------------------------------------------------------
    // Audit
    // -------------------------------------------------------
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        this.createdOn = LocalDateTime.now();
        this.isManualEntry = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedOn = LocalDateTime.now();
    }
}
