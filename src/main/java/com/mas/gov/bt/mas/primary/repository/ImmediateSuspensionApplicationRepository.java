package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.ImmediateSuspensionApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImmediateSuspensionApplicationRepository extends JpaRepository<ImmediateSuspensionApplication, Long> {

    @Query("""
    SELECT q
    FROM ImmediateSuspensionApplication q
    WHERE q.promoterUserId = :userId
    AND q.currentStatus IN ('SUBMITTED' , 'RECTIFICATION BY PROMOTER', 'RECTIFICATION NEEDED', 'SUSPENSION LIFTING')
""")
    Page<ImmediateSuspensionApplication> findAssignedToUserPromoter(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM ImmediateSuspensionApplication q
    WHERE q.promoterUserId = :userId
    AND q.currentStatus IN ('SUBMITTED', 'RECTIFICATION BY CMS', 'RECTIFICATION NEEDED', 'SUSPENSION LIFTING')
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
    AND t.taskStatus IN ('MI ASSIGNED', 'SUSPENSION LIFTING')
""")
    Page<ImmediateSuspensionApplication> findAssignedToUserMI(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM ImmediateSuspensionApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('MI ASSIGNED', 'SUSPENSION LIFTING')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<ImmediateSuspensionApplication> findAssignedToUserAndSearchMI(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM ImmediateSuspensionApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.createdBy = :currentUserId
    AND t.taskStatus IN ('SUBMITTED', 'RECTIFICATION BY CMS', 'SUSPENSION LIFTING')
""")
    Page<ImmediateSuspensionApplication> findAssignedToUserRCME(Long currentUserId, Pageable pageable);

    @Query("""
    SELECT q
    FROM ImmediateSuspensionApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.createdBy = :currentUserId
    AND t.taskStatus IN ('SUBMITTED', 'RECTIFICATION BY CMS', 'SUSPENSION LIFTING')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<ImmediateSuspensionApplication> findAssignedToUserAndSearchRCME(Long currentUserId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM ImmediateSuspensionApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE q.currentStatus IN :archivedStatuses
    AND t.assignedToUserId = :userId
""")
    Page<ImmediateSuspensionApplication> findArchivedAssignedToUserMI(Long userId, List<String> archivedStatuses, Pageable pageable);

    @Query("""
    SELECT q
    FROM ImmediateSuspensionApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN :archivedStatuses
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<ImmediateSuspensionApplication> findArchivedAssignedToUserAndSearchMI(Long userId, String search, List<String> archivedStatuses, Pageable pageable);

    @Query("""
    SELECT q
    FROM ImmediateSuspensionApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE q.currentStatus IN :archivedStatuses
    AND q.createdBy = :userId
""")
    Page<ImmediateSuspensionApplication> findArchivedAssignedToUser(Long userId, List<String> archivedStatuses, Pageable pageable);

    @Query("""
    SELECT q
    FROM ImmediateSuspensionApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE q.currentStatus IN :archivedStatuses
    AND q.createdBy = :userId
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<ImmediateSuspensionApplication> findArchivedAssignedToUserAndSearch(Long userId, String search, List<String> archivedStatuses, Pageable pageable);

    @Query("SELECT a FROM ImmediateSuspensionApplication a WHERE a.promoterUserId = :userId AND a.currentStatus IN :archivedStatuses")
    Page<ImmediateSuspensionApplication> findByApplicantUserIdAndStatusIn(Long userId, List<String> archivedStatuses, Pageable pageable);

    @Query("""
    SELECT q FROM ImmediateSuspensionApplication q
    WHERE q.currentStatus IN :archivedStatuses
    AND q.promoterUserId = :userId
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<ImmediateSuspensionApplication> findByApplicantUserIdAndSearch(Long userId, List<String> archivedStatuses, String search, Pageable pageable);

    @Query("""
    SELECT q FROM ImmediateSuspensionApplication q
    WHERE LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :trim, '%'))
""")
    Page<ImmediateSuspensionApplication> findAllBySearch(String trim, Pageable pageable);

    Optional<ImmediateSuspensionApplication> findByApplicationNumber(String applicationNo);
}
