package com.mas.gov.bt.mas.primary.controller.AuctionOfSurfaceCollection;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.ResubmitBGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SubmitBGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SurfaceCollectionAuctionListResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.BGInstructionViewResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.BGResponseDTO;
import com.mas.gov.bt.mas.primary.services.SurfaceCollectionBankGuaranteeService;
import com.mas.gov.bt.mas.primary.utility.PageRequest1Based;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/surface-collection-auction/bg/promoter")
@RequiredArgsConstructor
public class SurfaceCollectionBankGuaranteeController {

    private final SurfaceCollectionBankGuaranteeService bgService;

    private final UserContext userContext;

    /**
     * View BG instructions
     */
    @GetMapping("/{promoterId}/instructions")
    public ResponseEntity<SuccessResponse<BGInstructionViewResponseDTO>> viewInstructions(
            @PathVariable Long promoterId
    ) {

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Instructions fetched successfully",
                        bgService.viewInstructions(promoterId)
                )
        );
    }

    /**
     * Submit BG
     */
    @PostMapping("/{promoterId}/submit")
    public ResponseEntity<SuccessResponse<BGResponseDTO>> submitBG(
            @PathVariable Long promoterId,
            @RequestBody SubmitBGRequestDTO dto
    ) {

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "BG submitted successfully",
                        bgService.submitBG(promoterId, dto)
                )
        );
    }

    /**
     * Resubmit BG
     */
    @PutMapping("/{promoterId}/resubmit")
    public ResponseEntity<SuccessResponse<BGResponseDTO>> resubmitBG(
            @PathVariable Long promoterId,
            @RequestBody ResubmitBGRequestDTO dto
    ) {

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "BG resubmitted successfully",
                        bgService.resubmitBG(promoterId, dto)
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
                bgService.getMyApplications(search, pageable, userId);

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
                bgService.getMyArchive(search, pageable, userId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Applications fetched successfully",
                        response
                )
        );
    }

    @GetMapping("/{applicationNo}")
    public ResponseEntity<SuccessResponse<SurfaceCollectionAuctionListResponseDTO>> viewApplicationDetails(
            @PathVariable String applicationNo
    ) {

        Long userId = userContext.getCurrentUserId();

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Application details fetched successfully",
                        bgService.viewApplicationDetails(userId, applicationNo)
                )
        );
    }

    @GetMapping(value = "/qr/{applicationNo}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQr(
            @PathVariable String applicationNo
    ) throws Exception {

        Long userId = userContext.getCurrentUserId();

        byte[] qrCode = bgService.generateApplicationQr(userId, applicationNo);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=" + applicationNo + "_qr.png")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }

    @GetMapping(
            value = "/qr-link/{applicationNo}",
            produces = MediaType.IMAGE_PNG_VALUE
    )
    public ResponseEntity<byte[]> generateQrWithLink(
            @PathVariable String applicationNo
    ) throws Exception {

        Long userId = userContext.getCurrentUserId();

        byte[] qrCode =
                bgService.generateQrWithLink(userId, applicationNo);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }

}