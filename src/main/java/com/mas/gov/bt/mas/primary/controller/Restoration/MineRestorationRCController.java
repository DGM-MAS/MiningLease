package com.mas.gov.bt.mas.primary.controller.Restoration;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.MineRestorationVerificationReportRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mine-restoration/regional-coordinator")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mine Restoration - Regional Coordinator / Mining Inspector",
        description = "APIs for RC/MI to verify progress reports within 14 days")
@SecurityRequirement(name = "bearerAuth")
public class MineRestorationRCController {

    private final MineRestorationService mineRestorationService;
    private final UserContext userContext;

    @GetMapping("/applications")
    @Operation(summary = "Get active restoration applications",
            description = "List all mine restoration applications currently in progress (for RC/MI oversight)")
    public ResponseEntity<SuccessResponse<List<MineRestorationResponse>>> getActiveApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);
        return ResponseEntity.ok(
                mineRestorationService.getActiveApplicationsForRC(search, pageable));
    }

    @GetMapping("/progress-reports")
    @Operation(summary = "Get progress reports assigned to me",
            description = "List all progress reports submitted by promoters assigned to this RC/MI")
    public ResponseEntity<SuccessResponse<List<MineRestorationProgressReportResponse>>> getProgressReports(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Long userId = userContext.getCurrentUserId();
        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);
        return ResponseEntity.ok(
                mineRestorationService.getProgressReportsForRC(userId, search, pageable));
    }

    @GetMapping("/applications/{applicationNumber}/progress-reports")
    @Operation(summary = "Get all progress reports for an application",
            description = "View all progress reports and MRP for a specific restoration application")
    public ResponseEntity<SuccessResponse<List<MineRestorationProgressReportResponse>>> getProgressReportsByApplication(
            @PathVariable String applicationNumber,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest1Based.of(page, size);
        return ResponseEntity.ok(
                mineRestorationService.getProgressReports(applicationNumber, pageable));
    }

    @PostMapping("/verification-report")
    @Operation(summary = "Submit verification report",
            description = "Submit site verification report for a progress report (must be done within 14 days)")
    public ResponseEntity<SuccessResponse<MineRestorationProgressReportResponse>> submitVerificationReport(
            @Valid @RequestBody MineRestorationVerificationReportRequest request) {
        Long userId = userContext.getCurrentUserId();
        MineRestorationProgressReportResponse response =
                mineRestorationService.submitVerificationReport(request, userId);
        return ResponseEntity.ok(new SuccessResponse<>("Verification report submitted successfully", response));
    }
}
