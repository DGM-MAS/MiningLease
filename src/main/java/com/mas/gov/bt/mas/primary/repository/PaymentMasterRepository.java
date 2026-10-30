package com.mas.gov.bt.mas.primary.repository;


import com.mas.gov.bt.mas.primary.entity.PaymentMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentMasterRepository extends JpaRepository<PaymentMaster, Long> {

    Optional<PaymentMaster> findByServiceCodeAndFeeType(String serviceCode, String feeType);

    Optional<PaymentMaster> findByServiceCodeAndFeeTypeAndTriggerStatus(String serviceCode, String feeType, String triggerStatus);

    Optional<PaymentMaster> findByServiceCodeAndFeeTypeAndTriggerStatusIsNull(String serviceCode, String feeType);

    /**
     * The row that applies for this service/fee at the given application status: a
     * status-specific row wins if one exists, otherwise falls back to the wildcard
     * (triggerStatus IS NULL) row that applies at any status.
     */
    default Optional<PaymentMaster> resolveApplicable(String serviceCode, String feeType, String triggerStatus) {
        return findByServiceCodeAndFeeTypeAndTriggerStatus(serviceCode, feeType, triggerStatus)
                .or(() -> findByServiceCodeAndFeeTypeAndTriggerStatusIsNull(serviceCode, feeType));
    }
}
