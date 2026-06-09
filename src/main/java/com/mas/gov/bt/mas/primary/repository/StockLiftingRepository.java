package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.StockLiftingApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface StockLiftingRepository extends JpaRepository<StockLiftingApplication, Long> {

    @Query("""
        SELECT COUNT(a) FROM StockLiftingApplication a
        WHERE a.createdOn >= :monthStart AND a.createdOn < :monthEnd
    """)
    long countByMonth(
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);
}
