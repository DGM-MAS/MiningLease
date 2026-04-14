package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "surface_collection_permit", schema = "mas_db")
public class SurfaceCollectionPermitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String applicationNo;
    private String permitNo;
    private String ecNo;

    // ── Section 1: Applicant / Eligibility ────────────────────────────────────
    @Column(name = "applicant_cid")
    private String applicantCid;

    @Column(name = "applicant_name")
    private String applicantName;

    @Column(name = "mobile_no")
    private String mobileNo;

    @Column(name = "email")
    private String email;

    @Column(name = "security_clearance_validity")
    private String securityClearanceValidity;

    @Column(name = "tax_clearance_validity")
    private String taxClearanceValidity;

    /** true = state-owned entity */
    @Column(name = "is_state_owned")
    private Boolean isStateOwned;

    // ── Section 2: Proposed Activity ──────────────────────────────────────────
    /** Comma-separated: Surface collection, Dredging, Stock lifting, Migration works */
    @Column(name = "type_of_activity")
    private String typeOfActivity;

    /** Comma-separated: Stones, Sand, Boulder, Minerals */
    @Column(name = "type_of_materials")
    private String typeOfMaterials;

    @Column(name = "place_village")
    private String placeVillage;

    @Column(name = "gewog")
    private String gewog;

    @Column(name = "dzongkhag")
    private String dzongkhag;

    /** Comma-separated: Land surface, Riverbeds/Riverbanks, Road Right of Way, Land Developments, Others */
    @Column(name = "collection_site")
    private String collectionSite;

    @Column(name = "proposed_area_srf")
    private Double proposedAreaSrf;

    @Column(name = "proposed_area_state_land")
    private Double proposedAreaStateLand;

    @Column(name = "proposed_area_private")
    private Double proposedAreaPrivate;

    @Column(name = "proposed_area_row")
    private Double proposedAreaRow;

    // ── Promoter File Uploads ──────────────────────────────────────────────────
    @Column(name = "attachment_map_file_id")
    private String attachmentMapFileId;

    @Column(name = "recommendation_letter_file_id")
    private String recommendationLetterFileId;

    @Column(name = "consent_letter_file_id")
    private String consentLetterFileId;

    // ── Post-Acceptance Uploads (IEE / EMP / FC etc.) ─────────────────────────
    @Column(name = "fc_file_id")
    private String fcFileId;

    @Column(name = "iee_file_id")
    private String ieeFileId;

    @Column(name = "emp_file_id")
    private String empFileId;

    @Column(name = "adm_approval_file_id")
    private String admApprovalFileId;

    @Column(name = "undertaking_file_id")
    private String undertakingFileId;

    @Column(name = "bg_file_id")
    private String bgFileId;

    // ── MPCD ──────────────────────────────────────────────────────────────────
    @Column(name = "assigned_mpcd_id")
    private Long assignedMpcdId;

    @Column(name = "mpcd_remarks")
    private String mpcdRemarks;

    @Column(name = "mpcd_report_file_id")
    private String mpcdReportFileId;

    @Column(name = "iom_file_id")
    private String iomFileId;

    // ── RC (Regional Coordinator) ─────────────────────────────────────────────
    @Column(name = "assigned_rc_id")
    private Long assignedRcId;

    @Column(name = "rc_remarks")
    private String rcRemarks;

    @Column(name = "rc_report_file_id")
    private String rcReportFileId;

    // ── MI (Mine Inspector) ───────────────────────────────────────────────────
    @Column(name = "assigned_mi_id")
    private Long assignedMiId;

    @Column(name = "mi_remarks")
    private String miRemarks;

    @Column(name = "mi_report_file_id")
    private String miReportFileId;

    // ── ME (Mining Engineer, MD) ──────────────────────────────────────────────
    @Column(name = "assigned_me_id")
    private Long assignedMeId;

    @Column(name = "me_remarks")
    private String meRemarks;

    @Column(name = "ec_file_id")
    private String ecFileId;

    // ── MD (Mine Director) ────────────────────────────────────────────────────
    @Column(name = "assigned_md_id")
    private Long assignedMdId;

    @Column(name = "md_remarks")
    private String mdRemarks;

    // ── Permit Type ───────────────────────────────────────────────────────────
    /** true = RP-based (eligible for export); false = Direct Allocation (domestic only) */
    @Column(name = "is_rp_based")
    private Boolean isRpBased;

    // ── Status & Workflow ─────────────────────────────────────────────────────
    private String status;

    /** Tracks who requested the current revision so promoter/focals know who to resubmit to */
    @Column(name = "revision_requested_by")
    private String revisionRequestedBy;

    /** Tracks who must act on the revision: PROMOTER | MPCD | RC | MI | ME */
    @Column(name = "revision_target_role")
    private String revisionTargetRole;

    @Column(name = "revision_count")
    private Integer revisionCount = 0;

    // ── Audit ─────────────────────────────────────────────────────────────────
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @Column(name = "created_on", updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @PrePersist
    public void onCreate() {
        this.createdOn = LocalDateTime.now();
        this.isActive = true;
        if (this.revisionCount == null) this.revisionCount = 0;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedOn = LocalDateTime.now();
    }
}
