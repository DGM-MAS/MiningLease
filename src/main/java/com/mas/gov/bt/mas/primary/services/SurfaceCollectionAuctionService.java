package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.request.BGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.BidWinnerRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SurfaceCollectionAuctionRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.SurfaceCollectionAuctionResponseDTO;

public interface SurfaceCollectionAuctionService {

    SurfaceCollectionAuctionResponseDTO createAuction(
            SurfaceCollectionAuctionRequestDTO dto,
            Long userId
    );

    SurfaceCollectionAuctionResponseDTO submitForEC(Long auctionId);

    SurfaceCollectionAuctionResponseDTO submitForFC(Long auctionId);

    SurfaceCollectionAuctionResponseDTO updateEcApproval(Long auctionId);

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
}