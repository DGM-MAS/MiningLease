package com.mas.gov.bt.mas.primary.controller.ManualEntry;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.QuarryLeaseApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.QuarryLeaseResponse;
import com.mas.gov.bt.mas.primary.dto.request.ManualMiningEntryRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.MiningLeaseApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.response.ApplicationListResponse;
import com.mas.gov.bt.mas.primary.dto.response.ManualMiningEntryResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.services.ManualMiningEntryService;
import com.mas.gov.bt.mas.primary.services.ManualMiningLeaseService;
import com.mas.gov.bt.mas.primary.utility.PageRequest1Based;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
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
public class ManualMiningEntryController {

    private final ManualMiningLeaseService manualMiningLeaseService;
    private final ManualMiningEntryService service;
    private final UserContext userContext;

    @PostMapping("/applications")
    @Operation(
            summary = "Create manual mining entry",
            description = "Save manual mining / quarry / SC / stock lifting entry with attachments"
    )
    public ResponseEntity<SuccessResponse<ManualMiningEntryResponseDTO>> createApplication(
            @Valid @RequestBody ManualMiningEntryRequestDTO request) {

        Long userId = userContext.getCurrentUserId();

        ManualMiningEntryResponseDTO response =
                service.createApplication(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    @PostMapping("/manual-mining-applications")
    @Operation(summary = "Create new application", description = "Create a new mining lease application in draft state")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> createApplication(
            @Valid @RequestBody MiningLeaseApplicationRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = manualMiningLeaseService.createApplication(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    @PostMapping("/manual-quarry-applications")
    @Operation(summary = "Create new application", description = "Create a new quarry lease application in draft state")
    public ResponseEntity<SuccessResponse<QuarryLeaseResponse>> createApplication(
            @Valid @RequestBody QuarryLeaseApplicationRequest request) {

        Long userId = userContext.getCurrentUserId();
        QuarryLeaseResponse response = manualMiningLeaseService.createApplicationQuarry(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }


    @GetMapping("/manual-mining-archived-applications")
    @Operation(summary = "Get archived applications", description = "Get list of archived (APPROVED/REJECTED) applications. Agency users get all archived applications, others get only their own.")
    public ResponseEntity<SuccessResponse<List<ApplicationListResponse>>> getArchivedApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Page<ApplicationListResponse> applications;
        if (userContext.isAgencyUser()) {
            // Agency users can see all archived applications
            Long userId = userContext.getCurrentUserId();
            applications = manualMiningLeaseService.getArchivedApplications(pageable, search, userId);
        } else {
            // Regular users can only see their own archived applications
            Long userId = userContext.getCurrentUserId();
            applications = manualMiningLeaseService.getMyArchivedApplications(userId, pageable, search);
        }

        return ResponseEntity.ok(SuccessResponse.fromPage("Archived applications retrieved successfully", applications));
    }

}