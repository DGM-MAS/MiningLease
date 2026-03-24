package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.TerminationApplicationEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    WHERE ur.role_id = 21
      AND t.permission_id = 108
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
    Page<TerminationApplicationEntity> findAssignedToUserAndSearchMI(Long userId, String trim, Pageable pageable);
}
