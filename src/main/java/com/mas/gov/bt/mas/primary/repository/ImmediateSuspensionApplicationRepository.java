package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.ImmediateSuspensionApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ImmediateSuspensionApplicationRepository extends JpaRepository<ImmediateSuspensionApplication, Long> {

    @Query("""
    SELECT q
    FROM ImmediateSuspensionApplication q
    WHERE q.promoterUserId = :userId
    AND q.currentStatus IN ('SUBMITTED' , 'RECTIFICATION BY CMS', 'RECTIFICATION NEEDED')
""")
    Page<ImmediateSuspensionApplication> findAssignedToUserPromoter(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM ImmediateSuspensionApplication q
    WHERE q.promoterUserId = :userId
    AND q.currentStatus IN ('SUBMITTED', 'RECTIFICATION BY CMS', 'RECTIFICATION NEEDED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<ImmediateSuspensionApplication> findAssignedToUserAndSearchPromoter(Long userId, String search, Pageable pageable);

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
    FROM ImmediateSuspensionApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('MI ASSIGNED')
""")
    Page<ImmediateSuspensionApplication> findAssignedToUserMI(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM ImmediateSuspensionApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('MI ASSIGNED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<ImmediateSuspensionApplication> findAssignedToUserAndSearchMI(Long userId, String search, Pageable pageable);
}
