package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.StockLiftingApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockLiftingRepository extends JpaRepository<StockLiftingApplication, Long> {

    @Query("""
        SELECT COUNT(a) FROM StockLiftingApplication a
        WHERE a.createdOn >= :monthStart AND a.createdOn < :monthEnd
    """)
    long countByMonth(
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);

    List<StockLiftingApplication> findByIsManualEntry(String isManual);

    List<StockLiftingApplication> findByIsManualEntryAndManualEntryBy(String isManual, Long userId);

    Optional<StockLiftingApplication> findByStockLiftingPermitNo(String appNo);

    Optional<StockLiftingApplication> findByApplicationNo(String appNo);

    /**
     * Stock lifting applications whose validity ends in [:from, :toExclusive) that have
     * not yet been sent the Mine Restoration Plan reminder for their current validity
     * date, and for which no MRP has been submitted yet (a saved draft does not count).
     */
    @Query("""
        SELECT a
        FROM StockLiftingApplication a
        WHERE a.validityDate IS NOT NULL
          AND a.validityDate >= :from
          AND a.validityDate < :toExclusive
          AND (a.mrpReminderSentForEndDate IS NULL
               OR a.mrpReminderSentForEndDate <> a.validityDate)
          AND NOT EXISTS (
              SELECT 1
              FROM MineRestorationApplication r
              WHERE r.miningLeaseApplicationNumber = a.applicationNo
                AND r.currentStatus <> :draftStatus
          )
    """)
    List<StockLiftingApplication> findApplicationsDueForMrpReminder(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("draftStatus") String draftStatus);
}
