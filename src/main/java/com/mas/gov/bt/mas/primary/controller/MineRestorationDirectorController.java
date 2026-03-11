package com.mas.gov.bt.mas.primary.controller;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.ReviewMineRestorationCompletionRequest;
import com.mas.gov.bt.mas.primary.dto.response.MineRestorationCompletionReportResponse;
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
@RequestMapping("/api/mine-restoration/director")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mine Restoration - Director", description = "Director Review APIs for Mine Restoration")
@SecurityRequirement(name = "bearerAuth")
public class MineRestorationDirectorController {

    private final MineRestorationService mineRestorationService;
    private final UserContext userContext;

    @GetMapping("/applications")
    @Operation(summary = "Get all restoration applications",
            description = "List all mine restoration applications for Director oversight")
    public ResponseEntity<SuccessResponse<List<MineRestorationResponse>>> getAllApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        return ResponseEntity.ok(mineRestorationService.getAllApplicationsForDirector(search, pageable));
    }

    @GetMapping("/applications/{id}")
    @Operation(summary = "Get application by ID", description = "Get mine restoration application details by ID")
    public ResponseEntity<SuccessResponse<MineRestorationResponse>> getApplicationById(@PathVariable Long id) {
        return ResponseEntity.ok(new SuccessResponse<>("Application fetched successfully",
                mineRestorationService.getApplicationById(id)));
    }

    @GetMapping("/applications/{applicationNumber}/completion-report")
    @Operation(summary = "Get completion report", description = "View the Restoration Completion Report for an application")
    public ResponseEntity<SuccessResponse<MineRestorationCompletionReportResponse>> getCompletionReport(
            @PathVariable String applicationNumber) {
        return ResponseEntity.ok(new SuccessResponse<>("Completion report retrieved successfully",
                mineRestorationService.getCompletionReport(applicationNumber)));
    }

    @PostMapping("/completion-report/review")
    @Operation(summary = "Review completion report and decide ERB",
            description = "Director reviews Restoration Completion Report and decides ERB_RELEASED or ERB_UTILIZED")
    public ResponseEntity<SuccessResponse<MineRestorationResponse>> reviewCompletionReport(
            @Valid @RequestBody ReviewMineRestorationCompletionRequest request) {
        Long userId = userContext.getCurrentUserId();
        MineRestorationResponse response = mineRestorationService.reviewCompletionReport(request, userId);
        return ResponseEntity.ok(new SuccessResponse<>("Completion report reviewed successfully", response));
    }
}
