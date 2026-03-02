package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.MiningLeaseApplication;
import com.mas.gov.bt.mas.primary.entity.MiningLeaseRenewalApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MiningLeaseRenewalApplicationRepository extends JpaRepository<MiningLeaseRenewalApplication, Long> {

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('RENEWAL APPLICATION SUBMITTED')
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserDirector(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseRenewalApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND t.taskStatus IN ('RENEWAL APPLICATION SUBMITTED')
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseRenewalApplication> findAssignedToUserAndSearchDirector(Long userId, String search, Pageable pageable);
}
