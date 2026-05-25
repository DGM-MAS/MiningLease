package com.mas.gov.bt.mas.primary.controller.AuctionOfSurfaceCollection;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.BGResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.SurfaceCollectionAttachmentResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.SurfaceCollectionAuctionResponseDTO;
import com.mas.gov.bt.mas.primary.services.SurfaceCollectionAuctionService;
import com.mas.gov.bt.mas.primary.utility.PageRequest1Based;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/surface-collection-auction")
@RequiredArgsConstructor
public class SurfaceCollectionAuctionController {

    private final SurfaceCollectionAuctionService auctionService;

    private final UserContext userContext;

    /**
     * BR-1
     * Create Auction Application
     */
    @PostMapping
    public ResponseEntity<SuccessResponse<SurfaceCollectionAuctionResponseDTO>> createAuction(
            @RequestBody SurfaceCollectionAuctionRequestDTO dto
    ) {
        Long userId = userContext.getCurrentUserId();

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
            @PathVariable Long auctionId,
            @RequestBody String fileECid
    ) {

        SurfaceCollectionAuctionResponseDTO response =
                auctionService.submitForEC(auctionId, fileECid);

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
            @PathVariable Long auctionId,
            @RequestBody SurfaceCollectionAuctionECRequest fileECid
    ) {

        Long userId = userContext.getCurrentUserId();

        SurfaceCollectionAuctionResponseDTO response =
                auctionService.updateEcApproval(auctionId, fileECid, userId);

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

    @GetMapping
    public ResponseEntity<
            SuccessResponse<Page<SurfaceCollectionAuctionListResponseDTO>>> getAllApplications(

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

        Page<SurfaceCollectionAuctionListResponseDTO> response =
                auctionService.getAllApplications(search, pageable);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Applications fetched successfully",
                        response
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
                auctionService.getMyApplications(search, pageable, userId);

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
                auctionService.getMyArchive(search, pageable, userId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Applications fetched successfully",
                        response
                )
        );
    }

    @GetMapping("/{auctionId}/attachments")
    public ResponseEntity<
            SuccessResponse<List<SurfaceCollectionAttachmentResponseDTO>>
            > getAttachments(

            @PathVariable Long auctionId
    ) {

        List<SurfaceCollectionAttachmentResponseDTO> response =
                auctionService.getAttachmentsByAuctionId(auctionId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Attachments fetched successfully",
                        response
                )
        );
    }

    @GetMapping("/{auctionId}/bg")
    public ResponseEntity<
            SuccessResponse<List<BGResponseDTO>>
            > getBgAttachment(@PathVariable Long auctionId ) {

        List<BGResponseDTO> response =
                auctionService.getBGAttachmentsByAuctionId(auctionId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "BG Attachments fetched successfully",
                        response
                )
        );
    }
}