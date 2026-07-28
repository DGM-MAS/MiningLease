package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.MiningLeaseApplication;
import com.mas.gov.bt.mas.primary.entity.MiningLeaseRenewalApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MiningLeaseRenewalApplicationRepository extends JpaRepository<MiningLeaseRenewalApplication, Long> {

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus NOT IN ('MINING RENEWAL APPROVED', 'REJECTED')
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserDirector(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus NOT IN ('MINING RENEWAL APPROVED', 'REJECTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserAndSearchDirector(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus NOT IN (
    'MINING RENEWAL APPROVED', 'REJECTED'
    )
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserMineEngineer(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus NOT IN (
    'MINING RENEWAL APPROVED', 'REJECTED'
    )
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserIdAndSearchMineEngineer(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus NOT IN (
    'MINING RENEWAL APPROVED', 'REJECTED'
    )
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserGeologist(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus NOT IN (
    'MINING RENEWAL APPROVED', 'REJECTED'
    )
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserAndSearchGeologist(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus NOT IN (
   'MINING RENEWAL APPROVED', 'REJECTED'
    )
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserMiningChief(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus NOT IN ('MINING RENEWAL APPROVED', 'REJECTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserAndSearchMiningChief(Long userId, String search, Pageable pageable);

    Optional<MiningLeaseRenewalApplication> findByApplicationNumber(String applicationNo);

    // Applicant's own renewal applications
    Page<MiningLeaseRenewalApplication> findByCreatedBy(Long createdBy, Pageable pageable);

    @Query("""
    SELECT q FROM MiningLeaseRenewalApplication q
    WHERE q.createdBy = :createdBy
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseRenewalApplication> findByCreatedByAndSearch(Long createdBy, String search, Pageable pageable);

    // All renewal applications (admin view)
    @Query("""
    SELECT q FROM MiningLeaseRenewalApplication q
    WHERE LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseRenewalApplication> findAllBySearch(String search, Pageable pageable);

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
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus NOT IN (
    'MINING RENEWAL APPROVED', 'REJECTED'
    )
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserMPCD(
            Long userId,
            Pageable pageable
    );

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus NOT IN ('MINING RENEWAL APPROVED', 'REJECTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserAndSearchMPCD(
            Long userId,
            String search,
            Pageable pageable
    );


    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus IN ('MINING RENEWAL APPROVED', 'REJECTED')
""")
    Page<MiningLeaseRenewalApplication> findArchivedAssignedToUserMPCD(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus IN ('MINING RENEWAL APPROVED', 'REJECTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseRenewalApplication> findArchivedAssignedToUserAndSearchMPCD(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE q.createdBy = :userId
    AND q.currentStatus IN ('MINING RENEWAL APPROVED', 'REJECTED')
""")
    Page<MiningLeaseRenewalApplication> findArchivedAssignedToUserPromoter(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE q.createdBy = :userId
    AND q.currentStatus IN ('MINING RENEWAL APPROVED', 'REJECTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseRenewalApplication> findArchivedAssignedToUserAndSearchPromoter(Long userId, String trim, Pageable pageable);
}
