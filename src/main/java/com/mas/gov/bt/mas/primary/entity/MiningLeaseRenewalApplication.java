package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_mining_lease_renewal_application")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiningLeaseRenewalApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", nullable = false, length = 30)
    private String applicationNumber;

    @Column(name = "applicant_name", length = 255)
    private String applicantName;

    // APPLICANT DETAILS
    @Column(name = "applicant_contact", length = 20)
    private String applicantContact;

    @Column(name = "applicant_email", length = 255)
    private String applicantEmail;

    @Column(name = "applicant_cid", length = 11)
    private String applicantCid;

    @Column(name = "applicant_type", length = 50)
    private String applicantType;

    @Column(name = "postal_address", length = 500)
    private String postalAddress;

    @Column(name = "telephone_no", length = 20)
    private String telephoneNo;

    // ========== Lease Period ==========
    @Column(name = "lease_end_date")
    private LocalDate leaseEndDate;

    @Column(name = "lease_period_years")
    private Integer leasePeriodYears;

    @Column(name = "proposed_lease_renewal_period")
    private Integer proposedLeaseRenewalPeriod;

    // Reference to master application
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_master_id")
    private ApplicationMaster applicationMaster;

    // LOCATION OF THE MINES
    @Column(name = "place_of_mining_activity", length = 255)
    private String placeOfMiningActivity;

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

    @Column(name = "deposit_assessment_record_id")
    private String DepositAssessmentReportId;

    @Column(name = "declaration_status")
    private boolean declarationStatus;

    // APPLICANT FMFS DOC details
    @Column(name = "fmfs_doc_id", length = 100)
    private String fmfsDocId;

    @Column(name = "fmfs_id")
    private String fmfsId;

    @Column(name = "fmfs_status", length = 30)
    private String fmfsStatus;

    // Focal review details and file upload part

    // ========== Mining Engineer (ME) Review ==========
    @Column(name = "remarks_mine_engineer", columnDefinition = "TEXT")
    private String remarksME;

    @Column(name = "mine_engineer_reviewed_at")
    private LocalDateTime meReviewedAt;

    @Column(name = "llc_mine_engineer_doc_id")
    private String llcMineEngineerDocId;

    @Column(name = "notesheet_doc_id", length = 100)
    private String noteSheetDocId;

    @Column(name = "work_order_doc_id")
    private Long workOrderDocId;

    @Column(name = "work_order_remarks")
    private String workOrderRemarks;

    @Column(name = "mla_doc_id", length = 100)
    private String mlaDocId;

    @Column(name = "mla_status", length = 30)
    private String mlaStatus;

    @Column(name = "mla_signed_at")
    private LocalDateTime mlaSignedAt;

    // ========== Director Review ==========
    @Column(name = "remarks_director", columnDefinition = "TEXT")
    private String remarksDirector;

    @Column(name = "director_reviewed_at")
    private LocalDateTime directorReviewedAt;

    // ========== GEOLOGIST FOCAL =============== //

    @Column(name = "remarks_geologist")
    private String remarksGeologist;

    @Column(name = "geologist_reviewed_at")
    private LocalDateTime geologistReviewedAt;

    @Column(name = "geological_report_status", length = 30)
    private String geologicalReportStatus;

    // =========== MINING CHIEF ================== //

    @Column(name = "remarks_chief", columnDefinition = "TEXT")
    private String remarksChief;

    @Column(name = "chief_reviewed_at")
    private LocalDateTime chiefReviewedAt;


    // TASK status and application details

    @Column(name = "erb_regularization_required")
    private Boolean erbRegularizationRequired;

    @Column(name = "payable_amount", precision = 19, scale = 2)
    private java.math.BigDecimal payableAmount;

    @Column(name = "current_status", length = 30)
    private String currentStatus;

    // ========== Audit Fields ==========
    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_on", updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_by")
    private Long updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /* ===== Soft Delete ===== */
    @Column(name = "is_active")
    private Boolean isActive = true;

    @PrePersist
    public void onCreate() {
        this.createdOn = LocalDateTime.now();
        this.isActive = true;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedOn = LocalDateTime.now();
    }

}
