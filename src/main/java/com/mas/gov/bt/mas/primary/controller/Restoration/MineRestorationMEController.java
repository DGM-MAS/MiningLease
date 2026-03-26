package com.mas.gov.bt.mas.primary.controller.Restoration;

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mine-restoration/mining-engineer")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mine Restoration - Mining Engineer", description = "APIs for Mining Engineer to review and manage mine restoration")
@SecurityRequirement(name = "bearerAuth")
public class MineRestorationMEController {

    private final MineRestorationService mineRestorationService;
    private final UserContext userContext;

    @GetMapping("/assigned")
    @Operation(summary = "Get assigned restoration applications",
            description = "List all mine restoration applications assigned to the logged-in Mining Engineer")
    public ResponseEntity<SuccessResponse<List<MineRestorationResponse>>> getAssigned(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Long userId = userContext.getCurrentUserId();
        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);
        return ResponseEntity.ok(mineRestorationService.getAssignedToME(userId, search, pageable));
    }

    @GetMapping("/applications/{id}")
    @Operation(summary = "Get application by ID", description = "View full details of a restoration application")
    public ResponseEntity<SuccessResponse<MineRestorationResponse>> getApplicationById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                new SuccessResponse<>("Application retrieved successfully",
                        mineRestorationService.getApplicationById(id)));
    }

    @PostMapping("/mrp/review")
    @Operation(summary = "Review MRP",
            description = "Review Mining Restoration Plan: APPROVED, REVISION_REQUESTED, or REJECTED")
    public ResponseEntity<SuccessResponse<MineRestorationResponse>> reviewMRP(
            @Valid @RequestBody ReviewMineRestorationMRPRequest request) {
        Long userId = userContext.getCurrentUserId();
        MineRestorationResponse response = mineRestorationService.reviewMRP(request, userId);
        return ResponseEntity.ok(new SuccessResponse<>("MRP reviewed successfully", response));
    }

    @PostMapping("/work-order")
    @Operation(summary = "Upload and issue work order",
            description = "Upload work order document after MRP approval to begin restoration")
    public ResponseEntity<SuccessResponse<MineRestorationResponse>> uploadWorkOrder(
            @RequestParam Long restorationApplicationId,
            @RequestParam String workOrderDocId) {
        Long userId = userContext.getCurrentUserId();
        MineRestorationResponse response =
                mineRestorationService.uploadWorkOrder(restorationApplicationId, workOrderDocId, userId);
        return ResponseEntity.ok(new SuccessResponse<>("Work order issued successfully", response));
    }

    @GetMapping("/applications/{applicationNumber}/progress-reports")
    @Operation(summary = "Get progress reports", description = "View all progress reports for a restoration application")
    public ResponseEntity<SuccessResponse<List<MineRestorationProgressReportResponse>>> getProgressReports(
            @PathVariable String applicationNumber,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest1Based.of(page, size);
        return ResponseEntity.ok(mineRestorationService.getProgressReports(applicationNumber, pageable));
    }

    @PostMapping("/progress-report/review")
    @Operation(summary = "Review progress report",
            description = "Review progress report (with RC verification): REVIEWED to continue, COMPLETION_REQUESTED when restoration is done")
    public ResponseEntity<SuccessResponse<MineRestorationProgressReportResponse>> reviewProgressReport(
            @Valid @RequestBody ReviewMineRestorationProgressRequest request) {
        Long userId = userContext.getCurrentUserId();
        MineRestorationProgressReportResponse response =
                mineRestorationService.reviewProgressReport(request, userId);
        return ResponseEntity.ok(new SuccessResponse<>("Progress report reviewed successfully", response));
    }

    @GetMapping("/applications/{applicationNumber}/completion-report")
    @Operation(summary = "Get completion report", description = "View the Restoration Completion Report submitted by promoter")
    public ResponseEntity<SuccessResponse<MineRestorationCompletionReportResponse>> getCompletionReport(
            @PathVariable String applicationNumber) {
        return ResponseEntity.ok(
                new SuccessResponse<>("Completion report retrieved successfully",
                        mineRestorationService.getCompletionReport(applicationNumber)));
    }

    @PostMapping("/completion-report/review")
    @Operation(summary = "Review completion report and decide ERB",
            description = "Review Restoration Completion Report and decide ERB_RELEASED (satisfactory) or ERB_UTILIZED (not satisfactory)")
    public ResponseEntity<SuccessResponse<MineRestorationResponse>> reviewCompletionReport(
            @Valid @RequestBody ReviewMineRestorationCompletionRequest request) {
        Long userId = userContext.getCurrentUserId();
        MineRestorationResponse response =
                mineRestorationService.reviewCompletionReport(request, userId);
        return ResponseEntity.ok(new SuccessResponse<>("Completion report reviewed successfully", response));
    }

    @PostMapping("/erb-release-letter")
    @Operation(summary = "Issue ERB Release Letter",
            description = "Upload and issue the ERB Release Letter after approving the completion report. Finalizes the restoration as ERB_RELEASED.")
    public ResponseEntity<SuccessResponse<MineRestorationResponse>> issueERBReleaseLetter(
            @Valid @RequestBody IssueERBReleaseLetterRequest request) {
        Long userId = userContext.getCurrentUserId();
        MineRestorationResponse response =
                mineRestorationService.issueERBReleaseLetter(request.getRestorationApplicationId(), request.getErbReleaseLetterDocId(), userId);
        return ResponseEntity.ok(new SuccessResponse<>("ERB Release Letter issued successfully", response));
    }
}
