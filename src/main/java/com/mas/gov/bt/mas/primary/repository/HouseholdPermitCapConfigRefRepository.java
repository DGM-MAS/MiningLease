package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.HouseholdPermitCapConfigRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HouseholdPermitCapConfigRefRepository extends JpaRepository<HouseholdPermitCapConfigRef, Long> {

    Optional<HouseholdPermitCapConfigRef> findByServiceType(String serviceType);

    Optional<HouseholdPermitCapConfigRef> findByServiceTypeAndRegistrationType(String serviceType, String registrationType);
}
