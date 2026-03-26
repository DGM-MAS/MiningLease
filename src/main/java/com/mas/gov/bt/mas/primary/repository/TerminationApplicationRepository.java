package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.TerminationGroupedProjection;
import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.TerminationApplicationEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Transactional
public interface TerminationApplicationRepository extends JpaRepository<TerminationApplicationEntity, Long> {

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
    WHERE ur.role_id = 35
      AND t.permission_id = 112
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    ORDER BY workload ASC
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findCMSTermination();

    @Query("""
    SELECT q
    FROM TerminationApplicationEntity q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('SUBMITTED')
""")
    Page<TerminationApplicationEntity> findAssignedToUserMI(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM TerminationApplicationEntity q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('GR SUBMITTED', 'NOTE SHEET UPLOADED', 'MLA SUBMITTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<TerminationApplicationEntity> findAssignedToUserAndSearchMI(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM TerminationApplicationEntity q
    WHERE q.promoterUserId = :userId
    AND q.currentStatus IN ('SUBMITTED' , 'RECTIFICATION BY CMS')
""")
    Page<TerminationApplicationEntity> findAssignedToUserPromoter(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM TerminationApplicationEntity q
    WHERE q.promoterUserId = :userId
    AND q.currentStatus IN ('SUBMITTED', 'RECTIFICATION BY CMS')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<TerminationApplicationEntity> findAssignedToUserAndSearchPromoter(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM TerminationApplicationEntity q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus IN ('APPROVED BY RC')
""")
    Page<TerminationApplicationEntity> findArchivedAssignedToUser(Long userId, List<String> archivedStatuses, Pageable pageable);

    @Query("""
    SELECT q
    FROM TerminationApplicationEntity q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus IN ('APPROVED BY RC')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<TerminationApplicationEntity> findArchivedAssignedToUserAndSearch(Long userId, String search, Pageable pageable);

    @Query("SELECT a FROM TerminationApplicationEntity a WHERE a.currentStatus IN :archivedStatuses")
    Page<TerminationApplicationEntity> findByApplicantUserIdAndStatusIn(Long userId, List<String> archivedStatuses, Pageable pageable);

    @Query("""
    SELECT q FROM TerminationApplicationEntity q
    WHERE q.currentStatus IN :archivedStatuses
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<TerminationApplicationEntity> findByApplicantUserIdAndSearch(Long userId, List<String> archivedStatuses, String search, Pageable pageable);

    @Query("""
    SELECT MAX(
        CAST(
            SUBSTRING(t.terminationId, LENGTH(:prefix) + 1)
            AS integer
        )
    )
    FROM TerminationApplicationEntity t
    WHERE t.terminationId LIKE CONCAT(:prefix, '%')
""")
    Integer findMaxSequenceByPrefix(@Param("prefix") String prefix);

    @Query(value = """
    SELECT
    t.termination_id AS terminationId,
    ARRAY_AGG(t.application_number) AS applicationNumbers,
    MAX(t.current_status) AS currentStatus,
    MAX(t.created_at) AS createdAt,
    MAX(t.applicant_name) AS applicantName,
    MAX(t.promoter_user_id) AS promoterUserId
    FROM mas_db.t_termination_application t
    WHERE t.created_by = :userId
    AND (:search IS NULL OR\s
    LOWER(t.application_number) LIKE LOWER(CONCAT('%', :search, '%')) OR
    LOWER(t.termination_id) LIKE LOWER(CONCAT('%', :search, '%'))
    )
    GROUP BY t.termination_id
    ORDER BY MAX(t.created_at) DESC
    """,
            countQuery = """
    SELECT COUNT(DISTINCT t.termination_id)
    FROM mas_db.t_termination_application t
    WHERE
        t.created_by = :userId
        AND (
            :search IS NULL OR
            LOWER(t.application_number) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(t.termination_id) LIKE LOWER(CONCAT('%', :search, '%'))
        )
    """,
            nativeQuery = true)
    Page<TerminationGroupedProjection> findGroupedApplications(
            @Param("userId") Long userId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
    SELECT q FROM TerminationApplicationEntity q
    WHERE LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :trim, '%'))
""")
    Page<TerminationApplicationEntity> findAllBySearch(String trim, Pageable pageable);
}
