package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "t_surface_collection_attachment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurfaceCollectionAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "attachment_type")
    private String attachmentType;
    // MAP_KMZ / IEE / EMP / ADM_APPROVAL / FC

    @ManyToOne
    @JoinColumn(name = "auction_id")
    private SurfaceCollectionAuctionApplication auctionApplication;
}