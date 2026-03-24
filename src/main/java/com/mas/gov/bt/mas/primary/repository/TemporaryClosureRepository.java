package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.MiningLeaseApplication;
import com.mas.gov.bt.mas.primary.entity.TemporaryClosureEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemporaryClosureRepository extends JpaRepository<TemporaryClosureEntity, Long> {
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
    WHERE ur.role_id = 19
      AND t.permission_id = 108
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    ORDER BY workload ASC
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findRCTemporaryClosure();

    @Query("""
    SELECT a FROM TemporaryClosureEntity a
    JOIN TaskManagement t
      ON a.applicationId = t.applicationNumber
    WHERE t.assignedToUserId = :currentUserId
""")
    Page<TemporaryClosureEntity> findApplicationsAssignedToUser(Long currentUserId, Pageable pageable);

    @Query("SELECT a FROM TemporaryClosureEntity a WHERE a.applicantUserId = :userId AND a.currentStatus IN :applicationStatus")
    Page<TemporaryClosureEntity> findByApplicantUserIdAndStatusIn(Long userId, List<String> applicationStatus, Pageable pageable);

    @Query("""
    SELECT q FROM TemporaryClosureEntity q
    WHERE q.applicantUserId = :userId
    AND LOWER(q.applicationId) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<TemporaryClosureEntity> findByAssignedToUserAndSearch(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM TemporaryClosureEntity q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationId
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('SUBMITTED', 'MI REVIEWED')
""")
    Page<TemporaryClosureEntity> findAssignedToUserRC(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM TemporaryClosureEntity q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationId
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('SUBMITTED', 'MI REVIEWED')
    AND LOWER(q.applicationId) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<TemporaryClosureEntity> findAssignedToUserAndSearchRC(Long userId, String search, Pageable pageable);

    @Query(value = """
    SELECT 
        u.id AS userId,
        u.email AS email,
        u.username AS userName
    FROM mas_db.users u
    WHERE u.id = :miFocalId
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findUserDetailsMI(Long miFocalId);

    @Query("""
    SELECT q
    FROM TemporaryClosureEntity q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationId
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('MI ASSIGNED')
""")
    Page<TemporaryClosureEntity> findAssignedToUserMI(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM TemporaryClosureEntity q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationId
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('MI ASSIGNED')
    AND LOWER(q.applicationId) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<TemporaryClosureEntity> findAssignedToUserAndSearchMI(Long userId, String search, Pageable pageable);

    TemporaryClosureEntity findByApplicationId(String applicationId);

    // All renewal applications (admin view)
    @Query("""
    SELECT q FROM TemporaryClosureEntity q
    WHERE LOWER(q.applicationId) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<TemporaryClosureEntity> findAllBySearch(String trim, Pageable pageable);
}
