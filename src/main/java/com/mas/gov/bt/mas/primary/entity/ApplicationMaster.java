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

    @Column(name = "service_id")
    private String serviceId;

    @Column(name = "service_application_id")
    private Long serviceApplicationId;

    // Applicant Info
    // Canonical column is applicant_id — mas-backend-masters, Quarrying-Lease, and the
    // citizen tracking dashboard all read/write it. This entity used to point at a
    // separate applicant_user_id column that nothing else read, so every application
    // submitted through this service was invisible to citizen tracking. Java field name
    // kept as applicantUserId to avoid touching the many call sites across this service.
    @Column(name = "applicant_id")
    private Long applicantUserId;

    // Status & Workflow
    @Column(name = "current_status", length = 30)
    private String currentStatus;

    // Timestamps
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "submitted_on")
    private LocalDateTime submittedOn;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // mas-backend-masters' citizen tracking dashboard splits In Process vs Archived on
    // completed_on IS NULL, not completed_at. This service only ever set completedAt, so
    // approved/completed applications here stayed stuck as "in process" forever on the
    // dashboard. Kept as a separate field (not a column retarget like applicant_id) since
    // submittedOn/completedOn need to coexist with submittedAt/completedAt going forward.
    @Column(name = "completed_on")
    private LocalDateTime completedOn;

    private String remarks;

    private String rejectionRemarks;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "site_id")
    private Long siteId;

    /** Keeps the _at/_on timestamp pairs in sync regardless of which one this service's code sets. */
    @PrePersist
    @PreUpdate
    private void syncTimestampColumnPairs() {
        if (submittedOn == null && submittedAt != null) submittedOn = submittedAt;
        if (submittedAt == null && submittedOn != null) submittedAt = submittedOn;
        if (completedOn == null && completedAt != null) completedOn = completedAt;
        if (completedAt == null && completedOn != null) completedAt = completedOn;
    }
}
