package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.BGResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.SurfaceCollectionAttachmentResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.SurfaceCollectionAuctionResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SurfaceCollectionAuctionService {

    SurfaceCollectionAuctionResponseDTO createAuction(
            SurfaceCollectionAuctionRequestDTO dto,
            Long userId
    );

    SurfaceCollectionAuctionResponseDTO submitForEC(Long auctionId, String fileECid);

    SurfaceCollectionAuctionResponseDTO submitForFC(Long auctionId);

    SurfaceCollectionAuctionResponseDTO updateEcApproval(Long auctionId, SurfaceCollectionAuctionECRequest fileECid, Long userId);

    SurfaceCollectionAuctionResponseDTO updateFcApproval(Long auctionId);

    SurfaceCollectionAuctionResponseDTO saveBidWinner(
            Long auctionId,
            BidWinnerRequestDTO dto
    );

    SurfaceCollectionAuctionResponseDTO requestBG(
            Long auctionId,
            BGRequestDTO dto
    );

    SurfaceCollectionAuctionResponseDTO generatePermit(Long auctionId);

    Page<SurfaceCollectionAuctionListResponseDTO> getAllApplications(
            String search,
            Pageable pageable);

    List<SurfaceCollectionAttachmentResponseDTO> getAttachmentsByAuctionId(Long auctionId);

    Page<SurfaceCollectionAuctionListResponseDTO> getMyApplications(String search, Pageable pageable, Long userId);

    List<BGResponseDTO> getBGAttachmentsByAuctionId(Long auctionId);

    Page<SurfaceCollectionAuctionListResponseDTO> getMyArchive(String search, Pageable pageable, Long userId);
}