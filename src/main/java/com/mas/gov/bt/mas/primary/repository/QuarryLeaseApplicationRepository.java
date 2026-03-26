package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.QuarryLeaseApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for QuarryLeaseApplication entity.
 */
@Repository
public interface QuarryLeaseApplicationRepository extends JpaRepository<QuarryLeaseApplication, Long> {

    Optional<QuarryLeaseApplication> findByApplicationNumber(String applicationNumber);

    boolean existsByApplicationNumber(String applicationNumber);

    List<QuarryLeaseApplication> findByApplicantUserId(Long applicantUserId);

    Page<QuarryLeaseApplication> findByApplicantUserId(Long applicantUserId, Pageable pageable);

    List<QuarryLeaseApplication> findByCurrentStatus(String status);

    Page<QuarryLeaseApplication> findByCurrentStatus(String status, Pageable pageable);

    @Query("SELECT a FROM QuarryLeaseApplication a WHERE a.currentStatus IN :statuses")
    Page<QuarryLeaseApplication> findByStatusIn(@Param("statuses") List<String> statuses, Pageable pageable);

    @Query("SELECT a FROM QuarryLeaseApplication a WHERE a.applicantUserId = :userId AND a.currentStatus IN :statuses")
    Page<QuarryLeaseApplication> findByApplicantUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<String> statuses, Pageable pageable);

    @Query("SELECT COUNT(a) FROM QuarryLeaseApplication a WHERE a.currentStatus = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT a FROM QuarryLeaseApplication a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    List<QuarryLeaseApplication> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM QuarryLeaseApplication a WHERE a.applicantCid = :cid")
    List<QuarryLeaseApplication> findByApplicantCid(@Param("cid") String cid);

    @Query("SELECT a FROM QuarryLeaseApplication a WHERE a.businessLicenseNo = :licenseNo")
    List<QuarryLeaseApplication> findByBusinessLicenseNo(@Param("licenseNo") String licenseNo);

    @Query("SELECT a.currentStatus, COUNT(a) FROM QuarryLeaseApplication a GROUP BY a.currentStatus")
    List<Object[]> countByStatusGrouped();

    @Query(value = "SELECT nextval('mas_db.quarry_lease_seq_' || :year)", nativeQuery = true)
    Long getNextSequenceForYear(@Param("year") int year);

    @Query("SELECT MAX(CAST(SUBSTRING(a.applicationNumber, 9) AS int)) FROM QuarryLeaseApplication a WHERE a.applicationNumber LIKE :prefix%")
    Integer findMaxSequenceByPrefix(@Param("prefix") String prefix);

    @Query("SELECT a FROM QuarryLeaseApplication a WHERE " +
           "(:applicationNumber IS NULL OR a.applicationNumber LIKE %:applicationNumber%) AND " +
           "(:applicantName IS NULL OR LOWER(a.applicantName) LIKE LOWER(CONCAT('%', :applicantName, '%'))) AND " +
           "(:status IS NULL OR a.currentStatus = :status) AND " +
           "(:dzongkhagId IS NULL OR a.dzongkhag = :dzongkhagId)")
    Page<QuarryLeaseApplication> searchApplications(
            @Param("applicationNumber") String applicationNumber,
            @Param("applicantName") String applicantName,
            @Param("status") String status,
            @Param("dzongkhagId") String dzongkhagId,
            Pageable pageable);

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
    UserWorkloadProjection findLeastBusyUser(Long roleId);

    @Query("""
    SELECT MAX(
        CAST(
            SUBSTRING(a.applicationNumber, LENGTH(:prefix) + 1)
            AS int
        )
    )
    FROM QuarryLeaseApplication a
    WHERE a.applicationNumber LIKE CONCAT(:prefix, '%')
""")
    Integer findMaxDraftSequenceByPrefix(@Param("prefix") String prefix);

    @Query("SELECT COUNT(a) FROM QuarryLeaseApplication a WHERE a.applicantCid IN :cids AND a.currentStatus = 'APPROVED' AND a.isActive = true")
    long countActiveLeasesByCids(@Param("cids") List<String> cids);

    @Query("SELECT COUNT(a) FROM QuarryLeaseApplication a WHERE a.licenseNo = :licenseNo AND a.currentStatus = 'APPROVED' AND a.isActive = true")
    long countActiveLeasesByLicenseNo(@Param("licenseNo") String licenseNo);


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
    SELECT a FROM QuarryLeaseApplication a
    JOIN TaskManagement t
      ON a.applicationNumber = t.applicationNumber
    WHERE t.assignedToUserId = :userId
""")
    Page<QuarryLeaseApplication> findApplicationsAssignedToUser(
            Long userId,
            Pageable pageable);


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
    FROM QuarryLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('PENDING', 'ASSIGNED', 'APPROVED', 'ACCEPTED PFS', 'MINING_CHIEF_REVIEW')
""")
    Page<QuarryLeaseApplication> findAssignedToUserMPCD(
            Long userId,
            Pageable pageable
    );

    @Query("""
    SELECT q
    FROM QuarryLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ("PENDING", "ASSIGNED", "APPROVED", "ACCEPTED PFS", 'MINING_CHIEF_REVIEW')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<QuarryLeaseApplication> findAssignedToUserAndSearchMPCD(
            Long userId,
            String search,
            Pageable pageable
    );


    @Query("""
    SELECT q
    FROM QuarryLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('APPROVED')
""")
    Page<QuarryLeaseApplication> findArchivedAssignedToUserMPCD(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM QuarryLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('APPROVED', 'REJECTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<QuarryLeaseApplication> findArchivedAssignedToUserAndSearchMPCD(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM QuarryLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('GEOLOGIST_REVIEW', 'ACCEPTED PFS', 'FMFS SUBMITTED', 'GR SUBMITTED', 'ACCEPTED PFS MPCD')
""")
    Page<QuarryLeaseApplication> findAssignedToUserGeologist(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM QuarryLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('GEOLOGIST_REVIEW', 'ACCEPTED PFS', 'FMFS SUBMITTED', 'GR SUBMITTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<QuarryLeaseApplication> findAssignedToUserAndSearchGeologist(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM QuarryLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('SUBMITTED', 'NOTE SHEET UPLOADED', 'MLA SUBMITTED')
""")
    Page<QuarryLeaseApplication> findAssignedToUserDirector(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM QuarryLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('SUBMITTED', 'NOTE SHEET UPLOADED', 'MLA SUBMITTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<QuarryLeaseApplication> findAssignedToUserAndSearchDirector(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM QuarryLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('FMFS SUBMITTED', 'ACCEPTED DIRECTOR', 'BG SUBMITTED')
""")
    Page<QuarryLeaseApplication> findAssignedToUserMineEngineer(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM QuarryLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ("FMFS SUBMITTED", 'ACCEPTED DIRECTOR', 'BG SUBMITTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<QuarryLeaseApplication> findAssignedToUserIdAndSearchMineEngineer(Long userId, String search, Pageable pageable);

    @Query(value = """
        SELECT MAX(q.fmfs_id)
        FROM mas_db.t_quarry_lease_application q
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
}
