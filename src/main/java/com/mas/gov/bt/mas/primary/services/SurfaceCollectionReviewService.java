package com.mas.gov.bt.mas.primary.services;


import com.mas.gov.bt.mas.primary.dto.IssuePermitRequest;
import com.mas.gov.bt.mas.primary.dto.request.ReassignRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.ResubmitRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SurfaceCollectionAuctionListResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.ReviewResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.SurfaceCollectionAuctionResponseDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SurfaceCollectionReviewService {

    ReviewResponseDTO assignToME(Long bgId);

    ReviewResponseDTO reassign(Long reviewId, ReassignRequestDTO dto);

    ReviewResponseDTO requestResubmission(Long reviewId, ResubmitRequestDTO dto, Long userId);

    SurfaceCollectionAuctionResponseDTO issuePermit(Long reviewId, Long mdUserId, @Valid IssuePermitRequest issuePermitFileId);

    Page<SurfaceCollectionAuctionListResponseDTO> getMyApplicationsMD(String search, Pageable pageable, Long userId);

    Page<SurfaceCollectionAuctionListResponseDTO> getMyArchiveMD(String search, Pageable pageable, Long userId);
}