package com.mas.gov.bt.mas.primary.controller.AuctionOfSurfaceCollection;

import com.mas.gov.bt.mas.primary.dto.request.ReassignRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.ResubmitRequestDTO;
import com.mas.gov.bt.mas.primary.services.SurfaceCollectionReviewService;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mining-engineer/surface-collection")
@RequiredArgsConstructor
public class SurfaceCollectionReviewController {

    private final SurfaceCollectionReviewService reviewService;

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
        return ResponseEntity.ok(
                new SuccessResponse<>(

                        "Resubmission requested",
                        reviewService.requestResubmission(reviewId, dto)
                )
        );
    }

    /**
     * Issue permit
     */
    @PostMapping("/review/{reviewId}/issue-permit")
    public ResponseEntity<?> issuePermit(
            @PathVariable Long reviewId,
            @RequestParam Long mdUserId
    ) {
        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Permit issued successfully",
                        reviewService.issuePermit(reviewId, mdUserId)
                )
        );
    }
}