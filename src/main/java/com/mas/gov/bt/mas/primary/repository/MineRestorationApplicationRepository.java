package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.MineRestorationApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MineRestorationApplicationRepository extends JpaRepository<MineRestorationApplication, Long> {

    Optional<MineRestorationApplication> findByApplicationNumber(String applicationNumber);

    Page<MineRestorationApplication> findByApplicantUserId(Long applicantUserId, Pageable pageable);

    @Query("""
        SELECT r FROM MineRestorationApplication r
        WHERE r.applicantUserId = :userId
        AND LOWER(r.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    Page<MineRestorationApplication> findByApplicantUserIdAndSearch(
            @Param("userId") Long userId,
            @Param("search") String search,
            Pageable pageable);

    // Applications assigned to ME
    Page<MineRestorationApplication> findByAssignedMeUserId(Long meUserId, Pageable pageable);

    @Query("""
        SELECT r FROM MineRestorationApplication r
        WHERE r.assignedMeUserId = :userId
        AND LOWER(r.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    Page<MineRestorationApplication> findByAssignedMeUserIdAndSearch(
            @Param("userId") Long userId,
            @Param("search") String search,
            Pageable pageable);

    // All active applications for RC/MI (not assigned to specific user, based on role)
    @Query("""
        SELECT r FROM MineRestorationApplication r
        WHERE r.currentStatus IN ('RESTORATION_IN_PROGRESS', 'PROGRESS_REPORT_SUBMITTED')
    """)
    Page<MineRestorationApplication> findActiveForRC(Pageable pageable);

    @Query("""
        SELECT r FROM MineRestorationApplication r
        WHERE r.currentStatus IN ('RESTORATION_IN_PROGRESS', 'PROGRESS_REPORT_SUBMITTED')
        AND LOWER(r.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    Page<MineRestorationApplication> findActiveForRCWithSearch(
            @Param("search") String search,
            Pageable pageable);

    // Auto-assign ME by least workload
    @Query(value = """
        SELECT
            u.id AS userId,
            u.email AS email,
            u.username AS userName,
            COUNT(r.id) AS workload
        FROM mas_db.users u
        JOIN mas_db.user_roles ur ON u.id = ur.user_id
        LEFT JOIN mas_db.t_mine_restoration_application r
            ON r.assigned_me_user_id = u.id
            AND r.current_status NOT IN ('ERB_RELEASED', 'ERB_UTILIZED', 'MRP_REJECTED')
        WHERE ur.role_id = 21
          AND u.account_status = 'ACTIVE'
        GROUP BY u.id, u.email, u.username
        ORDER BY workload ASC
        LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findMEWithLeastWorkload();

    // Sequence generation
    @Query("""
        SELECT MAX(
            CAST(SUBSTRING(r.applicationNumber, LENGTH(:prefix) + 1) AS int)
        )
        FROM MineRestorationApplication r
        WHERE r.applicationNumber LIKE CONCAT(:prefix, '%')
    """)
    Integer findMaxSequenceByPrefix(@Param("prefix") String prefix);
}
