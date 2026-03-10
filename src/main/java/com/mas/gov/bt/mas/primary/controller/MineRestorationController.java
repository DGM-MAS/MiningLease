package com.mas.gov.bt.mas.primary.controller;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.MineRestorationCompletionReportResponse;
import com.mas.gov.bt.mas.primary.dto.response.MineRestorationProgressReportResponse;
import com.mas.gov.bt.mas.primary.dto.response.MineRestorationResponse;
import com.mas.gov.bt.mas.primary.services.MineRestorationService;
import com.mas.gov.bt.mas.primary.utility.PageRequest1Based;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mine-restoration/promoter")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mine Restoration - Promoter", description = "APIs for promoter to manage mine restoration applications")
@SecurityRequirement(name = "bearerAuth")
public class MineRestorationController {

    private final MineRestorationService mineRestorationService;
    private final UserContext userContext;

    // ---- MRP ----

    @PostMapping("/mrp")
    @Operation(summary = "Submit MRP", description = "Submit or save draft Mining Restoration Plan")
    public ResponseEntity<SuccessResponse<MineRestorationResponse>> submitMRP(
            @Valid @RequestBody MineRestorationMRPRequest request) {
        Long userId = userContext.getCurrentUserId();
        MineRestorationResponse response = mineRestorationService.submitMRP(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("MRP submitted successfully", response));
    }

    @PostMapping("/mrp/resubmit")
    @Operation(summary = "Resubmit MRP", description = "Resubmit revised MRP after revision request from ME")
    public ResponseEntity<SuccessResponse<MineRestorationResponse>> resubmitMRP(
            @Valid @RequestBody MineRestorationMRPResubmitRequest request) {
        Long userId = userContext.getCurrentUserId();
        MineRestorationResponse response = mineRestorationService.resubmitMRP(request, userId);
        return ResponseEntity.ok(new SuccessResponse<>("MRP resubmitted successfully", response));
    }

    // ---- Progress Report ----

    @PostMapping("/progress-report")
    @Operation(summary = "Submit progress report", description = "Submit or save draft progress report (every 6 months)")
    public ResponseEntity<SuccessResponse<MineRestorationProgressReportResponse>> submitProgressReport(
            @Valid @RequestBody MineRestorationProgressReportRequest request) {
        Long userId = userContext.getCurrentUserId();
        MineRestorationProgressReportResponse response =
                mineRestorationService.submitProgressReport(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Progress report submitted successfully", response));
    }

    // ---- Completion Report ----

    @PostMapping("/completion-report")
    @Operation(summary = "Submit completion report", description = "Submit or save draft Restoration Completion Report")
    public ResponseEntity<SuccessResponse<MineRestorationCompletionReportResponse>> submitCompletionReport(
            @Valid @RequestBody MineRestorationCompletionReportRequest request) {
        Long userId = userContext.getCurrentUserId();
        MineRestorationCompletionReportResponse response =
                mineRestorationService.submitCompletionReport(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Completion report submitted successfully", response));
    }

    // ---- Queries ----

    @GetMapping("/applications")
    @Operation(summary = "Get my restoration applications", description = "List all mine restoration applications for the logged-in promoter")
    public ResponseEntity<SuccessResponse<List<MineRestorationResponse>>> getMyApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Long userId = userContext.getCurrentUserId();
        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);
        return ResponseEntity.ok(mineRestorationService.getMyApplications(userId, search, pageable));
    }

    @GetMapping("/applications/{id}")
    @Operation(summary = "Get application by ID", description = "Get details of a specific mine restoration application")
    public ResponseEntity<SuccessResponse<MineRestorationResponse>> getApplicationById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                new SuccessResponse<>("Application retrieved successfully",
                        mineRestorationService.getApplicationById(id)));
    }

    @GetMapping("/applications/{applicationNumber}/progress-reports")
    @Operation(summary = "Get progress reports", description = "List all progress reports for a restoration application")
    public ResponseEntity<SuccessResponse<List<MineRestorationProgressReportResponse>>> getProgressReports(
            @PathVariable String applicationNumber,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest1Based.of(page, size);
        return ResponseEntity.ok(
                mineRestorationService.getProgressReports(applicationNumber, pageable));
    }

    @GetMapping("/applications/{applicationNumber}/completion-report")
    @Operation(summary = "Get completion report", description = "Get the Restoration Completion Report for an application")
    public ResponseEntity<SuccessResponse<MineRestorationCompletionReportResponse>> getCompletionReport(
            @PathVariable String applicationNumber) {
        return ResponseEntity.ok(
                new SuccessResponse<>("Completion report retrieved successfully",
                        mineRestorationService.getCompletionReport(applicationNumber)));
    }
}
