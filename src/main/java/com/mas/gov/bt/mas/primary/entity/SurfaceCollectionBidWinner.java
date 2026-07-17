package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "t_surface_collection_bid_winner")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfaceCollectionBidWinner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bid_winner_name")
    private String bidWinnerName;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "company_registration_number")
    private String companyRegistrationNumber;

    @Column(name = "company_type")
    private String companyType;

    @Column(name = "cid_number")
    private String cidNumber;

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


    /**
     * If bidder becomes promoter
     */
    @Column(name = "promoter_id")
    private Long promoterId;

    @OneToOne
    @JoinColumn(name = "auction_id")
    private SurfaceCollectionAuctionApplication auctionApplication;

    @Column(name = "bid_amount")
    private String bidAmount;
}