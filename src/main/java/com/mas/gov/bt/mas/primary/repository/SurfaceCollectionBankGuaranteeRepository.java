package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionBankGuarantee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurfaceCollectionBankGuaranteeRepository
        extends JpaRepository<SurfaceCollectionBankGuarantee, Long> {

    Optional<SurfaceCollectionBankGuarantee> findByAuctionApplicationId(Long auctionId);

    Optional<SurfaceCollectionBankGuarantee> findByPromoterId(Long promoterId);
}