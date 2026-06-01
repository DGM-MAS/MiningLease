package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.ReassignRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.ResubmitRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SurfaceCollectionAuctionListResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.BidWinnerResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.PermitResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.ReviewResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.SurfaceCollectionAuctionResponseDTO;
import com.mas.gov.bt.mas.primary.entity.*;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.repository.*;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurfaceCollectionReviewServiceImpl
        implements SurfaceCollectionReviewService {

    private final SurfaceCollectionBankGuaranteeRepository bgRepository;
    private final SurfaceCollectionPermitReviewRepository reviewRepository;
    private final SurfaceCollectionPermitAuctionRepository permitRepository;
    private final SurfaceCollectionAuctionRepository auctionRepository;
    private final ApplicationMasterRepository applicationMasterRepository;
    private final TaskManagementRepository taskManagementRepository;
    private final NotificationClient notificationClient;

    private static final String SERVICE_CODE = "SURFACE_COLLECTION_AUCTION";

    private static final int DEFAULT_TAT_DAYS = 2;


    @Override
    public ReviewResponseDTO assignToME(Long bgId) {

        SurfaceCollectionBankGuarantee bg = bgRepository.findById(bgId)
                .orElseThrow(() -> new RuntimeException("BG not found"));

        Long assignedMe = getLeastBusyME();

        SurfaceCollectionPermitReview review =
                SurfaceCollectionPermitReview.builder()
                        .reviewStatus("ASSIGNED")
                        .build();

        reviewRepository.save(review);

        return map(review);
    }

    @Override
    public ReviewResponseDTO reassign(
            Long reviewId,
            ReassignRequestDTO dto
    ) {

        SurfaceCollectionPermitReview review = getReview(reviewId);

        reviewRepository.save(review);

        return map(review);
    }

    @Override
    public ReviewResponseDTO requestResubmission(
            Long reviewId,
            ResubmitRequestDTO dto,
            Long userId) {

        Optional<SurfaceCollectionAuctionApplication> surfaceCollectionAuctionApplication =
                auctionRepository.findById(reviewId);

        SurfaceCollectionAuctionApplication surfaceCollectionAuctionApplication1 = null;

        if (surfaceCollectionAuctionApplication.isEmpty()) {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "No Auction application found with the given ID.");
        } else {
            surfaceCollectionAuctionApplication1 = surfaceCollectionAuctionApplication.get();
        }

        surfaceCollectionAuctionApplication1.setAuctionStatus("BG RESUBMIT");
        surfaceCollectionAuctionApplication1.setMdRemarks(dto.getRemarks());
        surfaceCollectionAuctionApplication1.setMd_reviewed_on(LocalDateTime.now());

        auctionRepository.save(surfaceCollectionAuctionApplication1);

        Optional<SurfaceCollectionBankGuarantee> surfaceCollectionBankGuarantee =
                bgRepository.findByAuctionApplicationId(surfaceCollectionAuctionApplication1.getId());

        if (surfaceCollectionBankGuarantee.isPresent()) {
            SurfaceCollectionBankGuarantee surfaceCollectionBankGuarantee1 = surfaceCollectionBankGuarantee.get();

            surfaceCollectionBankGuarantee1.setBgFileId(null);
            surfaceCollectionBankGuarantee1.setStatus("BG RESUBMIT");
            surfaceCollectionBankGuarantee1.setReviewedBy(userId);
            surfaceCollectionBankGuarantee1.setReviewedOn(LocalDateTime.now());
            surfaceCollectionBankGuarantee1.setRemarks(dto.getRemarks());

            bgRepository.save(surfaceCollectionBankGuarantee1);
        }else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "No Bank Guarantee details found with the given ID.");
        }

        ApplicationMaster applicationMaster = surfaceCollectionAuctionApplication1.getApplicationMaster();

        applicationMaster.setCurrentStatus("BG RESUBMIT");
        applicationMasterRepository.save(applicationMaster);

        Long promoterId = surfaceCollectionAuctionApplication1.getBidWinner().getPromoterId();
        createTask(applicationMaster , surfaceCollectionAuctionApplication1,"PROMOTER", promoterId, surfaceCollectionAuctionApplication1.getAssignedMdUserId() );

        UserWorkloadProjection userPromoterDetails =
                auctionRepository.findUserDetails(promoterId);

        SurfaceCollectionPermitReview review = new SurfaceCollectionPermitReview();

        review.setReviewedOn(LocalDateTime.now());
        review.setRemarks(dto.getRemarks());
        review.setApplicationNo(surfaceCollectionAuctionApplication1.getApplicationNo());
        review.setReviewStatus("BG RESUBMIT");

        try {
            reviewRepository.save(review);
        }catch (BusinessException e){
            throw new BusinessException(ErrorCodes.DATA_INTEGRITY_VIOLATION, "Data VIOLATION. While saving reviews for this application.", e.getCause());
        }
        // Notification and Email
        if (userPromoterDetails.getEmail() != null) {
            notificationClient.sendSurfaceCollectionAuctionMailToPromoterBGResubmit(
                    userPromoterDetails.getEmail(),
                    userPromoterDetails.getUsername(),
                    surfaceCollectionAuctionApplication1.getApplicationNo());
        }else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "No Email address found for promoter.");
        }

        if(userPromoterDetails.getUserId()!= null) {
            String title = "Bank guarantor details has been reviewed.";
            String message = "Bank guarantor details has been reviewed for surface collection auction. Application No. "+ surfaceCollectionAuctionApplication1.getApplicationNo();
            String serviceId = "71";
            notificationClient.sendUserNotification(title, message, userPromoterDetails.getUserId(), serviceId);
        }else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "Promoter Email Address not found.");
        }

        return map(review);
    }

    @Transactional
    private void createTask(ApplicationMaster master, SurfaceCollectionAuctionApplication application, String role, Long userId, Long directorId) {
        LocalDateTime now = LocalDateTime.now();

        TaskManagement task = new TaskManagement();
        task.setApplicationNumber(application.getApplicationNo());
        task.setServiceCode(SERVICE_CODE);
        task.setAssignedToRole(role);
        task.setAssignedByUserId(userId);
        task.setAssignedToUserId(directorId);
        task.setAssignedAt(now);
        task.setTaskStatus(master.getCurrentStatus());

        task.setDeadlineDate(now.plusDays(DEFAULT_TAT_DAYS));
        task.setCreatedBy(userId);

        taskManagementRepository.save(task);
        log.info("Created task for role {}", role);
    }

    @Override
    public SurfaceCollectionAuctionResponseDTO issuePermit(
            Long reviewId,
            Long mdUserId
    ) {

        SurfaceCollectionAuctionApplication entity = getAuction(reviewId);

        entity.setPermitGenerated(true);
        entity.setAuctionStatus("APPROVED");

        auctionRepository.save(entity);

        SurfaceCollectionBankGuarantee surfaceCollectionBankGuarantee = new SurfaceCollectionBankGuarantee();

        Optional<SurfaceCollectionBankGuarantee> surfaceCollectionBankGuarantee1 = bgRepository.findByAuctionApplicationId(reviewId);
        if (surfaceCollectionBankGuarantee1.isPresent()) {
            surfaceCollectionBankGuarantee = surfaceCollectionBankGuarantee1.get();
        }
        surfaceCollectionBankGuarantee.setStatus("APPROVED");

        bgRepository.save(surfaceCollectionBankGuarantee);

        SurfaceCollectionPermit surfaceCollectionPermit = new SurfaceCollectionPermit();
        surfaceCollectionPermit.setIssuedBy(mdUserId);
        surfaceCollectionPermit.setIssuedOn(LocalDateTime.now());
        surfaceCollectionPermit.setAuctionApplication(entity);
        surfaceCollectionPermit.setValidFrom(LocalDate.now());
        surfaceCollectionPermit.setPermitNo(generatePermitNo());
        surfaceCollectionPermit.setValidTo(LocalDate.now().plusDays(DEFAULT_TAT_DAYS));
        surfaceCollectionPermit.setPermitStatus("APPROVED");
        permitRepository.save(surfaceCollectionPermit);

        return mapToResponse(entity);
    }

    private SurfaceCollectionAuctionApplication getAuction(Long id) {
        return auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found"));
    }

    @Override
    public Page<SurfaceCollectionAuctionListResponseDTO> getMyApplicationsMD(String search, Pageable pageable, Long userId) {
        Page<SurfaceCollectionAuctionApplication> page;

        List<String> archivedStatuses = List.of("SUBMITTED", "EC_APPROVED", "AUCTION_COMPLETED", "BG SUBMITTED", "BG RESUBMIT", "BG RESUBMITTED");

        if (search == null || search.isBlank()) {

            page = auctionRepository
                    .findByAssignedMdUserIdAndAuctionStatusIn(userId,archivedStatuses,pageable);

        } else {

            page = auctionRepository
                    .findByAssignedMdUserIdAndApplicationNoContainingIgnoreCaseOrLocationContainingIgnoreCaseAndAuctionStatusIn(
                            userId,
                            search,
                            search,
                            archivedStatuses,
                            pageable
                    );
        }

        return page.map(this::mapListResponse);
    }

    @Override
    public Page<SurfaceCollectionAuctionListResponseDTO> getMyArchiveMD(String search, Pageable pageable, Long userId) {
        Page<SurfaceCollectionAuctionApplication> page;

        List<String> archivedStatuses = List.of("APPROVED");

        if (search == null || search.isBlank()) {

            page = auctionRepository
                    .findByAssignedMdUserIdAndAuctionStatusIn(userId,archivedStatuses,pageable);

        } else {

            page = auctionRepository
                    .findByAssignedMdUserIdAndApplicationNoContainingIgnoreCaseOrLocationContainingIgnoreCaseAndAuctionStatusIn(
                            userId,
                            search,
                            search,
                            archivedStatuses,
                            pageable
                    );
        }

        return page.map(this::mapListResponse);
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

    private SurfaceCollectionPermitReview getReview(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));
    }

    private Long getLeastBusyME() {
        return 1L;
    }

    private String generatePermitNo() {
        return "SCP-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private ReviewResponseDTO map(SurfaceCollectionPermitReview review) {
        return ReviewResponseDTO.builder()
                .auctionId(review.getId())
                .reviewStatus(review.getReviewStatus())
                .remarks(review.getRemarks())
                .build();
    }

    private PermitResponseDTO mapPermit(SurfaceCollectionPermit permit) {
        return PermitResponseDTO.builder()
                .permitId(permit.getId())
                .permitNo(permit.getPermitNo())
                .permitStatus(permit.getPermitStatus())
                .issuedOn(permit.getIssuedOn())
                .build();
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
                .bidWinner(mapToResponseBidWinner(entity.getBidWinner()))
                .createdOn(entity.getCreatedOn())
                .build();
    }

    private BidWinnerResponseDTO mapToResponseBidWinner(SurfaceCollectionBidWinner bidWinner) {
        if  (bidWinner == null) {
            return BidWinnerResponseDTO.builder().build();
        }
        return BidWinnerResponseDTO.builder()
                .id(bidWinner.getId())
                .bidWinnerName(bidWinner.getBidWinnerName())
                .agencyName(bidWinner.getAgencyName())
                .emailAddress(bidWinner.getEmailAddress())
                .contactNumber(bidWinner.getContactNumber())
                .cidNumber(bidWinner.getCidNumber())
                .build();
    }
}