package com.mas.gov.bt.mas.primary.controller.AuctionOfSurfaceCollection;

import com.mas.gov.bt.mas.primary.dto.request.BGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.BidWinnerRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SurfaceCollectionAuctionRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.SurfaceCollectionAuctionResponseDTO;
import com.mas.gov.bt.mas.primary.services.SurfaceCollectionAuctionService;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/surface-collection-auction")
@RequiredArgsConstructor
public class SurfaceCollectionAuctionController {

    private final SurfaceCollectionAuctionService auctionService;

    /**
     * BR-1
     * Create Auction Application
     */
    @PostMapping
    public ResponseEntity<SuccessResponse<SurfaceCollectionAuctionResponseDTO>> createAuction(
            @RequestBody SurfaceCollectionAuctionRequestDTO dto,
            @RequestParam Long userId
    ) {

        SurfaceCollectionAuctionResponseDTO response =
                auctionService.createAuction(dto, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>(
                        "Auction application created successfully",
                        response
                ));
    }

    /**
     * FR-3
     * Submit for EC
     */
    @PutMapping("/{auctionId}/submit-ec")
    public ResponseEntity<SuccessResponse<SurfaceCollectionAuctionResponseDTO>> submitForEC(
            @PathVariable Long auctionId
    ) {

        SurfaceCollectionAuctionResponseDTO response =
                auctionService.submitForEC(auctionId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Submitted for EC successfully",
                        response
                )
        );
    }

    /**
     * FR-2
     * Submit for FC
     */
    @PutMapping("/{auctionId}/submit-fc")
    public ResponseEntity<SuccessResponse<SurfaceCollectionAuctionResponseDTO>> submitForFC(
            @PathVariable Long auctionId
    ) {

        SurfaceCollectionAuctionResponseDTO response =
                auctionService.submitForFC(auctionId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Submitted for FC successfully",
                        response
                )
        );
    }

    /**
     * FR-4
     * Update EC Approval
     */
    @PutMapping("/{auctionId}/approve-ec")
    public ResponseEntity<SuccessResponse<SurfaceCollectionAuctionResponseDTO>> approveEC(
            @PathVariable Long auctionId
    ) {

        SurfaceCollectionAuctionResponseDTO response =
                auctionService.updateEcApproval(auctionId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "EC approved successfully",
                        response
                )
        );
    }

    /**
     * FR-4
     * Update FC Approval
     */
    @PutMapping("/{auctionId}/approve-fc")
    public ResponseEntity<SuccessResponse<SurfaceCollectionAuctionResponseDTO>> approveFC(
            @PathVariable Long auctionId
    ) {

        SurfaceCollectionAuctionResponseDTO response =
                auctionService.updateFcApproval(auctionId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "FC approved successfully",
                        response
                )
        );
    }

    /**
     * BR-5 / FR-5
     * Save Auction Winner
     */
    @PostMapping("/{auctionId}/bid-winner")
    public ResponseEntity<SuccessResponse<SurfaceCollectionAuctionResponseDTO>> saveBidWinner(
            @PathVariable Long auctionId,
            @RequestBody BidWinnerRequestDTO dto
    ) {

        SurfaceCollectionAuctionResponseDTO response =
                auctionService.saveBidWinner(auctionId, dto);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Bid winner details saved successfully",
                        response
                )
        );
    }

    /**
     * BR-6
     * Request Bank Guarantee
     */
    @PutMapping("/{auctionId}/request-bg")
    public ResponseEntity<SuccessResponse<SurfaceCollectionAuctionResponseDTO>> requestBG(
            @PathVariable Long auctionId,
            @RequestBody BGRequestDTO dto
    ) {

        SurfaceCollectionAuctionResponseDTO response =
                auctionService.requestBG(auctionId, dto);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "BG request sent successfully",
                        response
                )
        );
    }

    /**
     * Generate Surface Collection Permit
     */
    @PutMapping("/{auctionId}/generate-permit")
    public ResponseEntity<SuccessResponse<SurfaceCollectionAuctionResponseDTO>> generatePermit(
            @PathVariable Long auctionId
    ) {

        SurfaceCollectionAuctionResponseDTO response =
                auctionService.generatePermit(auctionId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Permit generated successfully",
                        response
                )
        );
    }
}