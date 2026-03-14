package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.MineRestorationProgressReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MineRestorationProgressReportRepository extends JpaRepository<MineRestorationProgressReport, Long> {

    List<MineRestorationProgressReport> findByRestorationApplicationNumber(String applicationNumber);

    Page<MineRestorationProgressReport> findByRestorationApplicationNumber(String applicationNumber, Pageable pageable);

    // Count submitted reports to generate next progress report number
    @Query("""
        SELECT COUNT(r) FROM MineRestorationProgressReport r
        WHERE r.restorationApplicationNumber = :appNumber
        AND r.status != 'DRAFT'
    """)
    long countSubmittedReports(@Param("appNumber") String applicationNumber);

    // Latest submitted report pending RC verification
    @Query("""
        SELECT r FROM MineRestorationProgressReport r
        WHERE r.restorationApplicationNumber = :appNumber
        AND r.status = 'PROGRESS_REPORT_SUBMITTED'
        ORDER BY r.createdOn DESC
    """)
    List<MineRestorationProgressReport> findPendingVerification(@Param("appNumber") String applicationNumber);

    // Latest progress report by status for a given application
    @Query("""
        SELECT r FROM MineRestorationProgressReport r
        WHERE r.restorationApplicationNumber = :appNumber
        AND r.status = :status
        ORDER BY r.createdOn DESC
    """)
    List<MineRestorationProgressReport> findByRestorationApplicationNumberAndStatus(
            @Param("appNumber") String applicationNumber,
            @Param("status") String status);

    // Reports assigned to a specific RC
    Page<MineRestorationProgressReport> findByAssignedRcUserId(Long rcUserId, Pageable pageable);

    @Query("""
        SELECT r FROM MineRestorationProgressReport r
        WHERE r.assignedRcUserId = :userId
        AND LOWER(r.restorationApplicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    Page<MineRestorationProgressReport> findByAssignedRcUserIdAndSearch(
            @Param("userId") Long userId,
            @Param("search") String search,
            Pageable pageable);
}
