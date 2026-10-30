package com.mas.gov.bt.mas.primary.repository;


import com.mas.gov.bt.mas.primary.entity.ApplicationRevisionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRevisionHistoryRepository extends JpaRepository<ApplicationRevisionHistory, Long> {

    List<ApplicationRevisionHistory> findByApplicationId(Long applicationId);

    List<ApplicationRevisionHistory> findByApplicationNumber(String applicationNumber);

    Optional<ApplicationRevisionHistory> findTopByApplicationIdAndRevisionStageOrderByRevisionNumberDesc(
            Long applicationId, String revisionStage);

    long countByApplicationIdAndRevisionStage(Long applicationId, String revisionStage);

    List<ApplicationRevisionHistory> findByApplicationIdAndStatus(Long applicationId, String status);
}
