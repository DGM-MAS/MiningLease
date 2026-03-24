package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_temporary_closure")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryClosureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, length = 30)
    private String applicationId;

    // Reference to master application
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_master_id")
    private ApplicationMaster applicationMaster;

    @Column(name = "applicant_user_id")
    private Long applicantUserId;

    // ========== Applicant Information ==========
    @Column(name = "applicant_type", length = 50)
    private String applicantType;

    @Column(name = "applicant_cid", length = 11)
    private String applicantCid;

    @Column(name = "applicant_name", length = 255)
    private String applicantName;

    @Column(name = "applicant_email", length = 255)
    private String applicantEmail;

    @Column(name = "applicant_file_id")
    private Long applicantFileId;


    @Column(name = "reason_for_closure")
    private String reasonForClosure;

    @Column(name = "number_of_months_for_closure")
    private Long numberOfMonthsForClosure;

    @Column(name = "remarks_applicant")
    private String remarksApplicant;

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

    @Column(name = "remark_mi", columnDefinition = "TEXT")
    private String remarksMI;

    @Column(name = "mi_reviewed_at")
    private LocalDateTime miReviewedAt;

    @Column(name = "file_id_mi")
    private Long fileIdMI;

    // RC Review
    @Column(name = "remarks_rc")
    private String remarksRC;

    @Column(name = "rc_reviewed_at")
    private LocalDateTime rcReviewedAt;

    @Column(name = "file_upload_id_rc")
    private Long fileUploadIdRC;

}
