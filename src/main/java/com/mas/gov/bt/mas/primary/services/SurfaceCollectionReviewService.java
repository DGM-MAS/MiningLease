package com.mas.gov.bt.mas.primary.services;


import com.mas.gov.bt.mas.primary.dto.request.ReassignRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.ResubmitRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.PermitResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.ReviewResponseDTO;

public interface SurfaceCollectionReviewService {

    ReviewResponseDTO assignToME(Long bgId);

    ReviewResponseDTO reassign(Long reviewId, ReassignRequestDTO dto);

    ReviewResponseDTO requestResubmission(Long reviewId, ResubmitRequestDTO dto);

    PermitResponseDTO issuePermit(Long reviewId, Long mdUserId);
}