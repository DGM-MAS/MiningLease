package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.request.BGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.BidWinnerRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SurfaceCollectionAuctionRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.SurfaceCollectionAuctionResponseDTO;
import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionAuctionApplication;
import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionBidWinner;
import com.mas.gov.bt.mas.primary.repository.SurfaceCollectionAuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SurfaceCollectionAuctionServiceImpl implements SurfaceCollectionAuctionService {

    private final SurfaceCollectionAuctionRepository auctionRepository;

    @Override
    public SurfaceCollectionAuctionResponseDTO createAuction(
            SurfaceCollectionAuctionRequestDTO dto,
            Long userId
    ) {

        SurfaceCollectionAuctionApplication entity =
                SurfaceCollectionAuctionApplication.builder()
                        .applicationNo(generateApplicationNo())
                        .location(dto.getLocation())
                        .area(dto.getArea())
                        .material(dto.getMaterial())
                        .auctionStatus("DRAFT")
                        .createdBy(userId)
                        .createdOn(LocalDateTime.now())
                        .build();

        auctionRepository.save(entity);

        return mapToResponse(entity);
    }

    @Override
    public SurfaceCollectionAuctionResponseDTO submitForEC(Long auctionId) {

        SurfaceCollectionAuctionApplication entity = getAuction(auctionId);

        entity.setSubmittedForEc(true);
        entity.setEcStatus("PENDING");
        entity.setAuctionStatus("EC_PENDING");

        auctionRepository.save(entity);

        return mapToResponse(entity);
    }

    @Override
    public SurfaceCollectionAuctionResponseDTO submitForFC(Long auctionId) {

        SurfaceCollectionAuctionApplication entity = getAuction(auctionId);

        entity.setSubmittedForFc(true);
        entity.setFcStatus("PENDING");

        auctionRepository.save(entity);

        return mapToResponse(entity);
    }

    @Override
    public SurfaceCollectionAuctionResponseDTO updateEcApproval(Long auctionId) {

        SurfaceCollectionAuctionApplication entity = getAuction(auctionId);

        entity.setEcStatus("APPROVED");
        entity.setEcApprovedOn(LocalDateTime.now());

        auctionRepository.save(entity);

        return mapToResponse(entity);
    }

    @Override
    public SurfaceCollectionAuctionResponseDTO updateFcApproval(Long auctionId) {

        SurfaceCollectionAuctionApplication entity = getAuction(auctionId);

        entity.setFcStatus("APPROVED");
        entity.setFcApprovedOn(LocalDateTime.now());

        auctionRepository.save(entity);

        return mapToResponse(entity);
    }


    @Override
    public SurfaceCollectionAuctionResponseDTO saveBidWinner(
            Long auctionId,
            BidWinnerRequestDTO dto
    ) {

        SurfaceCollectionAuctionApplication entity = getAuction(auctionId);

        SurfaceCollectionBidWinner winner =
                SurfaceCollectionBidWinner.builder()
                        .bidWinnerName(dto.getBidWinnerName())
                        .contactNumber(dto.getContactNumber())
                        .emailAddress(dto.getEmailAddress())
                        .otherDetails(dto.getOtherDetails())
                        .auctionApplication(entity)
                        .build();

        entity.setBidWinner(winner);
        entity.setAuctionCompleted(true);
        entity.setAuctionStatus("AUCTION_COMPLETED");

        auctionRepository.save(entity);

        return mapToResponse(entity);
    }

    @Override
    public SurfaceCollectionAuctionResponseDTO requestBG(
            Long auctionId,
            BGRequestDTO dto
    ) {

        SurfaceCollectionAuctionApplication entity = getAuction(auctionId);

        entity.setBgRequested(true);
        entity.setBgInstruction(dto.getBgInstruction());
        entity.setAuctionStatus("BG_PENDING");

        auctionRepository.save(entity);

        /**
         * Send notification/email here
         */

        return mapToResponse(entity);
    }

    @Override
    public SurfaceCollectionAuctionResponseDTO generatePermit(Long auctionId) {

        SurfaceCollectionAuctionApplication entity = getAuction(auctionId);

        entity.setPermitGenerated(true);
        entity.setAuctionStatus("PERMIT_GENERATED");

        auctionRepository.save(entity);

        return mapToResponse(entity);
    }

    private SurfaceCollectionAuctionApplication getAuction(Long id) {
        return auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found"));
    }

    private String generateApplicationNo() {
        return "SCA-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private SurfaceCollectionAuctionResponseDTO mapToResponse(
            SurfaceCollectionAuctionApplication entity
    ) {
        return SurfaceCollectionAuctionResponseDTO.builder()
                .id(entity.getId())
                .applicationNo(entity.getApplicationNo())
                .location(entity.getLocation())
                .area(entity.getArea())
                .material(entity.getMaterial())
                .ecStatus(entity.getEcStatus())
                .fcStatus(entity.getFcStatus())
                .auctionStatus(entity.getAuctionStatus())
                .submittedForEc(entity.getSubmittedForEc())
                .submittedForFc(entity.getSubmittedForFc())
                .bgRequested(entity.getBgRequested())
                .permitGenerated(entity.getPermitGenerated())
                .createdOn(entity.getCreatedOn())
                .build();
    }
}