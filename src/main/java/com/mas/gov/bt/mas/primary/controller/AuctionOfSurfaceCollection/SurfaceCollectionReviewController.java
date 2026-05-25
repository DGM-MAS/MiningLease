package com.mas.gov.bt.mas.primary.controller.AuctionOfSurfaceCollection;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.ReassignRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.ResubmitRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SurfaceCollectionAuctionListResponseDTO;
import com.mas.gov.bt.mas.primary.services.SurfaceCollectionReviewService;
import com.mas.gov.bt.mas.primary.utility.PageRequest1Based;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/surface-collection-auction/mining-engineer-auction")
@RequiredArgsConstructor
public class SurfaceCollectionReviewController {

    private final SurfaceCollectionReviewService reviewService;

    private final UserContext userContext;

    /**
     * Auto assign
     */
    @PostMapping("/assign/{bgId}")
    public ResponseEntity<?> assignToME(@PathVariable Long bgId) {
        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Assigned successfully",
                        reviewService.assignToME(bgId)
                )
        );
    }

    /**
     * Reassign
     */
    @PutMapping("/review/{reviewId}/reassign")
    public ResponseEntity<?> reassign(
            @PathVariable Long reviewId,
            @RequestBody ReassignRequestDTO dto
    ) {



        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Reassigned successfully",
                        reviewService.reassign(reviewId, dto)
                )
        );
    }

    /**
     * Request resubmission
     */
    @PutMapping("/review/{reviewId}/resubmit")
    public ResponseEntity<?> requestResubmission(
            @PathVariable Long reviewId,
            @RequestBody ResubmitRequestDTO dto
    ) {

        Long userId = userContext.getCurrentUserId();

        return ResponseEntity.ok(
                new SuccessResponse<>(

                        "Resubmission requested",
                        reviewService.requestResubmission(reviewId, dto, userId)
                )
        );
    }

    /**
     * Issue permit
     */
    @PostMapping("/review/{auctionId}/issue-permit")
    public ResponseEntity<?> issuePermit(
            @PathVariable Long auctionId,
            @RequestParam Long mdUserId
    ) {
        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Permit issued successfully",
                        reviewService.issuePermit(auctionId, mdUserId)
                )
        );
    }

    @GetMapping("/my-applications")
    public ResponseEntity<
            SuccessResponse<Page<SurfaceCollectionAuctionListResponseDTO>>> getMyApplications(

            @RequestParam(required = false) String search,

            @RequestParam(defaultValue = "1") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(defaultValue = "createdOn") String sortBy,

            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {

        Pageable pageable = PageRequest1Based.of(
                page,
                size,
                Sort.Direction.fromString(sortDirection),
                sortBy
        );

        Long userId = userContext.getCurrentUserId();

        Page<SurfaceCollectionAuctionListResponseDTO> response =
                reviewService.getMyApplicationsMD(search, pageable, userId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Applications fetched successfully",
                        response
                )
        );
    }

    @GetMapping("/my-archive")
    public ResponseEntity<
            SuccessResponse<Page<SurfaceCollectionAuctionListResponseDTO>>> getMyArchive(

            @RequestParam(required = false) String search,

            @RequestParam(defaultValue = "1") int page,

            @RequestParam(defaultValue = "10") int size,

            @RequestParam(defaultValue = "createdOn") String sortBy,

            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {

        Pageable pageable = PageRequest1Based.of(
                page,
                size,
                Sort.Direction.fromString(sortDirection),
                sortBy
        );

        Long userId = userContext.getCurrentUserId();

        Page<SurfaceCollectionAuctionListResponseDTO> response =
                reviewService.getMyArchiveMD(search, pageable, userId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Applications fetched successfully",
                        response
                )
        );
    }
}