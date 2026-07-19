package com.mas.gov.bt.mas.primary.controller.RenewalEnviromentalClearance;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.payment.PaymentCallbackDTO;
import com.mas.gov.bt.mas.primary.dto.request.EnvironmentClearanceRenewalRequestDTO;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Renewal of environmental clearance Application management.
 */
@RestController
@RequestMapping("/api/renewal-environmental-clearance")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Renewal Environmental Clearance ", description = "Renewal Environmental Clearance Application Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class RenewalEnvironmentalClearanceController {

    private final UserContext userContext;

    private final RenewalEnvironmentalClearanceService renewalEnvironmentalClearanceService;

    @PostMapping("/draft")
    @Operation(
            summary = "Save EC renewal draft",
            description = "Save environment clearance renewal application as draft"
    )
    public ResponseEntity<SuccessResponse<EnvironmentClearanceRenewalResponseDTO>> saveDraft(
            @Valid @RequestBody EnvironmentClearanceRenewalRequestDTO request
    ) {

        Long userId = userContext.getCurrentUserId();

        EnvironmentClearanceRenewalResponseDTO response =
                renewalEnvironmentalClearanceService.saveDraft(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>(
                        "Draft saved successfully",
                        response
                ));
    }

    @PutMapping("/draft/{id}")
    @Operation(
            summary = "Update draft application",
            description = "Update an existing draft application"
    )
    public ResponseEntity<SuccessResponse<EnvironmentClearanceRenewalResponseDTO>> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody EnvironmentClearanceRenewalRequestDTO request
    ) {

        Long userId = userContext.getCurrentUserId();

        EnvironmentClearanceRenewalResponseDTO response =
                renewalEnvironmentalClearanceService.updateDraft(
                        id,
                        request,
                        userId
                );

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Draft updated successfully",
                        response
                )
        );
    }

    @DeleteMapping("/draft/{id}")
    @Operation(
            summary = "Delete draft application",
            description = "Delete an existing draft application"
    )
    public ResponseEntity<SuccessResponse<Void>> deleteDraft(
            @PathVariable Long id
    ) {

        Long userId = userContext.getCurrentUserId();

        renewalEnvironmentalClearanceService.deleteDraft(
                id,
                userId
        );

        return SuccessResponse.buildSuccessResponse(
                "Draft deleted successfully"
        );
    }

    @PostMapping("/submit")
    @Operation(
            summary = "Submit EC renewal application",
            description = "Submit environment clearance renewal application"
    )
    public ResponseEntity<SuccessResponse<EnvironmentClearanceRenewalResponseDTO>> submitApplication(
            @Valid @RequestBody EnvironmentClearanceRenewalRequestDTO request
    ) {

        Long userId = userContext.getCurrentUserId();

        EnvironmentClearanceRenewalResponseDTO response =
                renewalEnvironmentalClearanceService.submitApplication(
                        request,
                        userId
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>(
                        "Application submitted successfully",
                        response
                ));
    }

    @PutMapping("/resubmit/{id}")
    @Operation(
            summary = "Resubmit application",
            description = "Resubmit application after requested correction"
    )
    public ResponseEntity<SuccessResponse<EnvironmentClearanceRenewalResponseDTO>> resubmitApplication(
            @PathVariable Long id,
            @Valid @RequestBody EnvironmentClearanceRenewalRequestDTO request
    ) {

        Long userId = userContext.getCurrentUserId();

        EnvironmentClearanceRenewalResponseDTO response =
                renewalEnvironmentalClearanceService.resubmitApplication(
                        id,
                        request,
                        userId
                );

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Application resubmitted successfully",
                        response
                )
        );
    }


    @GetMapping("/my-applications")
    @Operation(summary = "Get my applications", description = "Get list of applications. others get only their own.")
    public ResponseEntity<SuccessResponse<List<EnvironmentClearanceRenewalResponseDTO>>> getMyApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Page<EnvironmentClearanceRenewalResponseDTO> applications;
            // Regular users can only see their own applications
            Long userId = userContext.getCurrentUserId();
            applications = renewalEnvironmentalClearanceService.getMyApplications(userId, pageable, search);

        return ResponseEntity.ok(SuccessResponse.fromPage("Applications retrieved successfully", applications));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get EC renewal application",
            description = "Retrieve environment clearance renewal application by ID"
    )
    public ResponseEntity<SuccessResponse<EnvironmentClearanceRenewalResponseDTO>> getApplication(
            @PathVariable Long id
    ) {

        Long userId = userContext.getCurrentUserId();

        EnvironmentClearanceRenewalResponseDTO response =
                renewalEnvironmentalClearanceService
                        .getApplicationById(id, userId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "Application retrieved successfully",
                        response
                )
        );
    }

    // The agency user path will be taken by super admin as all application
    // in Approved and Rejected will be pulled regardless of assignment
    @GetMapping("/archived-applications")
    @Operation(
            summary = "Get archived EC renewal applications",
            description = "Get list of archived (APPROVED/REJECTED) EC renewal applications"
    )
    public ResponseEntity<SuccessResponse<List<EnvironmentClearanceRenewalResponseDTO>>> getArchivedApplications(
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

        Page<EnvironmentClearanceRenewalResponseDTO> applications;

        Long userId = userContext.getCurrentUserId();

        if (userContext.isAgencyUser()) {

            applications =
                    renewalEnvironmentalClearanceService
                            .getArchivedApplications(
                                    pageable,
                                    search,
                                    userId
                            );

        } else {

            applications =
                    renewalEnvironmentalClearanceService
                            .getMyArchivedApplications(
                                    userId,
                                    pageable,
                                    search
                            );
        }

        return ResponseEntity.ok(
                SuccessResponse.fromPage(
                        "Archived applications retrieved successfully",
                        applications
                )
        );
    }

    @PostMapping("/{id}/pay-ec-fee")
    @Operation(
            summary = "Pay EC renewal fee",
            description = "Citizen pays the environmental clearance renewal fee through BIRMS, once MPCD has submitted the IOM"
    )
    public ResponseEntity<SuccessResponse<EnvironmentClearanceRenewalResponseDTO>> payEcFee(
            @PathVariable Long id
    ) {

        Long userId = userContext.getCurrentUserId();

        EnvironmentClearanceRenewalResponseDTO response =
                renewalEnvironmentalClearanceService.payEcFee(id, userId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "EC renewal fee payment initiated",
                        response
                )
        );
    }

    /**
     * Called by the masters payment service once the EC renewal fee payment is confirmed as
     * PAID. No JWT required — internal service call from mas-backend-masters (whitelisted in
     * SecurityConfig).
     */
    @PostMapping("/ec-payment-callback")
    @Operation(
            summary = "EC renewal fee payment callback",
            description = "Internal callback from payment service — confirms EC renewal fee payment and forwards the application to MD"
    )
    public ResponseEntity<SuccessResponse<Void>> ecPaymentCallback(
            @RequestBody PaymentCallbackDTO dto
    ) {

        renewalEnvironmentalClearanceService.onEcPaymentConfirmed(dto.getApplicationNo());

        return SuccessResponse.buildSuccessResponse(
                "EC renewal fee payment for application " + dto.getApplicationNo() + " has been confirmed."
        );
    }
}
