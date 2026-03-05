package com.mas.gov.bt.mas.primary.controller;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.ReviewMiningLeaseApplicationGeologist;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.services.MiningLeaseRenewalService;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mining Lease - Geologist", description = "Geologist Review APIs")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/mining-lease-renewal/geologist")
public class GeologistRenewalController {

    private final MiningLeaseRenewalService miningLeaseRenewalService;
    private final UserContext userContext;

    @PostMapping("/review")
    @Operation(summary = "Review new application", description = "Review new mining lease application by MPCD")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> createApplication(
            @Valid @RequestBody ReviewMiningLeaseApplicationGeologist reviewQuarryLeaseApplicationGeologist) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseRenewalService.reviewApplicationGeologist(reviewQuarryLeaseApplicationGeologist, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }

    // ** Dashboard information for geologist application assigned to particular geologist ** //
    @GetMapping("/assigned")
    public ResponseEntity<SuccessResponse<List<MiningLeaseResponse>>> assignedToGeologist(
            @RequestParam(required = false) String search,
            Pageable pageable) {

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(
                miningLeaseRenewalService.getAssignedToGeologist(userId, pageable, search)
        );
    }
}
