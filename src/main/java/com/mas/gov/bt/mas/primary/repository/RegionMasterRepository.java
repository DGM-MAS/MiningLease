package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.RegionMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionMasterRepository extends JpaRepository<RegionMaster, Long> {

}
