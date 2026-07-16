package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.dto.CitizenGroupingProjection;
import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.entity.MiningLeaseApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MiningLeaseApplicationRepository extends JpaRepository<MiningLeaseApplication, Long> {

    Optional<MiningLeaseApplication> findByApplicationNumber(String applicationNumber);

    @Query("SELECT MAX(CAST(SUBSTRING(a.applicationNumber, 9) AS int)) FROM MiningLeaseApplication a WHERE a.applicationNumber LIKE :prefix%")
    Integer findMaxSequenceByPrefix(@Param("prefix") String prefix);

    @Query("""
    SELECT MAX(
        CAST(
            SUBSTRING(a.applicationNumber, LENGTH(:prefix) + 1)
            AS int
        )
    )
    FROM MiningLeaseApplication a
    WHERE a.applicationNumber LIKE CONCAT(:prefix, '%')
""")
    Integer findMaxDraftSequenceByPrefix(@Param("prefix") String prefix);

    @Query("""
    SELECT a FROM MiningLeaseApplication a
    JOIN TaskManagement t
      ON a.applicationNumber = t.applicationNumber
    WHERE t.assignedToUserId = :currentUserId
""")
    Page<MiningLeaseApplication> findApplicationsAssignedToUser(Long currentUserId, Pageable pageable);

    @Query("SELECT a FROM MiningLeaseApplication a WHERE a.applicantUserId = :userId AND a.currentStatus IN :applicationStatus ")
    Page<MiningLeaseApplication> findByApplicantUserIdAndStatusIn(Long userId, List<String> applicationStatus, Pageable pageable);

    @Query("SELECT a FROM MiningLeaseApplication a WHERE a.applicantUserId = :userId ")
    Page<MiningLeaseApplication> findByApplicantUserId(Long userId, Pageable pageable);

    @Query("SELECT a FROM MiningLeaseApplication a WHERE a.currentStatus IN :statuses")
    Page<MiningLeaseApplication> findByStatusIn(@Param("statuses") List<String> statuses, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus IN :archivedStatuses
""")
    Page<MiningLeaseApplication> findArchivedAssignedToUserMPCD(Long userId, List<String> archivedStatuses, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus IN :archivedStatuses
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findArchivedAssignedToUserAndSearchMPCD(Long userId, List<String> archivedStatuses, String search, Pageable pageable);

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
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus NOT IN :archivedStatuses
""")
    Page<MiningLeaseApplication> findAssignedToUserDirector(Long userId, List<String> archivedStatuses, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus NOT IN :archivedStatuses
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findAssignedToUserAndSearchDirector(Long userId, List<String> archivedStatuses, String search, Pageable pageable);

    @Query(value = """
    SELECT 
        u.id AS userId,
        u.email AS email,
        u.username AS userName
    FROM mas_db.users u
    WHERE u.id = :mpcdFocalId
      AND u.account_status = 'ACTIVE'
    GROUP BY u.id, u.email, u.username
    LIMIT 1
    """, nativeQuery = true)
    UserWorkloadProjection findUserDetailsMPCD(Long mpcdFocalId);

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
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus IN (
    'ASSIGNED',
    'GEOLOGIST_REVIEW',
    "ACCEPTED PFS MPCD",
    "ACCEPTED PFS",
    "APPROVED",
    "RESUBMIT APPLICATION",
    'APPROVED BY DIRECTOR',
    'NOTE SHEET UPLOADED',
    'MA SUBMITTED',
    'MLA SUBMITTED',
    'ACCEPTED PFS',
    'APPROVED PA/FC',
    'RESUBMIT PA/FC',
    'RESUBMITTED PFS',
    'RESUBMIT PFS GEOLOGIST',
    'FMFS SUBMITTED',
    'RESUBMIT FMFS',
    'RESUBMITTED FMFS',
    'GR SUBMITTED',
    'RESUBMITTED PFS GEOLOGIST',
    "MA SUBMITTED",
    "PA/FC SUBMITTED",
    "APPROVED GR",
    "NOTE SHEET UPLOADED",
    "GR SUBMITTED",
    "BG SUBMITTED",
    "FMFS SUBMITTED",
    "MLA SUBMITTED",
    "APPROVED BY DIRECTOR",
    "RESUBMITTED PFS",
    "RESUBMIT GR",
    "RESUBMITTED GR",
    "RESUBMIT FMFS",
    "MPCD ASSIGNED",
    "RESUBMITTED FMFS",
    "RESUBMIT APPLICATION",
    "RESUBMIT PFS GEOLOGIST",
    "RESUBMIT PFS MPCD",
    "RESUBMIT PA/FC",
    "APPROVED PA/FC"
    )
""")
    Page<MiningLeaseApplication> findAssignedToUserGeologist(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus IN (
    'ASSIGNED',
    'GEOLOGIST_REVIEW',
    "ACCEPTED PFS MPCD",
    "ACCEPTED PFS",
    "APPROVED",
    "RESUBMIT APPLICATION",
    'APPROVED BY DIRECTOR',
    'NOTE SHEET UPLOADED',
    'MA SUBMITTED',
    'MLA SUBMITTED',
    'ACCEPTED PFS',
    'APPROVED PA/FC',
    'RESUBMIT PA/FC',
    'RESUBMITTED PFS',
    'RESUBMIT PFS GEOLOGIST',
    'FMFS SUBMITTED',
    'RESUBMIT FMFS',
    'RESUBMITTED FMFS',
    'GR SUBMITTED',
    'RESUBMITTED PFS GEOLOGIST',
    "MA SUBMITTED",
    "PA/FC SUBMITTED",
    "APPROVED GR",
    "NOTE SHEET UPLOADED",
    "GR SUBMITTED",
    "BG SUBMITTED",
    "FMFS SUBMITTED",
    "MLA SUBMITTED",
    "APPROVED BY DIRECTOR",
    "RESUBMITTED PFS",
    "RESUBMIT GR",
    "RESUBMITTED GR",
    "RESUBMIT FMFS",
    "MPCD ASSIGNED",
    "RESUBMITTED FMFS",
    "RESUBMIT APPLICATION",
    "RESUBMIT PFS GEOLOGIST",
    "RESUBMIT PFS MPCD",
    "RESUBMIT PA/FC",
    "APPROVED PA/FC"
    )
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findAssignedToUserAndSearchGeologist(Long userId, String search, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus IN (
    'ASSIGNED',
    'MPCD ASSIGNED',
    'GEOLOGIST_REVIEW',
    'RESUBMIT APPLICATION',
    'PENDING',
    'MA SUBMITTED',
    'APPROVED',
    'ACCEPTED PFS',
    'APPROVED PA/FC',
    'MINING_CHIEF_REVIEW',
    'PA/FC SUBMITTED',
    'RESUBMITTED PFS MPCD',
    "MA SUBMITTED",
    "PA/FC SUBMITTED",
    "APPROVED GR",
    "NOTE SHEET UPLOADED",
    "GR SUBMITTED",
    "BG SUBMITTED",
    "FMFS SUBMITTED",
    "MLA SUBMITTED",
    "APPROVED BY DIRECTOR",
    "RESUBMITTED PFS",
    "RESUBMIT GR",
    "RESUBMITTED GR",
    "RESUBMIT FMFS",
    "MPCD ASSIGNED",
    "RESUBMITTED FMFS",
    "RESUBMIT APPLICATION",
    "RESUBMIT PFS GEOLOGIST",
    "RESUBMIT PFS MPCD",
    "RESUBMIT PA/FC",
    "APPROVED PA/FC",
    "FORWARDED TO DIRECTOR"
    )
""")
    Page<MiningLeaseApplication> findAssignedToUserMPCD(
            Long userId,
            Pageable pageable
    );

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus IN (
    'PENDING',
    'MPCD ASSIGNED',
    'GEOLOGIST_REVIEW',
    'MA SUBMITTED',
    'ASSIGNED',
    'APPROVED',
    'ACCEPTED PFS',
    'APPROVED PA/FC',
    'MINING_CHIEF_REVIEW',
    'PA/FC SUBMITTED',
    'RESUBMIT APPLICATION',
    'RESUBMITTED PFS MPCD',
    'FORWARDED TO DIRECTOR'
    )
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findAssignedToUserAndSearchMPCD(
            Long userId,
            String search,
            Pageable pageable
    );

    @Query(value = """
        SELECT MAX(q.fmfs_id)
        FROM mas_db.t_mining_lease_application q
        WHERE q.fmfs_id LIKE CONCAT(:prefix, '%')
        """, nativeQuery = true)
    String findLastFmfsId(@Param("prefix") String prefix);



    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus IN (
    'FMFS SUBMITTED',
    'ACCEPTED DIRECTOR',
    'BG SUBMITTED',
    'FORWARDED TO DIRECTOR',
    'DIRECTOR APPROVED FMFS',
    'LLC UPLOADED',
    'NOTE SHEET UPLOADED',
    'RESUBMITTED FMFS',
    'MINING_CHIEF_REVIEW',
    'MLA SUBMITTED'
    )
""")
    Page<MiningLeaseApplication> findAssignedToUserMineEngineer(Long userId, Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.assignedToUserId = :userId
    AND q.currentStatus IN (
    'FMFS SUBMITTED',
    'MINING_CHIEF_REVIEW',
    'ACCEPTED DIRECTOR',
    'BG SUBMITTED',
    'FORWARDED TO DIRECTOR',
    'DIRECTOR APPROVED FMFS',
    'LLC UPLOADED',
    'NOTE SHEET UPLOADED',
    'RESUBMITTED FMFS',
    'MLA SUBMITTED"'
    )
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findAssignedToUserIdAndSearchMineEngineer(Long userId, String search, Pageable pageable);

    // Team-wide queue — every pending application regardless of who it auto-assigned to
    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.taskStatus IN (
    'FMFS SUBMITTED',
    'ACCEPTED DIRECTOR',
    'BG SUBMITTED',
    'FORWARDED TO DIRECTOR',
    'DIRECTOR APPROVED FMFS',
    'LLC UPLOADED',
    'NOTE SHEET UPLOADED',
    'RESUBMITTED FMFS',
    'MINING_CHIEF_REVIEW'
    )
""")
    Page<MiningLeaseApplication> findTeamQueueMineEngineer(Pageable pageable);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    JOIN TaskManagement t
        ON t.applicationNumber = q.applicationNumber
    WHERE t.taskStatus IN (
    'FMFS SUBMITTED',
    'ACCEPTED DIRECTOR',
    'BG SUBMITTED',
    'FORWARDED TO DIRECTOR',
    'DIRECTOR APPROVED FMFS',
    'LLC UPLOADED',
    'NOTE SHEET UPLOADED',
    'RESUBMITTED FMFS',
    'MINING_CHIEF_REVIEW'
    )
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findTeamQueueWithSearchMineEngineer(String search, Pageable pageable);

    // All renewal applications (admin view)
    @Query("""
    SELECT q FROM MiningLeaseApplication q
    WHERE LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findAllBySearch(String search, Pageable pageable);

    @Query("""
    SELECT a
    FROM MiningLeaseApplication a
    JOIN TaskManagement t
        ON t.applicationNumber = a.applicationNumber
    WHERE a.currentStatus IN :archivedStatuses
    AND t.assignedToUserId = :userId
    AND LOWER(a.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findArchivedAssignedToUserAndSearch(Long userId, List<String> archivedStatuses, String search, Pageable pageable);

    @Query("""
    SELECT q FROM MiningLeaseApplication q
    WHERE q.createdBy = :userId
    AND q.currentStatus NOT IN :applicationStatus
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findByAssignedToUserAndSearch(Long userId, List<String> applicationStatus, String search, Pageable pageable);

    @Query("""
    SELECT q FROM MiningLeaseApplication q
    WHERE q.createdBy = :userId
    AND q.currentStatus IN :archivedStatuses
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findByApplicantUserIdAndSearch(Long userId, List<String> archivedStatuses, String search, Pageable pageable);

    @Query(value = """
    SELECT 
    u.household_number
    FROM mas_db.t_citizens u
    WHERE u.id = :userId
    AND u.account_status = 'ACTIVE'
    LIMIT 1
    """, nativeQuery = true)
    String findUserHouseHoldNumber(Long userId);

    @Query(value = """
    SELECT
    SUM(mining_lease_count) AS total_mining_lease_count
    FROM mas_db.t_citizens
    WHERE household_number = :houseHoldNumber
    """, nativeQuery = true)
    Integer findLeaseCountForMining(String houseHoldNumber);

    List<MiningLeaseApplication> findByIsManualEntry(String isManualEntry);

    List<MiningLeaseApplication> findByIsManualEntryAndManualEntryBy(String isManualEntry, Long manualEntryBy);

    /** Resolves the fields needed to pick the application-cap grouping key for a user. */
    @Query(value = """
    SELECT registration_type AS registrationType,
           household_number AS householdNumber,
           license_no AS licenseNo,
           company_registration_number AS companyRegistrationNumber,
           cid AS cid
    FROM mas_db.t_citizens
    WHERE id = :userId
    """, nativeQuery = true)
    Optional<CitizenGroupingProjection> findGroupingInfoByUserId(@Param("userId") Long userId);

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
    FROM mas_db.t_mining_lease_application mla
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

    @Query(value = """
SELECT
(
    SELECT COUNT(*)
    FROM mas_db.t_household_permit_threshold h
    JOIN t_citizens c
      ON c.cid = h.applicant_cid
    WHERE c.household_number = :householdNumber
      AND h.service_type = 'MINING_LEASE'
      AND h.status = 'ACTIVE'
)
+
(
    SELECT COUNT(*)
    FROM  mas_db.t_mining_lease_application mla
    JOIN t_citizens c
      ON c.cid = mla.applicant_cid
    WHERE c.household_number = :householdNumber
    AND mla.current_status NOT IN (
     "DRAFT"
      )
)
""", nativeQuery = true)
    Integer countMiningLeasesByHousehold(String householdNumber);

    @Query(value = """
    SELECT COUNT(*)
    FROM mas_db.t_household_permit_threshold h
    WHERE h.applicant_cid = :applicantCid
      AND h.service_type = :serviceType
      AND h.status = 'ACTIVE'
    """,
            nativeQuery = true)
    Integer countByApplicantCidAndServiceType(@Param("applicantCid") String applicantCid,
                                              @Param("serviceType") String serviceType);

    @Query("""
    SELECT q
    FROM MiningLeaseApplication q
    WHERE q.currentStatus IN :archivedStatuses
    AND LOWER(q.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
""")
    Page<MiningLeaseApplication> findByStatusInAndSearch(List<String> archivedStatuses, String search, Pageable pageable);
}
