package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "t_mining_lease_application")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiningLeaseApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", unique = true, nullable = false, length = 30)
    private String applicationNumber;

    // Reference to master application
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_master_id")
    private ApplicationMaster applicationMaster;

    @Column(name = "applicant_user_id")
    private Long applicantUserId;

    // ========== Applicant Information ==========
    @Column(name = "applicant_type", length = 50)
    private String applicantType;

    @Column(name = "application_type", length = 50)
    private String applicationType;

    @Column(name = "applicant_cid", length = 11)
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

    // ========== Mining Details ==========
    @Column(name = "type_of_mines", length = 100)
    private String typeOfMines;

    @Column(name = "type_of_minerals_products", length = 255)
    private String typeOfMineralsProducts;

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

    // ========== Land Details ==========
    @Column(name = "srf", length = 100)
    private String srf;

    @Column(name = "land_private", length = 100)
    private String landPrivate;

    @Column(name = "total_land", length = 100)
    private String totalLand;

    @Column(name = "place_of_mining_activity", length = 255)
    private String placeOfMiningActivity;

    // ========== Location Details ==========
    @Column(name = "dungkhag", length = 100)
    private String dungkhag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dzongkhag_id")
    private DzongkhagLookup dzongkhag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gewog_id")
    private GewogLookup gewog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "village_id")
    private VillageLookup nearestVillage;

    // =========== Client file uplaod =========== //
    @Column(name = "file_upload_id_gr")
    private Long fileUploadIdGr;

    @Column(name = "file_upload_id_pa")
    private String fileUploadIdPA;

    @Column(name = "file_upload_id_fc")
    private String fileUploadIdFC;

    @Column(name = "file_upload_id_public_clearance")
    private String fileUploadIdPublicClearance;

    // =========== MPCD Focal =================
    @Column(name = "mpcd_file_upload_id_pa")
    private Long mpcdFileUploadIdPA;

    @Column(name = "mpcd_file_upload_id_ma")
    private Long mpcdFileUploadIdMa;

    @Column(name = "remarks_mpcd")
    private String remarksMPCD;

    // ========== GEOLOGIST FOCAL =============== //

    @Column(name = "remarks_geologist")
    private String remarksGeologist;

    @Column(name = "geological_report_status", length = 30)
    private String geologicalReportStatus;

    // ========== Mining Engineer (ME) Review ==========
    @Column(name = "remarks_me", columnDefinition = "TEXT")
    private String remarksME;

    @Column(name = "fmfs_status", length = 30)
    private String fmfsStatus;

    @Column(name = "ec_status", length = 30)
    private String eCStatus;

    @Column(name = "ec_expiry_date", length = 30)
    private Date eCExpiryDate;

    @Column(name = "approved_area", length = 100)
    private String approvedArea;

    @Column(name = "approved_erb", length = 100)
    private String approvedErb;

    @Column(name = "approved_lease_period", length = 50)
    private String approvedLeasePeriod;

    @Column(name = "approved_mineral", length = 255)
    private String approvedMineral;

    @Column(name = "llc_doc_id", length = 100)
    private String llcDocId;

    @Column(name = "notesheet_doc_id", length = 100)
    private String notesheetDocId;

    @Column(name = "mla_doc_id", length = 100)
    private String mlaDocId;

    @Column(name = "mla_status", length = 30)
    private String mlaStatus;

    @Column(name = "mla_signed_at")
    private LocalDateTime mlaSignedAt;

    @Column(name = "remarks_chief", columnDefinition = "TEXT")
    private String remarksChief;

    // ========== Director Review ==========
    @Column(name = "remarks_director", columnDefinition = "TEXT")
    private String remarksDirector;

    // ========== Stage Timestamps ==========
    @Column(name = "mpcd_reviewed_at")
    private LocalDateTime mpcdReviewedAt;

    @Column(name = "geologist_reviewed_at")
    private LocalDateTime geologistReviewedAt;

    @Column(name = "me_reviewed_at")
    private LocalDateTime meReviewedAt;

    @Column(name = "chief_reviewed_at")
    private LocalDateTime chiefReviewedAt;

    @Column(name = "director_reviewed_at")
    private LocalDateTime directorReviewedAt;

    // ========== Status & Workflow ==========
    @Column(name = "current_status", length = 30)
    private String currentStatus;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ========== Lease Period ==========
    @Column(name = "lease_start_date")
    private LocalDate leaseStartDate;

    @Column(name = "lease_end_date")
    private LocalDate leaseEndDate;

    @Column(name = "lease_period_years")
    private Integer leasePeriodYears;

    // ========== Fees ==========
    @Column(name = "application_fees_required")
    private Boolean applicationFeesRequired;

    @Column(name = "application_fees_amount")
    private BigDecimal applicationFeesAmount;

    // ========== Document IDs ==========
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

    @Column(name = "fmfs_id")
    private String fmfsId;

    // ========== Flags ==========
    @Column(name = "is_active")
    private Boolean isActive = true;

    // ========== Audit Fields ==========
    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "bank_gurantor_doc_id")
    private Long bankGuarantorDocId;

    @Column(name = "upfront_payment_amount")
    private BigDecimal upfrontPaymentAmount;

    @Column(name = "work_order_doc_id")
    private Long workOrderDocId;

    @Column(name = "work_order_remarks")
    private String workOrderRemarks;
}
