package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Master entity for all applications across services.
 * Contains common application metadata.
 */
@Entity
@Table(name = "t_application_master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_number", unique = true, nullable = false, length = 30)
    private String applicationNumber;

    @Column(name = "service_code", nullable = false, length = 30)
    private String serviceCode;

    @Column(name = "service_application_id")
    private Long serviceApplicationId;

    // Applicant Info
    @Column(name = "applicant_user_id")
    private Long applicantUserId;

    // Status & Workflow
    @Column(name = "current_status", length = 30)
    private String currentStatus;

    // Timestamps
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    private String remarks;

    private String rejectionRemarks;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;
}
