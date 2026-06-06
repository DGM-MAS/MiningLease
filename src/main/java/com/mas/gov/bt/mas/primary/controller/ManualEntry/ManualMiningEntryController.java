package com.mas.gov.bt.mas.primary.controller.ManualEntry;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.QuarryLeaseApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.QuarryLeaseResponse;
import com.mas.gov.bt.mas.primary.dto.request.ManualMiningEntryRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.MiningLeaseApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.response.ApplicationListResponse;
import com.mas.gov.bt.mas.primary.dto.response.ManualEntryFieldConfigResponse;
import com.mas.gov.bt.mas.primary.dto.response.ManualMiningEntryResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.services.ManualEntryFieldConfigService;
import com.mas.gov.bt.mas.primary.services.ManualMiningEntryService;
import com.mas.gov.bt.mas.primary.services.ManualMiningLeaseService;
import com.mas.gov.bt.mas.primary.utility.PageRequest1Based;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manual-mining")
@RequiredArgsConstructor
@Tag(name = "Manual Mining Entry", description = "Focal-side manual entry for Mining, Quarry, Surface Collection and Stock Lifting")
@SecurityRequirement(name = "bearerAuth")
public class ManualMiningEntryController {

    private final ManualMiningEntryService service;
    private final ManualMiningLeaseService manualMiningLeaseService;
    private final ManualEntryFieldConfigService fieldConfigService;
    private final UserContext userContext;

    @PostMapping("/applications")
    @Operation(
            summary = "Create manual entry",
            description = "Focal officer records a completed application. activityType must be one of: MINING_LEASE, QUARRY_LEASE, SURFACE_COLLECTION, STOCK_LIFTING. Application is marked as manual entry and goes directly to approved status — no review flow."
    )
    public ResponseEntity<SuccessResponse<ManualMiningEntryResponseDTO>> createApplication(
            @Valid @RequestBody ManualMiningEntryRequestDTO request) {

        Long userId = userContext.getCurrentUserId();
        ManualMiningEntryResponseDTO response = service.createApplication(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    @GetMapping("/applications")
    @Operation(
            summary = "List manual entries",
            description = "Agency users see all manual entries across all focal officers. Regular users see only their own submissions."
    )
    public ResponseEntity<SuccessResponse<List<ManualMiningEntryResponseDTO>>> getApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        if (userContext.isAgencyUser()) {
            return ResponseEntity.ok(service.getAllApplications(pageable, search));
        }

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(service.getApplications(userId, pageable, search));
    }

    @GetMapping("/applications/{applicationNo}")
    @Operation(summary = "Get manual entry by application number")
    public ResponseEntity<SuccessResponse<ManualMiningEntryResponseDTO>> getApplication(
            @PathVariable String applicationNo) {

        ManualMiningEntryResponseDTO response = service.getApplicationByNo(applicationNo);
        return ResponseEntity.ok(new SuccessResponse<>("Application fetched successfully", response));
    }

    @PostMapping("/manual-mining-applications")
    @Operation(summary = "Create mining lease manual entry", description = "Create a mining lease application as a manual entry — saves directly to approved status")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> createMiningLeaseApplication(
            @Valid @RequestBody MiningLeaseApplicationRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = manualMiningLeaseService.createApplication(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    @PostMapping("/quarry-applications")
    @Operation(summary = "Create quarry lease manual entry", description = "Create a quarry lease application as a manual entry — saves directly to approved status")
    public ResponseEntity<SuccessResponse<QuarryLeaseResponse>> createQuarryLeaseApplication(
            @Valid @RequestBody QuarryLeaseApplicationRequest request) {

        Long userId = userContext.getCurrentUserId();
        QuarryLeaseResponse response = manualMiningLeaseService.createApplicationQuarry(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    @GetMapping("/archived-applications")
    @Operation(summary = "Get archived applications", description = "Agency users see all archived applications; regular users see only their own.")
    public ResponseEntity<SuccessResponse<List<ApplicationListResponse>>> getArchivedApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Long userId = userContext.getCurrentUserId();
        Page<ApplicationListResponse> applications;

        if (userContext.isAgencyUser()) {
            applications = manualMiningLeaseService.getArchivedApplications(pageable, search, userId);
        } else {
            applications = manualMiningLeaseService.getMyArchivedApplications(userId, pageable, search);
        }

        return ResponseEntity.ok(SuccessResponse.fromPage("Archived applications retrieved successfully", applications));
    }

    @GetMapping("/field-config")
    @Operation(
            summary = "Get field configuration for an activity type",
            description = "Returns the list of sections and fields (with required/optional flags) for the given activityType. " +
                    "Use this to drive the manual entry form on the frontend. " +
                    "activityType must be one of: MINING_LEASE, QUARRY_LEASE, SURFACE_COLLECTION, STOCK_LIFTING"
    )
    public ResponseEntity<SuccessResponse<ManualEntryFieldConfigResponse>> getFieldConfig(
            @RequestParam String activityType) {

        ManualEntryFieldConfigResponse config = fieldConfigService.getConfig(activityType);
        return ResponseEntity.ok(new SuccessResponse<>("Field configuration fetched successfully", config));
    }
}
