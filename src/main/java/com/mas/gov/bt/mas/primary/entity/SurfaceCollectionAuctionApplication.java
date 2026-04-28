package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "t_surface_collection_auction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfaceCollectionAuctionApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Auction Application Details
     */
    @Column(name = "application_no", unique = true)
    private String applicationNo;

    @Column(name = "location")
    private String location;

    @Column(name = "area", precision = 10, scale = 2)
    private BigDecimal area; // in acres

    @Column(name = "material")
    private String material;

    /**
     * EC / FC Workflow Tracking
     */
    @Column(name = "ec_status")
    private String ecStatus; // PENDING / APPROVED / REJECTED

    @Column(name = "fc_status")
    private String fcStatus; // PENDING / APPROVED / REJECTED

    @Column(name = "submitted_for_ec")
    private Boolean submittedForEc = false;

    @Column(name = "submitted_for_fc")
    private Boolean submittedForFc = false;

    @Column(name = "ec_approved_on")
    private LocalDateTime ecApprovedOn;

    @Column(name = "fc_approved_on")
    private LocalDateTime fcApprovedOn;

    /**
     * Auction Process
     */
    @Column(name = "auction_status")
    private String auctionStatus; 
    // DRAFT / EC_PENDING / EC_APPROVED / AUCTION_COMPLETED / BG_PENDING / PERMIT_GENERATED

    @Column(name = "auction_completed")
    private Boolean auctionCompleted = false;

    /**
     * BG Request
     */
    @Column(name = "bg_requested")
    private Boolean bgRequested = false;

    @Column(name = "bg_instruction", columnDefinition = "TEXT")
    private String bgInstruction;

    /**
     * Permit
     */
    @Column(name = "permit_generated")
    private Boolean permitGenerated = false;

    /**
     * Audit Fields
     */
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    /**
     * Attachments
     */
    @OneToMany(mappedBy = "auctionApplication",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<SurfaceCollectionAttachment> attachments;

    /**
     * Bid Winner
     */
    @OneToOne(mappedBy = "auctionApplication",
            cascade = CascadeType.ALL)
    private SurfaceCollectionBidWinner bidWinner;
}