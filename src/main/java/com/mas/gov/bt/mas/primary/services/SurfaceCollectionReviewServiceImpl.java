package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.request.ReassignRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.ResubmitRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.PermitResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.ReviewResponseDTO;
import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionBankGuarantee;
import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionPermit;
import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionPermitReview;
import com.mas.gov.bt.mas.primary.repository.SurfaceCollectionBankGuaranteeRepository;
import com.mas.gov.bt.mas.primary.repository.SurfaceCollectionPermitAuctionRepository;
import com.mas.gov.bt.mas.primary.repository.SurfaceCollectionPermitReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SurfaceCollectionReviewServiceImpl
        implements SurfaceCollectionReviewService {

    private final SurfaceCollectionBankGuaranteeRepository bgRepository;
    private final SurfaceCollectionPermitReviewRepository reviewRepository;
    private final SurfaceCollectionPermitAuctionRepository permitRepository;

    @Override
    public ReviewResponseDTO assignToME(Long bgId) {

        SurfaceCollectionBankGuarantee bg = bgRepository.findById(bgId)
                .orElseThrow(() -> new RuntimeException("BG not found"));

        Long assignedMe = getLeastBusyME();

        SurfaceCollectionPermitReview review =
                SurfaceCollectionPermitReview.builder()
                        .bankGuarantee(bg)
                        .assignedMeId(assignedMe)
                        .assignedOn(LocalDateTime.now())
                        .reviewStatus("ASSIGNED")
                        .build();

        reviewRepository.save(review);

        /**
         * Notify ME
         */

        return map(review);
    }

    @Override
    public ReviewResponseDTO reassign(
            Long reviewId,
            ReassignRequestDTO dto
    ) {

        SurfaceCollectionPermitReview review = getReview(reviewId);

        review.setReassignedTo(dto.getNewMeId());
        review.setAssignedMeId(dto.getNewMeId());
        review.setReassignedOn(LocalDateTime.now());

        reviewRepository.save(review);

        return map(review);
    }

    @Override
    public ReviewResponseDTO requestResubmission(
            Long reviewId,
            ResubmitRequestDTO dto
    ) {

        SurfaceCollectionPermitReview review = getReview(reviewId);

        review.setReviewStatus("RESUBMISSION_REQUESTED");
        review.setRemarks(dto.getRemarks());

        SurfaceCollectionBankGuarantee bg = review.getBankGuarantee();
        bg.setStatus("REJECTED");
        bg.setRemarks(dto.getRemarks());

        bgRepository.save(bg);
        reviewRepository.save(review);

        /**
         * Notify promoter
         */

        return map(review);
    }

    @Override
    public PermitResponseDTO issuePermit(
            Long reviewId,
            Long mdUserId
    ) {

        SurfaceCollectionPermitReview review = getReview(reviewId);

        review.setReviewStatus("APPROVED");
        review.setReviewedOn(LocalDateTime.now());

        SurfaceCollectionPermit permit =
                SurfaceCollectionPermit.builder()
                        .permitNo(generatePermitNo())
                        .auctionApplication(
                                review.getBankGuarantee().getAuctionApplication()
                        )
                        .issuedBy(mdUserId)
                        .issuedOn(LocalDateTime.now())
                        .permitStatus("ACTIVE")
                        .build();

        permitRepository.save(permit);
        reviewRepository.save(review);

        /**
         * Notify promoter
         */

        return mapPermit(permit);
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
                .reviewId(review.getId())
                .bgId(review.getBankGuarantee().getId())
                .assignedMeId(review.getAssignedMeId())
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
}