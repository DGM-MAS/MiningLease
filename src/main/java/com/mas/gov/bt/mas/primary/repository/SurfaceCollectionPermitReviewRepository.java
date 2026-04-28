package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionPermitReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurfaceCollectionPermitReviewRepository
        extends JpaRepository<SurfaceCollectionPermitReview, Long> {

    List<SurfaceCollectionPermitReview> findByAssignedMeId(Long meId);
}