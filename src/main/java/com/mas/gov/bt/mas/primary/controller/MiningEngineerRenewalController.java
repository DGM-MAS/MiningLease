package com.mas.gov.bt.mas.primary.controller;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.ReviewMiningLeaseApplicationME;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.services.MiningLeaseRenewalService;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mining-lease-renewal/mining-engineer")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mining Lease Renewal- Mining Engineer", description = "Mining Engineer renewal Review APIs")
@SecurityRequirement(name = "bearerAuth")
public class MiningEngineerRenewalController {

    private final UserContext userContext;
    private final MiningLeaseRenewalService miningLeaseRenewalService;

    @PostMapping("/review")
    @Operation(summary = "Review renewal application", description = "Review  renewal mining lease application by Mining Engineer")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> reviewApplication(
            @Valid @RequestBody ReviewMiningLeaseApplicationME request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseRenewalService.reviewApplicationME(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }
}
