package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.ApplicationMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationMasterRepository extends JpaRepository<ApplicationMaster, Long> {

    Optional<ApplicationMaster> findByApplicationNumber(String applicationNumber);

    Page<ApplicationMaster> findByApplicantUserId(Long applicantUserId, Pageable pageable);

    Page<ApplicationMaster> findByServiceCode(String serviceCode, Pageable pageable);

    Page<ApplicationMaster> findByCurrentStatus(String status, Pageable pageable);

    @Query("SELECT COUNT(am) FROM ApplicationMaster am WHERE am.serviceCode = :serviceCode AND am.currentStatus = :status")
    Long countByServiceCodeAndStatus(@Param("serviceCode") String serviceCode, @Param("status") String status);
}
