package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.ManualMiningAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManualMiningAttachmentRepository
        extends JpaRepository<ManualMiningAttachmentEntity, Long> {

    List<ManualMiningAttachmentEntity> findByManualMiningEntryIdIn(List<Long> entryIds);
}