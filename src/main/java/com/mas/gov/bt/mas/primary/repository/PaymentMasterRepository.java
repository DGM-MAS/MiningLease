package com.mas.gov.bt.mas.primary.repository;


import com.mas.gov.bt.mas.primary.entity.PaymentMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentMasterRepository extends JpaRepository<PaymentMaster, Long> {

    PaymentMaster findByServiceName(String serviceCode);
}
