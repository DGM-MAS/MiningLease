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
    WHERE t.assignedToUserId = :currentUserId
""")
    Page<MiningLeaseApplication> findApplicationsAssignedToUser(Long currentUserId, Pageable pageable);

    @Query("SELECT a FROM MiningLeaseApplication a WHERE a.applicantUserId = :userId AND a.currentStatus IN :applicationStatus")
    Page<MiningLeaseApplication> findByApplicantUserIdAndStatusIn(Long userId, List<String> applicationStatus, Pageable pageable);

    @Query("SELECT a FROM MiningLeaseApplication a WHERE a.currentStatus IN :statuses")
    Page<MiningLeaseApplication> findByStatusIn(@Param("statuses") List<String> statuses, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('MINING LEASE APPROVED')
""")
    Page<MiningLeaseApplication> findArchivedAssignedToUserMPCD(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('MINING LEASE APPROVED', 'REJECTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findArchivedAssignedToUserAndSearchMPCD(Long userId, String search, Pageable pageable);

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

    @Query(value = """
    SELECT
        u.id AS userId,
        u.email AS email,
        u.username AS userName,
        COUNT(t.id) AS workload
    FROM mas_db.users u
    JOIN mas_db.user_roles ur
        ON u.id = ur.user_id
    LEFT JOIN mas_db.t_task_management t
        ON t.assigned_to_user_id = u.id
        AND t.task_status IN ('PENDING', 'ASSIGNED')
    WHERE ur.role_id = :roleId
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    ORDER BY workload ASC
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findLeastBusyMineEngineer(Long roleId);


    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('GR SUBMITTED', 'NOTE SHEET UPLOADED', 'MLA SUBMITTED')
""")
    Page<MiningLeaseApplication> findAssignedToUserDirector(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('GR SUBMITTED', 'NOTE SHEET UPLOADED', 'MLA SUBMITTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findAssignedToUserAndSearchDirector(Long userId, String search, Pageable pageable);

    @Query(value = """
    SELECT 
        u.id AS userId,
        u.email AS email,
        u.username AS userName
    FROM mas_db.users u
    WHERE u.id = :mpcdFocalId
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findUserDetailsMPCD(Long mpcdFocalId);

    @Query(value = """
    SELECT 
        u.id AS userId,
        u.email AS email,
        u.username AS userName
    FROM mas_db.users u
    WHERE u.id = :mineEngineerFocalId
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findUserDetailsME(Long mineEngineerFocalId);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('ASSIGNED','GEOLOGIST_REVIEW', 'ACCEPTED PFS', 'FMFS SUBMITTED', 'GR SUBMITTED', 'ACCEPTED PFS MPCD')
""")
    Page<MiningLeaseApplication> findAssignedToUserGeologist(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('ASSIGNED','GEOLOGIST_REVIEW', 'ACCEPTED PFS', 'FMFS SUBMITTED', 'GR SUBMITTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findAssignedToUserAndSearchGeologist(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('PENDING', 'MPCD ASSIGNED', 'ASSIGNED', 'APPROVED', 'ACCEPTED PFS', 'MINING_CHIEF_REVIEW', 'PA/FC SUBMITTED')
""")
    Page<MiningLeaseApplication> findAssignedToUserMPCD(
            Long userId,
            Pageable pageable
    );

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('PENDING', 'MPCD ASSIGNED', 'ASSIGNED', 'APPROVED', 'ACCEPTED PFS', 'MINING_CHIEF_REVIEW')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findAssignedToUserAndSearchMPCD(
            Long userId,
            String search,
            Pageable pageable
    );

    @Query(value = """
        SELECT MAX(q.fmfs_id)
        FROM mas_db.t_mining_lease_application q
        WHERE q.fmfs_id LIKE CONCAT(:prefix, '%')
        """, nativeQuery = true)
    String findLastFmfsId(@Param("prefix") String prefix);

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
    WHERE ur.role_id = 30
      AND t.permission_id = 37
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    ORDER BY workload ASC
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findChiefQuarrying();


    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('FMFS SUBMITTED', 'ACCEPTED DIRECTOR', 'BG SUBMITTED')
""")
    Page<MiningLeaseApplication> findAssignedToUserMineEngineer(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ("FMFS SUBMITTED", 'ACCEPTED DIRECTOR', 'BG SUBMITTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findAssignedToUserIdAndSearchMineEngineer(Long userId, String search, Pageable pageable);

    // All renewal applications (admin view)
    @Query("""
    SELECT q FROM MiningLeaseApplication q
    WHERE LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findAllBySearch(String search, Pageable pageable);

    @Query("""
    SELECT a
    FROM MiningLeaseApplication a
    WHERE a.applicantUserId = :userId
    AND a.currentStatus IN ('MINING LEASE APPROVED')
    AND LOWER(a.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findArchivedAssignedToUserAndSearch(Long userId, String search, Pageable pageable);
}
