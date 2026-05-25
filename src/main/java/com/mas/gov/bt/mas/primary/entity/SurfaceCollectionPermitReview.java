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


    @Column(name = "application_no", unique = true)
    private String applicationNo;

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
     * Review completion
     */
    @Column(name = "reviewed_on")
    private LocalDateTime reviewedOn;
}