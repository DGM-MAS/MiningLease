package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.MineRestorationCompletionReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MineRestorationCompletionReportRepository extends JpaRepository<MineRestorationCompletionReport, Long> {

    Optional<MineRestorationCompletionReport> findByRestorationApplicationNumber(String applicationNumber);
}
