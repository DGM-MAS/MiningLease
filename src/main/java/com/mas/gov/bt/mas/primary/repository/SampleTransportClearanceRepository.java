package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.SampleTransportClearanceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SampleTransportClearanceRepository extends JpaRepository<SampleTransportClearanceEntity, Long> {
    @Query("""
            SELECT MAX(
                CAST(SUBSTRING(a.applicationNo, 14) AS integer)
            )
            FROM SampleTransportClearanceEntity a
            WHERE a.applicationNo LIKE CONCAT(:prefix, '%')
            """)
    Integer findMaxSequenceByPrefix(String prefix);


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
    UserWorkloadProjection findChiefGSDSample();

    Page<SampleTransportClearanceEntity> findByCreatedByAndStatusIn(Long userId, List<String> applicationStatus, Pageable pageable);

    @Query("""
    SELECT q FROM SampleTransportClearanceEntity q
    WHERE q.createdBy = :userId
    AND LOWER(q.applicationNo) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<SampleTransportClearanceEntity> findByAssignedToUserAndSearch(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT a
    FROM SampleTransportClearanceEntity a
    WHERE a.status IN ('APPROVED', 'REJECTED')
    AND a.createdBy = :userId
    AND LOWER(a.applicationNo) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<SampleTransportClearanceEntity> findByCreatedByAndSearch(Long userId, List<String> archivedStatuses, String search, Pageable pageable);

    @Query("""
    SELECT q FROM SampleTransportClearanceEntity q
    WHERE LOWER(q.applicationNo) LIKE LOWER(CONCAT('%', :trim, '%'))
""")
    Page<SampleTransportClearanceEntity> findAllBySearch(String trim, Pageable pageable);

    Page<SampleTransportClearanceEntity> findByAssignedGSDChiefIdAndStatusIn(Long userId, Pageable pageable, List<String> applicationStatus);

    Page<SampleTransportClearanceEntity> findByAssignedGSDChiefIdAndApplicationNoContainingIgnoreCaseAndStatusIn(Long userId, String trim, Pageable pageable, List<String> applicationStatus);

    @Query(value = """
    SELECT 
        u.id AS userId,
        u.email AS email,
        u.username AS userName
    FROM mas_db.users u
    WHERE u.id = :userId
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findUserDetails(Long createdBy);

   Optional<SampleTransportClearanceEntity> findByApplicationNo(String applicationNo);

    Page<SampleTransportClearanceEntity> findByAssignedGSDFocalIdAndStatusIn(Long userId, Pageable pageable, List<String> applicationStatus);

    Page<SampleTransportClearanceEntity> findByAssignedGSDFocalIdAndApplicationNoContainingIgnoreCaseAndStatusIn(Long userId, String trim, Pageable pageable, List<String> applicationStatus);
}