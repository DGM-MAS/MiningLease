package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionPermitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SurfaceCollectionPermitRepository extends JpaRepository<SurfaceCollectionPermitEntity, Long> {

    @Query("""
        SELECT COUNT(e) FROM SurfaceCollectionPermitEntity e
        WHERE e.createdOn >= :monthStart AND e.createdOn < :monthEnd
    """)
    long countByMonth(
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);

    Optional<SurfaceCollectionPermitEntity> findByApplicationNo(String applicationNumber);

    @Query("""
        SELECT MAX(CAST(SUBSTRING(a.applicationNo, LENGTH(:prefix) + 1) AS int))
        FROM SurfaceCollectionPermitEntity a
        WHERE a.applicationNo LIKE CONCAT(:prefix, '%')
    """)
    Integer findMaxSequenceByPrefix(@Param("prefix") String prefix);

    List<SurfaceCollectionPermitEntity> findByIsManualEntry(String isManualEntry);

    List<SurfaceCollectionPermitEntity> findByIsManualEntryAndManualEntryBy(String isManualEntry, Long manualEntryBy);
}
