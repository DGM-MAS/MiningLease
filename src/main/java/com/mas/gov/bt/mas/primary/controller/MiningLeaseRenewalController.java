package com.mas.gov.bt.mas.primary.controller;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.MiningLeaseApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.request.MiningLeaseFMFSRequest;
import com.mas.gov.bt.mas.primary.dto.request.RenewalMiningLeaseRequest;
import com.mas.gov.bt.mas.primary.dto.response.ApplicationListResponse;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.services.MiningLeaseRenewalService;
import com.mas.gov.bt.mas.primary.utility.PageRequest1Based;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mining-lease/renewal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mining Lease Renewal", description = "Renewal Review APIs")
@SecurityRequirement(name = "bearerAuth")
public class MiningLeaseRenewalController {

    private final UserContext userContext;
    private final MiningLeaseRenewalService miningLeaseRenewalService;

    // Get all approved mining lease application for particular applicant id
    // Pagination added start from 1
    @GetMapping("/application")
    @Operation(summary = "Get application by number", description = "Get application details by application number. Agency users can view any application, others can only view their own.")
    public ResponseEntity<SuccessResponse<List<ApplicationListResponse>>> getApplicationByNumber(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Page<ApplicationListResponse> applications;

        Long userId = userContext.getCurrentUserId();

        applications = miningLeaseRenewalService.getApplicationByApplicantId(userId , pageable);

        return ResponseEntity.ok(SuccessResponse.fromPage("Applications retrieved successfully", applications));
    }

    @PostMapping("/applications")
    @Operation(summary = "Create renewal application", description = "Create a renewal mining lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> createApplication(
            @Valid @RequestBody RenewalMiningLeaseRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseRenewalService.createRenewalApplication(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }


    // Used by user to submit FMFS
    @PostMapping("/applicationsFMFS")
    @Operation(summary = "Submit FMFS file application", description = "Submit FMFS file for mining lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitFMFSFile(
            @Valid @RequestBody MiningLeaseFMFSRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseRenewalService.submitFMFS(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }




}
