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
@Table(name = "t_mine_restoration_progress_report")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MineRestorationProgressReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restoration_application_number", nullable = false, length = 30)
    private String restorationApplicationNumber;

    // Auto-incremented per application
    @Column(name = "progress_report_number")
    private Integer progressReportNumber;

    // ========== Background of Mine (pulled from lease) ==========
    @Column(name = "name_of_mine", length = 255)
    private String nameOfMine;

    @Column(name = "lease_area_acres", length = 50)
    private String leaseAreaAcres;

    @Column(name = "name_of_lessee", length = 255)
    private String nameOfLessee;

    @Column(name = "location_image_doc_id", length = 100)
    private String locationImageDocId;

    // ========== Progress Details ==========
    @Column(name = "start_date_of_mine_restoration")
    private LocalDate startDateOfMineRestoration;

    @Column(name = "date_of_progress_report")
    private LocalDate dateOfProgressReport;

    @Column(name = "activity_description", columnDefinition = "TEXT")
    private String activityDescription;

    @Column(name = "financial_progress", columnDefinition = "TEXT")
    private String financialProgress;

    @Column(name = "physical_progress", columnDefinition = "TEXT")
    private String physicalProgress;

    @Column(name = "pictorial_evidence_doc_id", length = 100)
    private String pictorialEvidenceDocId;

    // ========== RC/MI Verification ==========
    @Column(name = "assigned_rc_user_id")
    private Long assignedRcUserId;

    @Column(name = "verification_report_doc_id", length = 100)
    private String verificationReportDocId;

    @Column(name = "verification_submitted_at")
    private LocalDateTime verificationSubmittedAt;

    @Column(name = "verification_remarks", columnDefinition = "TEXT")
    private String verificationRemarks;

    // ========== ME Review ==========
    @Column(name = "me_remarks", columnDefinition = "TEXT")
    private String meRemarks;

    @Column(name = "me_reviewed_at")
    private LocalDateTime meReviewedAt;

    // "DRAFT", "SUBMITTED", "VERIFICATION_SUBMITTED", "ME_REVIEWED", "COMPLETION_REQUESTED"
    @Column(name = "status", length = 255)
    private String status;

    // ========== Audit ==========
    @Column(name = "submitted_by")
    private Long submittedBy;

    @CreationTimestamp
    @Column(name = "created_on", updatable = false)
    private LocalDateTime createdOn;

    @UpdateTimestamp
    @Column(name = "updated_on")
    private LocalDateTime updatedOn;
}
