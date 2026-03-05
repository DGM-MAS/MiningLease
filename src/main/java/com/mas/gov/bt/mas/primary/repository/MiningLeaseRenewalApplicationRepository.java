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
    AND t.taskStatus IN ('RENEWAL APPLICATION')
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserDirector(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('RENEWAL APPLICATION')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserAndSearchDirector(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('ASSIGNED')
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserMineEngineer(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('ASSIGNED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserIdAndSearchMineEngineer(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('ASSIGNED','GEOLOGIST_REVIEW', 'ACCEPTED PFS', 'FMFS SUBMITTED', 'GR SUBMITTED', 'ACCEPTED PFS MPCD')
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserGeologist(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('ASSIGNED','GEOLOGIST_REVIEW', 'ACCEPTED PFS', 'FMFS SUBMITTED', 'GR SUBMITTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserAndSearchGeologist(Long userId, String search, Pageable pageable);

    Optional<MiningLeaseRenewalApplication> findByApplicationNumber(String applicationNo);

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
}
