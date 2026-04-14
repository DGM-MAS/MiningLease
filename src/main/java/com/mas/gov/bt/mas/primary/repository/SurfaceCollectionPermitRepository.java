package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionPermitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SurfaceCollectionPermitRepository extends JpaRepository<SurfaceCollectionPermitEntity, Long> {

    Optional<SurfaceCollectionPermitEntity> findByApplicationNo(String applicationNumber);
}
