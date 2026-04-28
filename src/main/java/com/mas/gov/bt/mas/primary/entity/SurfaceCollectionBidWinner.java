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

    @Column(name = "other_details", columnDefinition = "TEXT")
    private String otherDetails;

    /**
     * If bidder becomes promoter
     */
    @Column(name = "promoter_id")
    private Long promoterId;

    @OneToOne
    @JoinColumn(name = "auction_id")
    private SurfaceCollectionAuctionApplication auctionApplication;
}