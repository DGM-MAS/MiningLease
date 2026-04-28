package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.ManualMiningEntryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ManualMiningEntryRepository extends JpaRepository<ManualMiningEntryEntity, Long> {
    @Query("""
    SELECT MAX(CAST(SUBSTRING(a.applicationNo, 14) AS int))
    FROM ManualMiningEntryEntity a
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
    UserWorkloadProjection findChiefManualEntry();

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
    UserWorkloadProjection findUserDetails(Long userId);

    Page<ManualMiningEntryEntity> findByAssignedChiefIdAndStatusIn(Long userId, Pageable pageable, List<String> applicationStatus);

    Page<ManualMiningEntryEntity> findByAssignedChiefIdAndApplicationNoContainingIgnoreCaseAndStatusIn(Long userId, String trim, Pageable pageable, List<String> applicationStatus);

    Optional<ManualMiningEntryEntity> findByApplicationNo(String applicationNo);

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
    UserWorkloadProjection findDirectorManualEntry();

    Page<ManualMiningEntryEntity> findByAssignedDirectorIdAndStatusIn(Long userId, Pageable pageable, List<String> applicationStatus);

    Page<ManualMiningEntryEntity> findByAssignedDirectorIdAndApplicationNoContainingIgnoreCaseAndStatusIn(Long userId, String trim, Pageable pageable, List<String> applicationStatus);

}
