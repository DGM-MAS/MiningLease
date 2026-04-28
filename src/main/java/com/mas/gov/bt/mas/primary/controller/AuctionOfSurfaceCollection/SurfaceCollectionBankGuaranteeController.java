package com.mas.gov.bt.mas.primary.controller.AuctionOfSurfaceCollection;

import com.mas.gov.bt.mas.primary.dto.request.ResubmitBGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SubmitBGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.BGInstructionViewResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.BGResponseDTO;
import com.mas.gov.bt.mas.primary.services.SurfaceCollectionBankGuaranteeService;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promoter/surface-collection/bg")
@RequiredArgsConstructor
public class SurfaceCollectionBankGuaranteeController {

    private final SurfaceCollectionBankGuaranteeService bgService;

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
}