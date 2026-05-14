package com.mas.gov.bt.mas.primary.controller.RenewalEnviromentalClearance;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.EnvironmentClearanceRenewalResponseDTO;
import com.mas.gov.bt.mas.primary.services.RenewalEnvironmentalClearanceService;
import com.mas.gov.bt.mas.primary.utility.PageRequest1Based;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;
import java.util.List;

@RestController
@RequestMapping("/api/renewal-environmental-clearance/mpcd")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Renewal environmental clearance MPCD", description = "Renewal environmental clearance Application Management APIs MPCD")
@SecurityRequirement(name = "bearerAuth")
public class RenewalEnvironmentalClearanceMPCDController {

    private final UserContext userContext;

    private final RenewalEnvironmentalClearanceService renewalEnvironmentalClearanceService;

    @PostMapping("/review")
    @Operation(
            summary = "Review EC renewal application",
            description = "Review assigned environmental clearance renewal application by MPCD"
    )
    public ResponseEntity<SuccessResponse<EnvironmentClearanceRenewalResponseDTO>> reviewApplicationMPCD(
            @Valid @RequestBody ReviewEnvironmentClearanceMPCDRequest request
    ) {

        Long userId = userContext.getCurrentUserId();

        EnvironmentClearanceRenewalResponseDTO response =
                renewalEnvironmentalClearanceService.reviewApplicationMPCD(
                        request,
                        userId
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>(
                        "Application reviewed successfully",
                        response
                ));
    }

    @GetMapping("/assigned")
    @Operation(
            summary = "Assigned EC renewal applications",
            description = "Get assigned environmental clearance renewal applications for MPCD"
    )
    public ResponseEntity<SuccessResponse<List<EnvironmentClearanceRenewalResponseDTO>>> assignedToMPCD(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {

        Pageable pageable = PageRequest1Based.of(
                page,
                size,
                Sort.Direction.fromString(sortDirection),
                sortBy
        );

        Long userId = userContext.getCurrentUserId();

        return ResponseEntity.ok(
                renewalEnvironmentalClearanceService
                        .getAssignedToMPCD(userId, pageable, search)
        );
    }

    @PutMapping("/tasks/reassign")
    @Operation(
            summary = "Reassign MPCD task",
            description = "Reassign a pending EC renewal task to another MPCD officer"
    )
    public ResponseEntity<SuccessResponse<Void>> reassignTaskMPCD(
            @Valid @RequestBody ReassignTaskRequest request
    ) {

        Long userId = userContext.getCurrentUserId();

        renewalEnvironmentalClearanceService
                .reassignTaskMPCD(request, userId);

        return SuccessResponse.buildSuccessResponse(
                "Task reassigned successfully"
        );
    }

    @GetMapping("/archived-applications")
    @Operation(
            summary = "Get MPCD archived applications",
            description = "Get approved/rejected EC renewal applications handled by the logged-in MPCD focal"
    )
    public ResponseEntity<SuccessResponse<List<EnvironmentClearanceRenewalResponseDTO>>> getArchivedApplicationsMPCD(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {

        Pageable pageable = PageRequest1Based.of(
                page,
                size,
                Sort.Direction.fromString(sortDirection),
                sortBy.trim()
        );

        Long userId = userContext.getCurrentUserId();

        Page<EnvironmentClearanceRenewalResponseDTO> applications =
                renewalEnvironmentalClearanceService
                        .getArchivedApplicationsMPCD(
                                userId,
                                pageable,
                                search
                        );

        return ResponseEntity.ok(
                SuccessResponse.fromPage(
                        "Archived applications retrieved successfully",
                        applications
                )
        );
    }

    @PutMapping("/request-resubmission")
    @Operation(
            summary = "Request resubmission",
            description = "Request applicant to resubmit additional documents"
    )
    public ResponseEntity<SuccessResponse<EnvironmentClearanceRenewalResponseDTO>> requestResubmission(
            @Valid @RequestBody RequestResubmissionDTO request
    ) {

        Long userId = userContext.getCurrentUserId();

        EnvironmentClearanceRenewalResponseDTO response =
                renewalEnvironmentalClearanceService
                        .requestResubmission(
                                request,
                                userId
                        );

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Resubmission requested successfully",
                        response
                )
        );
    }

    @PutMapping("/reject")
    @Operation(
            summary = "Reject application",
            description = "Reject EC renewal application"
    )
    public ResponseEntity<SuccessResponse<EnvironmentClearanceRenewalResponseDTO>> rejectApplication(
            @Valid @RequestBody RejectApplicationDTO request
    ) {

        Long userId = userContext.getCurrentUserId();

        EnvironmentClearanceRenewalResponseDTO response =
                renewalEnvironmentalClearanceService
                        .rejectApplicationMPCD(
                                request,
                                userId
                        );

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Application rejected successfully",
                        response
                )
        );
    }

    @PostMapping("/assign-rc")
    @Operation(
            summary = "Assign RC",
            description = "Assign Regional Coordinator for site assessment"
    )
    public ResponseEntity<SuccessResponse<EnvironmentClearanceRenewalResponseDTO>> assignRC(
            @Valid @RequestBody AssignRCRequestDTO request
    ) {

        Long userId = userContext.getCurrentUserId();

        EnvironmentClearanceRenewalResponseDTO response =
                renewalEnvironmentalClearanceService.assignRC(
                        request,
                        userId
                );

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "RC assigned successfully",
                        response
                )
        );
    }
}
