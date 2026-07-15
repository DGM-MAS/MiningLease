package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.HouseholdPermitThresholdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HouseholdPermitThresholdRepository
        extends JpaRepository<HouseholdPermitThresholdEntity, Long> {

    long countByApplicantCidAndServiceTypeAndStatusIn(
            String applicantCid, String serviceType, List<String> statuses);

    Optional<HouseholdPermitThresholdEntity> findByApplicationNoAndServiceType(
            String applicationNo, String serviceType);
}
