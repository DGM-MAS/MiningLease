package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.GewogLookup;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GewogLookupRepository extends JpaRepository<GewogLookup, Integer> {
    Optional<Object> findByGewogId(@Size(max = 100, message = "Gewog must not exceed 100 characters") String gewog);
}
