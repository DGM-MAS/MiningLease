package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.ImmediateSuspensionReasonMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImmediateSuspensionReasonRepository extends JpaRepository<ImmediateSuspensionReasonMaster, Long> {

}
