package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_surface_collection_auction_permit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfaceCollectionAuctionPermit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "permit_no")
    private String permitNo;

    @Column(name = "issue_permit_file_id")
    private String issuePermitFileId;

    @OneToOne
    @JoinColumn(name = "auction_id")
    private SurfaceCollectionAuctionApplication auctionApplication;

    @Column(name = "issued_by")
    private Long issuedBy;

    @Column(name = "issued_on")
    private LocalDateTime issuedOn;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "permit_status")
    private String permitStatus;
}