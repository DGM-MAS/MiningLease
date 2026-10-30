package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.ExplorationPermit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ExplorationPermitRepository extends JpaRepository<ExplorationPermit, Long> {

    Optional<ExplorationPermit> findByApplicationId(Long applicationId);

    @Query("SELECT MAX(p.permitNumber) FROM ExplorationPermit p WHERE p.permitNumber LIKE 'EP2-%'")
    String findMaxPermitNumber();

    ExplorationPermit findByPermitNumber(String expPermitNo);
}
