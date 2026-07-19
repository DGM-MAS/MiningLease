package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Column(name = "site_name")
    private String siteName;

    /**
     * Auction Application Details
     */
    @Column(name = "application_no", unique = true)
    private String applicationNo;

    // Reference to master application
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_master_id")
    private ApplicationMaster applicationMaster;

    @Column(name = "location")
    private String location;

    @Column(name = "area", precision = 10, scale = 2)
    private BigDecimal area; // in acres

    @Column(name = "material")
    private String material;

    /**
     * EC / FC Workflow Tracking
     */

    @Column(name = "ec_file_id")
    private String fileECid;

    @Column(name = "ec_number")
    private String ecNumber;

    @Column(name = "ec_valid_upto")
    private LocalDate ecValidUpto;

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
     * Assigned Focal
     */
    @Column(name = "assigned_md_user_id")
    private Long assignedMdUserId;

    @Column(name = "md_reviewed_on")
    private LocalDateTime md_reviewed_on;

    @Column(name = "md_remarks", columnDefinition = "TEXT")
    private String mdRemarks;

    @Column(name = "approved_date")
    private LocalDate approvedDate;
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

    // Dzongkhag, gewog and village details have been saved
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dzongkhag_id")
    private DzongkhagLookup dzongkhagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gewog_id")
    private GewogLookup gewogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "village_id")
    private VillageLookup villageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private RegionMaster regionId;
}