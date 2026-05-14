package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.request.BGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.BidWinnerRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SurfaceCollectionAuctionListResponseDTO;
import com.mas.gov.bt.mas.primary.dto.request.SurfaceCollectionAuctionRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.SurfaceCollectionAttachmentResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.SurfaceCollectionAuctionResponseDTO;
import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionAttachment;
import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionAuctionApplication;
import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionBidWinner;
import com.mas.gov.bt.mas.primary.repository.SurfaceCollectionAttachmentRepository;
import com.mas.gov.bt.mas.primary.repository.SurfaceCollectionAuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SurfaceCollectionAuctionServiceImpl implements SurfaceCollectionAuctionService {

    private final SurfaceCollectionAuctionRepository auctionRepository;

    private final SurfaceCollectionAttachmentRepository surfaceCollectionAttachmentRepository;

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
                        .auctionStatus("SUBMITTED")
                        .createdBy(userId)
                        .createdOn(LocalDateTime.now())
                        .build();


        List<SurfaceCollectionAttachment> attachments =
                dto.getAttachments()
                        .stream()
                        .map(att -> SurfaceCollectionAttachment.builder()
                                .attachmentType(att.getAttachmentType())
                                .uploadFileId(att.getFileId())
                                .auctionApplication(entity)
                                .build())
                        .toList();

        entity.setAttachments(attachments);

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
        int year = Year.now().getValue();
        String prefix = String.format("SCA-%d-", year);

        // Get max sequence from database for current year
        Integer maxSequence = auctionRepository.findMaxSequenceByPrefix(prefix);
        long nextSequence = (maxSequence == null ? 0 : maxSequence) + 1;

        return String.format("SCA-%d-%06d", year, nextSequence);
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

    @Override
    public Page<SurfaceCollectionAuctionListResponseDTO> getAllApplications(
            String search,
            Pageable pageable) {

        Page<SurfaceCollectionAuctionApplication> page;

        if (search == null || search.isBlank()) {

            page = auctionRepository.findAll(pageable);

        } else {

            page = auctionRepository
                    .findByApplicationNoContainingIgnoreCaseOrLocationContainingIgnoreCase(
                            search,
                            search,
                            pageable
                    );
        }

        return page.map(this::mapListResponse);
    }

    @Override
    public List<SurfaceCollectionAttachmentResponseDTO>
    getAttachmentsByAuctionId(Long auctionId) {

        List<SurfaceCollectionAttachment> attachments =
                surfaceCollectionAttachmentRepository.findByAuctionApplicationId(auctionId);

        return attachments.stream()
                .map(this::mapAttachment)
                .toList();
    }

    @Override
    public Page<SurfaceCollectionAuctionListResponseDTO> getMyApplications(String search, Pageable pageable, Long userId) {
        Page<SurfaceCollectionAuctionApplication> page;

        List<String> archivedStatuses = List.of("SUBMITTED");

        if (search == null || search.isBlank()) {

            page = auctionRepository.findByCreatedByAndAuctionStatusIn(userId,archivedStatuses,pageable);

        } else {

            page = auctionRepository
                    .findByApplicationNoContainingIgnoreCaseOrLocationContainingIgnoreCaseAndAuctionStatusIn(
                            search,
                            search,
                            archivedStatuses,
                            pageable
                    );
        }

        return page.map(this::mapListResponse);
    }

    private SurfaceCollectionAttachmentResponseDTO mapAttachment(
            SurfaceCollectionAttachment attachment
    ) {

        return SurfaceCollectionAttachmentResponseDTO.builder()
                .id(attachment.getId())
                .attachmentType(attachment.getAttachmentType())
                .uploadFileId(attachment.getUploadFileId())
                .build();
    }

    private SurfaceCollectionAuctionListResponseDTO mapListResponse(
            SurfaceCollectionAuctionApplication entity
    ) {

        return SurfaceCollectionAuctionListResponseDTO.builder()
                .id(entity.getId())
                .applicationNo(entity.getApplicationNo())
                .location(entity.getLocation())
                .area(entity.getArea())
                .material(entity.getMaterial())
                .ecStatus(entity.getEcStatus())
                .fcStatus(entity.getFcStatus())
                .auctionStatus(entity.getAuctionStatus())
                .bgRequested(entity.getBgRequested())
                .permitGenerated(entity.getPermitGenerated())
                .createdOn(entity.getCreatedOn())
                .build();
    }
}