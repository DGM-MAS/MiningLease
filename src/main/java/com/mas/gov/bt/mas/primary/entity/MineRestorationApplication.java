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
@Table(name = "t_mine_restoration_application", schema = "mas_db")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MineRestorationApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", nullable = false, unique = true, length = 30)
    private String applicationNumber;

    // Reference to the original mining lease application
    @Column(name = "mining_lease_application_number", nullable = false, length = 30)
    private String miningLeaseApplicationNumber;

    // "MINE_CLOSURE" or "TERMINATION_SURRENDER"
    @Column(name = "restoration_type", nullable = false, length = 50)
    private String restorationType;

    // ========== Promoter details (pulled from lease) ==========
    @Column(name = "applicant_user_id")
    private Long applicantUserId;

    @Column(name = "applicant_name", length = 255)
    private String applicantName;

    @Column(name = "applicant_email", length = 255)
    private String applicantEmail;

    @Column(name = "applicant_contact", length = 20)
    private String applicantContact;

    // ========== Mine details (pulled from lease) ==========
    @Column(name = "name_of_mine", length = 255)
    private String nameOfMine;

    @Column(name = "lease_area_acres", length = 50)
    private String leaseAreaAcres;

    @Column(name = "dzongkhag", length = 100)
    private String dzongkhag;

    @Column(name = "gewog", length = 100)
    private String gewog;

    @Column(name = "lease_end_date")
    private LocalDate leaseEndDate;

    // ========== MRP (Mining Restoration Plan) ==========
    @Column(name = "mrp_doc_id", length = 100)
    private String mrpDocId;

    @Column(name = "mrp_submitted_at")
    private LocalDateTime mrpSubmittedAt;

    // ========== Work Order ==========
    @Column(name = "work_order_doc_id", length = 100)
    private String workOrderDocId;

    @Column(name = "work_order_issued_at")
    private LocalDateTime workOrderIssuedAt;

    // ========== Mining Engineer (ME) Review ==========
    @Column(name = "assigned_me_user_id")
    private Long assignedMeUserId;

    @Column(name = "remarks_me", columnDefinition = "TEXT")
    private String remarksME;

    @Column(name = "me_reviewed_at")
    private LocalDateTime meReviewedAt;

    // ========== ERB Decision: "RELEASED" or "UTILIZED" ==========
    @Column(name = "erb_decision", length = 20)
    private String erbDecision;

    @Column(name = "erb_decided_at")
    private LocalDateTime erbDecidedAt;

    @Column(name = "erb_remarks", columnDefinition = "TEXT")
    private String erbRemarks;

    // ========== Status ==========
    @Column(name = "current_status", length = 50)
    private String currentStatus;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ========== Audit ==========
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

    @Column(name = "is_active")
    private Boolean isActive = true;

    @PrePersist
    public void onCreate() {
        this.isActive = true;
    }
}
