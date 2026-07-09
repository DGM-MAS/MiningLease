package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.EnvironmentClearanceRenewal;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RenewalEnvironmentalClearanceRepository extends JpaRepository<EnvironmentClearanceRenewal, Long> {
    @Query("""
       SELECT MAX(
           CAST(
               SUBSTRING(
                   e.applicationNo,
                   LENGTH(:prefix) + 1
               ) AS integer
           )
       )
       FROM EnvironmentClearanceRenewal e
       WHERE e.applicationNo LIKE CONCAT(:prefix, '%')
       """)
    Integer findMaxSequenceByPrefix(
            @Param("prefix") String prefix
    );

    @Query(value = """
    SELECT
        u.id AS userId,
        u.email AS email,
        u.username AS userName,
        COUNT(ecr.id) AS workload
    FROM mas_db.users u
    JOIN mas_db.user_roles ur
        ON u.id = ur.user_id
    LEFT JOIN mas_db.t_environment_clearance_renewal ecr
        ON ecr.assignedmdid = u.id
        AND ecr.status NOT IN ('APPROVED', 'REJECTED', 'EC_RENEWED')
    WHERE ur.role_id = 21
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    ORDER BY workload ASC
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findMDEnvironmentalClearance();

    @Query(value = """
    SELECT
        u.id AS userId,
        u.email AS email,
        u.username AS username,
        COUNT(ecr.id) AS workload
    FROM mas_db.users u
    JOIN mas_db.user_roles ur
        ON u.id = ur.user_id
    LEFT JOIN mas_db.t_environment_clearance_renewal ecr
        ON ecr.assignedmpcdid = u.id
        AND ecr.status NOT IN ('APPROVED', 'REJECTED', 'EC_RENEWED')
    WHERE ur.role_id = 23
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    ORDER BY workload ASC
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findMPCDEnvironmentalClearance();


    @Query(value = """
    SELECT 
        u.id AS userId,
        u.email AS email,
        u.username AS userName
    FROM mas_db.users u
    WHERE u.id = :directorId
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findUserDetails(Long directorId);

    Page<EnvironmentClearanceRenewal> findByAssignedMPCDId(Long userId, Pageable pageable);

    Page<EnvironmentClearanceRenewal> findByAssignedMPCDIdAndApplicationNoContainingIgnoreCase(Long userId, String search, Pageable pageable);

    @Query("SELECT a FROM EnvironmentClearanceRenewal a WHERE a.status IN :applicationStatus AND a.createdBy = :userId ORDER BY a.createdOn DESC")
    Page<EnvironmentClearanceRenewal> findByApplicantUserIdAndStatusIn(Long userId, List<String> applicationStatus, Pageable pageable);

    @Query("""
    SELECT q FROM EnvironmentClearanceRenewal q
    WHERE q.createdBy = :userId
    AND q.status IN :applicationStatus
    AND LOWER(q.applicationNo) LIKE LOWER(CONCAT('%', :trim, '%'))
""")
    Page<EnvironmentClearanceRenewal> findByAssignedToUserAndSearch(Long userId, List<String> applicationStatus, String trim, Pageable pageable);

    Page<EnvironmentClearanceRenewal> findByStatusIn(List<String> approved, Pageable pageable);

    Page<EnvironmentClearanceRenewal> findByStatusInAndApplicationNoContainingIgnoreCase(List<String> approved, String search, Pageable pageable);

    Page<EnvironmentClearanceRenewal> findByCreatedByAndStatusIn(Long userId, List<String> approved, Pageable pageable);

    Page<EnvironmentClearanceRenewal> findByCreatedByAndStatusInAndApplicationNoContainingIgnoreCase(Long userId, List<String> approved, String search, Pageable pageable);

    Optional<EnvironmentClearanceRenewal> findByIdAndCreatedBy(Long id, Long userId);

    Optional<EnvironmentClearanceRenewal> findByApplicationNo(String applicationNumber);

    Page<EnvironmentClearanceRenewal> findByAssignedMPCDIdAndStatusIn(Long userId, List<String> archivedStatuses, Pageable pageable);

    Page<EnvironmentClearanceRenewal> findByAssignedMPCDIdAndApplicationNoContainingIgnoreCaseAndStatusIn(Long userId, String trim, List<String> archivedStatuses, Pageable pageable);

    Page<EnvironmentClearanceRenewal>
    findByAssignedRCId(
            Long userId,
            Pageable pageable
    );

    Page<EnvironmentClearanceRenewal>
    findByAssignedRCIdAndApplicationNoContainingIgnoreCase(
            Long userId,
            String search,
            Pageable pageable
    );

    Page<EnvironmentClearanceRenewal> findByAssignedRCIdAndStatusIn(Long userId, List<String> applicationStatuses, Pageable pageable);

    Page<EnvironmentClearanceRenewal> findByAssignedRCIdAndApplicationNoContainingIgnoreCaseAndStatusIn(Long userId, String trim, List<String> applicationStatuses, Pageable pageable);

    Page<EnvironmentClearanceRenewal> findByAssignedMIIdAndStatusIn(Long userId, List<String> applicationStatuses, Pageable pageable);

    Page<EnvironmentClearanceRenewal> findByAssignedMIIdAndApplicationNoContainingIgnoreCaseAndStatusIn(Long userId, String trim, List<String> applicationStatuses, Pageable pageable);

    Page<EnvironmentClearanceRenewal>
    findByAssignedMDIdAndStatusIn(
            Long userId,
            List<String> status,
            Pageable pageable
    );

    Page<EnvironmentClearanceRenewal>
    findByAssignedMDIdAndApplicationNoContainingIgnoreCaseAndStatusIn(
            Long userId,
            String search,
            List<String> status,
            Pageable pageable
    );
}
