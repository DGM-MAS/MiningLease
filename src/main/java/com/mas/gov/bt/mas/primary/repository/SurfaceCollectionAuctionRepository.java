package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionAuctionApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SurfaceCollectionAuctionRepository
        extends JpaRepository<SurfaceCollectionAuctionApplication, Long> {

    Optional<SurfaceCollectionAuctionApplication> findByApplicationNo(String applicationNo);

    Page<SurfaceCollectionAuctionApplication> findByApplicationNoContainingIgnoreCaseOrLocationContainingIgnoreCase(String search, String search1, Pageable pageable);

    Page<SurfaceCollectionAuctionApplication> findByCreatedByAndAuctionStatusIn(Long userId, List<String> archivedStatuses, Pageable pageable);

    Page<SurfaceCollectionAuctionApplication> findByApplicationNoContainingIgnoreCaseOrLocationContainingIgnoreCaseAndAuctionStatusIn(String search, String search1, List<String> archivedStatuses, Pageable pageable);

    @Query(value = """
    SELECT 
        u.id AS userId,
        u.email AS email,
        u.username AS userName
    FROM mas_db.users u
    WHERE u.id = :directorId
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findUserDetails(Long directorId);

    @Query("""
    SELECT MAX(CAST(SUBSTRING(a.applicationNo, 10) AS integer))
    FROM SurfaceCollectionAuctionApplication a
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
    WHERE ur.role_id = 20
      AND t.permission_id = 71 
      AND u.account_status = 'ACTIVE'
      AND u.region_id = :id
    GROUP BY u.id, u.email, u.username
    ORDER BY workload ASC
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findMDSurfaceCollection(Long id);

    @Query(value = """
    SELECT 
        u.id AS userId,
        u.email AS email,
        u.username AS userName
    FROM mas_db.users u
    WHERE u.email = :emailAddress
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findUserDetailsByEmail(String emailAddress);

    Page<SurfaceCollectionAuctionApplication> findByCreatedByAndApplicationNoContainingIgnoreCaseOrLocationContainingIgnoreCaseAndAuctionStatusIn(Long userId, String search, String search1, List<String> archivedStatuses, Pageable pageable);

    Page<SurfaceCollectionAuctionApplication> findByAssignedMdUserIdAndAuctionStatusIn(Long userId, List<String> archivedStatuses, Pageable pageable);

    Page<SurfaceCollectionAuctionApplication> findByAssignedMdUserIdAndApplicationNoContainingIgnoreCaseOrLocationContainingIgnoreCaseAndAuctionStatusIn(Long userId, String search, String search1, List<String> archivedStatuses, Pageable pageable);

    Page<SurfaceCollectionAuctionApplication>
    findByBidWinnerPromoterId(
            Long promoterId,
            Pageable pageable
    );

    Page<SurfaceCollectionAuctionApplication> findByBidWinnerPromoterIdAndAuctionStatusIn(Long userId, List<String> archivedStatuses, Pageable pageable);

    Optional<SurfaceCollectionAuctionApplication> findByApplicationNoAndBidWinnerPromoterId(String applicationNo, Long userId);

    Optional<SurfaceCollectionAuctionApplication> findByApplicationNoAndCreatedBy(String applicationNo, Long createdBy);

    Optional<SurfaceCollectionAuctionApplication> findByApplicationNoAndAuctionStatusAndBidWinnerPromoterId(String applicationNo, String auctionStatus, Long bidWinnerPromoterId);

    /**
     * Counts in-progress (excluding DRAFT) + ACTIVE mining lease applications for
     * everyone sharing the given grouping key. groupingType is one of
     * INDIVIDUAL | BUSINESS_LICENSE | REGISTERED_COMPANY | CID (CID = fallback
     * when the applicant has no household/license/company number on file).
     */
    @Query(value = """
SELECT
(
    SELECT COUNT(*)
    FROM mas_db.household_permit_threshold h
    JOIN mas_db.t_citizens c
      ON c.cid = h.applicant_cid
    WHERE h.service_type = 'MINING_LEASE'
      AND h.status = 'ACTIVE'
      AND (
        (:groupingType = 'INDIVIDUAL' AND c.household_number = :groupingKey) OR
        (:groupingType = 'BUSINESS_LICENSE' AND c.license_no = :groupingKey) OR
        (:groupingType = 'REGISTERED_COMPANY' AND c.company_registration_number = :groupingKey) OR
        (:groupingType = 'CID' AND c.cid = :groupingKey)
      )
)
+
(
    SELECT COUNT(*)
    FROM mas_db.t_surface_collection_auction mla
    JOIN mas_db.t_citizens c
      ON c.cid = mla.applicant_cid
    WHERE mla.current_status IN (
     'SUBMITTED',
     'ASSIGNED',
     'MPCD ASSIGNED',
     'GEOLOGIST_REVIEW',
     'GR SUBMITTED',
     'LLC UPLOADED',
     'PAYMENT PENDING',
     'ACCEPTED PFS',
     'ADDITIONAL DATA NEEDED',
     'MA SUBMITTED',
     'PA/FC SUBMITTED',
     'APPROVED GR',
     'NOTE SHEET UPLOADED',
     'BG SUBMITTED',
     'FMFS SUBMITTED',
     'MLA SUBMITTED',
     'APPROVED BY DIRECTOR',
     'RESUBMITTED PFS',
     'RESUBMIT GR',
     'RESUBMITTED GR',
     'RESUBMIT FMFS',
     'RESUBMITTED FMFS',
     'RESUBMIT APPLICATION',
     'RESUBMIT PFS GEOLOGIST',
     'RESUBMIT PFS MPCD',
     'RESUBMIT PA/FC',
     'APPROVED PA/FC'
      )
      AND (
        (:groupingType = 'INDIVIDUAL' AND c.household_number = :groupingKey) OR
        (:groupingType = 'BUSINESS_LICENSE' AND c.license_no = :groupingKey) OR
        (:groupingType = 'REGISTERED_COMPANY' AND c.company_registration_number = :groupingKey) OR
        (:groupingType = 'CID' AND c.cid = :groupingKey)
      )
)
""", nativeQuery = true)
    Integer countMiningLeasesForGrouping(@Param("groupingType") String groupingType, @Param("groupingKey") String groupingKey);
}