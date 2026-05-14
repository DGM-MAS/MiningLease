package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionAuctionApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SurfaceCollectionAuctionRepository
        extends JpaRepository<SurfaceCollectionAuctionApplication, Long> {

    Optional<SurfaceCollectionAuctionApplication> findByApplicationNo(String applicationNo);

    Page<SurfaceCollectionAuctionApplication> findByApplicationNoContainingIgnoreCaseOrLocationContainingIgnoreCase(String search, String search1, Pageable pageable);

    Page<SurfaceCollectionAuctionApplication> findByCreatedByAndAuctionStatusIn(Long userId, List<String> archivedStatuses, Pageable pageable);

    Page<SurfaceCollectionAuctionApplication> findByApplicationNoContainingIgnoreCaseOrLocationContainingIgnoreCaseAndAuctionStatusIn(String search, String search1, List<String> archivedStatuses, Pageable pageable);

    @Query("""
    SELECT MAX(CAST(SUBSTRING(a.applicationNo, 10) AS integer))
    FROM SurfaceCollectionAuctionApplication a
    WHERE a.applicationNo LIKE CONCAT(:prefix, '%')
""")
    Integer findMaxSequenceByPrefix(String prefix);
}