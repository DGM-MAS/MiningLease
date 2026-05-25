package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurfaceCollectionAttachmentRepository
        extends JpaRepository<SurfaceCollectionAttachment, Long> {

    List<SurfaceCollectionAttachment>
    findByAuctionApplicationId(Long auctionId);
}