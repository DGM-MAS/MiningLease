package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_surface_collection_bank_guarantee")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfaceCollectionBankGuarantee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Link to auction
     */
    @OneToOne
    @JoinColumn(name = "auction_id")
    private SurfaceCollectionAuctionApplication auctionApplication;

    /**
     * Winner / promoter
     */
    @Column(name = "promoter_id")
    private Long promoterId;

    /**
     * BG file
     */

    @Column(name = "bg_file_id")
    private String bgFileId;

    /**
     * MPCD instruction
     */
    @Column(name = "bg_instruction", columnDefinition = "TEXT")
    private String bgInstruction;

    /**
     * BG Status
     */
    @Column(name = "status")
    private String status;
    // PENDING_SUBMISSION / SUBMITTED / RESUBMITTED / APPROVED / REJECTED

    /**
     * Submission tracking
     */
    @Column(name = "submitted_on")
    private LocalDateTime submittedOn;

    @Column(name = "resubmitted_on")
    private LocalDateTime resubmittedOn;

    /**
     * MD review (future)
     */
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_on")
    private LocalDateTime reviewedOn;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}