package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.SiteMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteMasterRepository extends JpaRepository<SiteMaster, Long> {

    boolean existsByLeaseTypeAndLeaseApplicationId(String leaseType, Long leaseApplicationId);

    Optional<SiteMaster> findByLeaseTypeAndLeaseApplicationNumber(String leaseType, String leaseApplicationNumber);
}
