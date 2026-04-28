package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_surface_collection_permit_review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfaceCollectionPermitReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * BG reference
     */
    @OneToOne
    @JoinColumn(name = "bg_id")
    private SurfaceCollectionBankGuarantee bankGuarantee;

    /**
     * Assigned ME
     */
    @Column(name = "assigned_me_id")
    private Long assignedMeId;

    /**
     * Assigned by system
     */
    @Column(name = "assigned_on")
    private LocalDateTime assignedOn;

    /**
     * Review action
     */
    @Column(name = "review_status")
    private String reviewStatus;
    // ASSIGNED / RESUBMISSION_REQUESTED / APPROVED

    /**
     * Remarks
     */
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    /**
     * Reassignment
     */
    @Column(name = "reassigned_to")
    private Long reassignedTo;

    @Column(name = "reassigned_on")
    private LocalDateTime reassignedOn;

    /**
     * Review completion
     */
    @Column(name = "reviewed_on")
    private LocalDateTime reviewedOn;
}