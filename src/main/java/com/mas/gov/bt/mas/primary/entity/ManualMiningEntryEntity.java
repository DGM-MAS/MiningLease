package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "t_manual_mining_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualMiningEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_no", unique = true, nullable = false)
    private String applicationNo;

    // Reference to master application
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_master_id")
    private ApplicationMaster applicationMaster;

    // Enter Details
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "promoter_id")
    private Long promoterId;

    // Mine / Quarry / SC / Stock Lifting
    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;

    // Assigned Focal Details

    @Column(name = "assigned_chief_id")
    private Long assignedChiefId;

    @Column(name = "assigned_chief_remarks", columnDefinition = "TEXT")
    private String assignedChiefRemarks;

    @Column(name = "assigned_director_id")
    private Long assignedDirectorId;

    @Column(name = "assigned_director_remarks", columnDefinition = "TEXT")
    private String assignedDirectorRemarks;

    // Audit fields
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "status")
    private String status;

    @PrePersist
    protected void onCreate() {
        this.createdOn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedOn = LocalDateTime.now();
    }
}