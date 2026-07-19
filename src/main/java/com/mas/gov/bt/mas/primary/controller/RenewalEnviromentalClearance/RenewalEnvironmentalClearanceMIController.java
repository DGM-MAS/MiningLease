package com.mas.gov.bt.mas.primary.controller.RenewalEnviromentalClearance;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.RequestResubmissionDTO;
import com.mas.gov.bt.mas.primary.dto.request.SubmitMIReportDTO;
import com.mas.gov.bt.mas.primary.dto.response.EnvironmentClearanceRenewalResponseDTO;
import com.mas.gov.bt.mas.primary.services.RenewalEnvironmentalClearanceService;
import com.mas.gov.bt.mas.primary.utility.PageRequest1Based;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;
import java.util.List;

@RestController
@RequestMapping("/api/renewal-environmental-clearance/mi")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Renewal environmental clearance MI", description = "Renewal environmental clearance Application Management APIs MI")
@SecurityRequirement(name = "bearerAuth")
public class RenewalEnvironmentalClearanceMIController {

    private final UserContext userContext;

    private final RenewalEnvironmentalClearanceService renewalEnvironmentalClearanceService;

    @GetMapping("/assigned")
    public ResponseEntity<SuccessResponse<List<EnvironmentClearanceRenewalResponseDTO>>> assignedToMI(
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

        return ResponseEntity.ok(
                renewalEnvironmentalClearanceService
                        .getAssignedToMI(
                                userId,
                                pageable,
                                search
                        )
        );
    }

    @PostMapping("/submit-report")
    public ResponseEntity<SuccessResponse<EnvironmentClearanceRenewalResponseDTO>> submitMIReport(
            @Valid @RequestBody SubmitMIReportDTO request
    ) {

        Long userId = userContext.getCurrentUserId();

        EnvironmentClearanceRenewalResponseDTO response =
                renewalEnvironmentalClearanceService
                        .submitMIReport(
                                request,
                                userId
                        );

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "MI report submitted successfully",
                        response
                )
        );
    }

    @PutMapping("/request-resubmission")
    public ResponseEntity<SuccessResponse<EnvironmentClearanceRenewalResponseDTO>> requestResubmission(
            @Valid @RequestBody RequestResubmissionDTO request
    ) {

        Long userId = userContext.getCurrentUserId();

        EnvironmentClearanceRenewalResponseDTO response =
                renewalEnvironmentalClearanceService
                        .requestResubmissionMI(
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
}
