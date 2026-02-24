package com.mas.gov.bt.mas.primary.controller;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.MiningLeaseApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.request.MiningLeaseGRRequest;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.services.MiningLeaseService;
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

/**
 * REST Controller for Mining Lease Application management.
 */
@RestController
@RequestMapping("/api/mining-lease")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mining Lease", description = "Mining Lease Application Management APIs")
//@SecurityRequirement(name = "bearerAuth")
public class MiningLeaseController {


    private final UserContext userContext;
    private final MiningLeaseService miningLeaseService;

    // ========== Applicant APIs ==========

    // Used by user to submit Geological report
    @PostMapping("/applicationsGR")
    @Operation(summary = "Submit GR file application", description = "Submit PA/FC file for quarry lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitGRFile(
            @Valid @RequestBody MiningLeaseGRRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseService.submitGR(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    @PostMapping("/applications")
    @Operation(summary = "Create new application", description = "Create a new quarry lease application in draft state")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> createApplication(
            @Valid @RequestBody MiningLeaseApplicationRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseService.createApplication(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }



}
