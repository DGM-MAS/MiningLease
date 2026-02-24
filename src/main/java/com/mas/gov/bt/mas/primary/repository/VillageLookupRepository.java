package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.VillageLookup;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VillageLookupRepository extends JpaRepository<VillageLookup, Integer> {

    Optional<Object> findByVillageSerialNo(@Size(max = 100, message = "Village must not exceed 100 characters") Integer nearestVillage);
}
