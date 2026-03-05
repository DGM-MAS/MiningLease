package com.mas.gov.bt.mas.primary.controller;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.ReviewMiningLeaseApplicationChief;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.services.MiningLeaseRenewalService;
import com.mas.gov.bt.mas.primary.services.MiningLeaseService;
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

@RestController
@RequestMapping("/api/mining-lease-renewal/mining-chief")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mining Lease renewal - Mining Chief", description = "Mining Chief renewal Review APIs")
@SecurityRequirement(name = "bearerAuth")
public class MiningChiefRenewalController {

    private final MiningLeaseRenewalService miningLeaseRenewalService;
    private final UserContext userContext;

    @PostMapping("/review")
    @Operation(summary = "Review application", description = "Review quarry lease application by Mining Chief")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> reviewApplication(
            @Valid @RequestBody ReviewMiningLeaseApplicationChief request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseRenewalService.reviewApplicationChief(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }

    @GetMapping("/assigned")
    public ResponseEntity<SuccessResponse<List<MiningLeaseResponse>>> assignedToMiningChief(
            @RequestParam(required = false) String search,
            Pageable pageable) {

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(
                miningLeaseRenewalService.getAssignedToMiningChief(userId, pageable, search)
        );
    }

}
