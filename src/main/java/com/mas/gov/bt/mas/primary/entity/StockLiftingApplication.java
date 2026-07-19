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
@Table(name = "stock_lifting_application", schema = "mas_db")
public class StockLiftingApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String applicationNo;

    private String stockLiftingPermitNo;

    // ── Promoter upload ──────────────────────────────────────────────────────────
    @Column(name = "application_file_id")
    private String applicationFileId;

    // ── Workflow state ───────────────────────────────────────────────────────────
    // Statuses:
    //   DRAFT                  – promoter saved but not yet submitted
    //   SUBMITTED              – promoter submitted; MPCD auto-assigned
    //   REVISION_REQUIRED      – MPCD asked promoter to revise
    //   RESUBMITTED            – promoter resubmitted after MPCD revision request
    //   ASSIGNED_TO_RC         – MPCD approved initial review; RC assigned for stock verify
    //   RC_REVISION_REQUESTED  – MPCD asked RC to revise the report
    //   RC_REPORT_SUBMITTED    – RC submitted (or resubmitted) verification report
    //   IOM_ISSUED             – MPCD issued IOM; ME auto-assigned
    //   FORWARDED_TO_ME        – application forwarded to Mining Engineer
    //   ME_REVISION_REQUIRED   – ME asked promoter/MPCD for more info
    //   APPROVED               – ME issued the permit
    //   REJECTED               – rejected at any stage
    @Column(name = "status")
    private String status;

    @Column(name = "revision_no")
    private Integer revisionNo;

    // ── Assignment ───────────────────────────────────────────────────────────────
    @Column(name = "assigned_mpcd_id")
    private Long assignedMpcdId;

    @Column(name = "assigned_rc_id")
    private Long assignedRcId;

    @Column(name = "assigned_me_id")
    private Long assignedMeId;

    // ── RC/MI ────────────────────────────────────────────────────────────────────
    @Column(name = "rc_report_file_id")
    private String rcReportFileId;

    @Column(name = "rc_remarks", length = 1000)
    private String rcRemarks;

    // ── MPCD ─────────────────────────────────────────────────────────────────────
    @Column(name = "mpcd_remarks", length = 1000)
    private String mpcdRemarks;

    @Column(name = "iom_file_id")
    private String iomFileId;

    // ── Mining Engineer ──────────────────────────────────────────────────────────
    @Column(name = "me_remarks", length = 1000)
    private String meRemarks;

    @Column(name = "permit_file_id")
    private String permitFileId;

    // Newly added as per the client requirement
    @Column(name = "validity_date")
    private LocalDateTime validityDate;

    @Column(name = "quantity")
    private String quantity;
    // New columns added

    // ── Audit ────────────────────────────────────────────────────────────────────
    @Column(updatable = false)
    private Long createdBy;

    @Column(updatable = false)
    private LocalDateTime createdOn;

    private LocalDateTime submittedOn;
    private LocalDateTime lastResubmittedOn;

    private String updatedBy;
    private LocalDateTime updatedOn;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // APPLICANT DETAILS

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

    @Column(name = "is_manual_entry")
    private String isManualEntry;

    @Column(name = "manual_entry_by")
    private Long manualEntryBy;

    @Column(name = "manual_entry_on")
    private LocalDateTime manualEntryOn;

    @Column(name = "name_of_stock_lifting")
    private String nameOfStockLifting;

    @Column(name = "dzongkhag")
    private String dzongkhag;

    @Column(name = "gewog")
    private String gewog;

    @Column(name = "place_village")
    private String placeVillage;

    @Column(name = "dzongkhag_id")
    private String dzongkhagId;

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
