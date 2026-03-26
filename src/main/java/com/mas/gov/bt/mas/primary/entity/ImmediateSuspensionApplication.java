package com.mas.gov.bt.mas.primary.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_immediate_suspension_application")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImmediateSuspensionApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", unique = true, nullable = false, length = 30)
    private String applicationNumber;

    @Column(name = "application_from")
    private String applicationFrom;

    // Reference to master application
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_master_id")
    private ApplicationMaster applicationMaster;

    @Column(name = "promoter_user_id")
    private Long promoterUserId;

    @Column(name = "applicant_email", length = 255)
    private String applicantEmail;

    @Column(name = "applicant_name", length = 255)
    private String applicantName;

    @Column(name = "suspension_reason_id")
    private Long suspensionReasonId;

    @Column(name = "remarks_rc_mi")
    private String remarksRcMi;

    @Column(name = "rc_mi_reviewed_at")
    private LocalDateTime rcMiReviewedAt;

    @Column(name = "promoter_reviewed_at")
    private LocalDateTime promoterReviewedAt;

    @Column(name = "promoter_file_id")
    private Long promoterFileId;

    @Column(name = "mi_reviewed_at")
    private LocalDateTime miReviewedAt;

    @Column(name = "mi_file_id")
    private Long miFileId;

    // ========== Status & Workflow ==========
    @Column(name = "current_status", length = 30)
    private String currentStatus;

    // ========== Audit Fields ==========
    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
