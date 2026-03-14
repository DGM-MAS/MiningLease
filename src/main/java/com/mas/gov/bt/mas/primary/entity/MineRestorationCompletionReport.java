package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_mine_restoration_completion_report")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MineRestorationCompletionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restoration_application_number", nullable = false, length = 30)
    private String restorationApplicationNumber;

    // ========== Background of Mine (pulled from lease) ==========
    @Column(name = "name_of_mine", length = 255)
    private String nameOfMine;

    @Column(name = "lease_area_acres", length = 50)
    private String leaseAreaAcres;

    @Column(name = "name_of_lessee", length = 255)
    private String nameOfLessee;

    @Column(name = "location_image_doc_id", length = 100)
    private String locationImageDocId;

    // ========== Activities Undertaken ==========
    // Stored as JSON string: [{activityDescription, duration, cost}]
    @Column(name = "activities_undertaken", columnDefinition = "TEXT")
    private String activitiesUndertaken;

    // ========== Attachments ==========
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "pictorial_evidence_doc_id", length = 100)
    private String pictorialEvidenceDocId;

    @Column(name = "maps_and_plans_doc_id", length = 100)
    private String mapsAndPlansDocId;

    @Column(name = "other_doc_id", length = 100)
    private String otherDocId;

    // "DRAFT", "SUBMITTED", "ME_REVIEWED", "APPROVED", "REJECTED"
    @Column(name = "status", length = 200)
    private String status;

    @Column(name = "me_remarks", columnDefinition = "TEXT")
    private String meRemarks;

    @Column(name = "me_reviewed_at")
    private LocalDateTime meReviewedAt;

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
