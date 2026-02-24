package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.MiningLeaseApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MiningLeaseApplicationRepository extends JpaRepository<MiningLeaseApplication, Long> {

    Optional<MiningLeaseApplication> findByApplicationNumber(String applicationNumber);

    @Query("SELECT MAX(CAST(SUBSTRING(a.applicationNumber, 9) AS int)) FROM MiningLeaseApplication a WHERE a.applicationNumber LIKE :prefix%")
    Integer findMaxSequenceByPrefix(@Param("prefix") String prefix);

    @Query("""
    SELECT MAX(
        CAST(
            SUBSTRING(a.applicationNumber, LENGTH(:prefix) + 1)
            AS int
        )
    )
    FROM MiningLeaseApplication a
    WHERE a.applicationNumber LIKE CONCAT(:prefix, '%')
""")
    Integer findMaxDraftSequenceByPrefix(@Param("prefix") String prefix);

    @Query(value = """
    SELECT
        u.id AS userId,
        u.email AS email,
        u.username AS userName,
        COUNT(u.id) AS workload
    FROM mas_db.users u
    JOIN mas_db.user_roles ur
        ON u.id = ur.user_id
    LEFT JOIN mas_db.role_permissions t
        ON t.role_id = ur.role_id
    WHERE ur.role_id = 21
      AND t.permission_id = 37
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    ORDER BY workload ASC
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findDirectorQuarrying();

    @Query("""
    SELECT a FROM MiningLeaseApplication a
    JOIN TaskManagement t
      ON a.applicationNumber = t.applicationNumber
    WHERE t.assignedToUserId = :userId
""")
    Page<MiningLeaseApplication> findApplicationsAssignedToUser(Long currentUserId, Pageable pageable);

    @Query("SELECT a FROM MiningLeaseApplication a WHERE a.applicantUserId = :userId AND a.currentStatus IN :statuses")
    Page<MiningLeaseApplication> findByApplicantUserIdAndStatusIn(Long userId, List<String> applicationStatus, Pageable pageable);
}
