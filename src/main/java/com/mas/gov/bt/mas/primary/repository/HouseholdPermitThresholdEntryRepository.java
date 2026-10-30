package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.HouseholdPermitThresholdEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HouseholdPermitThresholdEntryRepository extends JpaRepository<HouseholdPermitThresholdEntry, Long> {

    boolean existsByServiceTypeAndApplicationNo(String serviceType, String applicationNo);
}
