package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.ManualMiningEntryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManualMiningEntryRepository extends JpaRepository<ManualMiningEntryEntity, Long> {

    @Query("""
        SELECT MAX(CAST(SUBSTRING(a.applicationNo, :startIndex) AS int))
        FROM ManualMiningEntryEntity a
        WHERE a.applicationNo LIKE CONCAT(:prefix, '%')
    """)
    Integer findMaxSequenceByPrefixAndStartIndex(@Param("prefix") String prefix,
                                                 @Param("startIndex") int startIndex);

    @Query(value = """
        SELECT
            u.id        AS userId,
            u.email     AS email,
            u.username  AS userName
        FROM mas_db.users u
        WHERE u.id = :userId
          AND u.account_status = 'ACTIVE'
        LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findUserDetails(@Param("userId") Long userId);

    Optional<ManualMiningEntryEntity> findByApplicationNo(String applicationNo);

    Page<ManualMiningEntryEntity> findByCreatedByAndActivityTypeAndStatusIn(
            Long createdBy, String activityType, List<String> statuses, Pageable pageable);

    Page<ManualMiningEntryEntity> findByActivityTypeAndStatusIn(
            String activityType, List<String> statuses, Pageable pageable);

    Page<ManualMiningEntryEntity> findByCreatedByAndStatusIn(
            Long createdBy, List<String> statuses, Pageable pageable);

    Page<ManualMiningEntryEntity> findByStatusIn(List<String> statuses, Pageable pageable);

    @Query("""
        SELECT e FROM ManualMiningEntryEntity e
        WHERE e.createdBy = :createdBy
          AND (:search IS NULL OR e.applicationNo LIKE CONCAT('%', :search, '%')
               OR e.applicantName LIKE CONCAT('%', :search, '%'))
    """)
    Page<ManualMiningEntryEntity> findByCreatedByAndSearch(@Param("createdBy") Long createdBy,
                                                           @Param("search") String search,
                                                           Pageable pageable);

    @Query("""
        SELECT e FROM ManualMiningEntryEntity e
        WHERE (:search IS NULL OR e.applicationNo LIKE CONCAT('%', :search, '%')
               OR e.applicantName LIKE CONCAT('%', :search, '%'))
    """)
    Page<ManualMiningEntryEntity> findAllBySearch(@Param("search") String search, Pageable pageable);
}
