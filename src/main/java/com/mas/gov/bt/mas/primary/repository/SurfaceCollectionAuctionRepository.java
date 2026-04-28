package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionAuctionApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurfaceCollectionAuctionRepository
        extends JpaRepository<SurfaceCollectionAuctionApplication, Long> {

    Optional<SurfaceCollectionAuctionApplication> findByApplicationNo(String applicationNo);
}