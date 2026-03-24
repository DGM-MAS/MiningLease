package com.mas.gov.bt.mas.primary.controller.Termination;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.TerminationApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.response.TerminationApplicationResponse;
import com.mas.gov.bt.mas.primary.services.TerminationService;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Termination Application management.
 */
@RestController
@RequestMapping("/api/termination")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Termination Chief User", description = "Termination Chief User Application Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class TerminationController {

    private final UserContext userContext;
    private final TerminationService terminationService;

    // ========== Applicant APIs ==========

    // Used by chief to submit termination report
    // The initial stage of termination application submission
    @PostMapping("/applications")
    @Operation(summary = "Submit termination file application", description = "Submit termination file for termination application")
    public ResponseEntity<SuccessResponse<TerminationApplicationResponse>> submitApplication(
            @Valid @RequestBody TerminationApplicationRequest request) {

        Long userId = userContext.getCurrentUserId();
        TerminationApplicationResponse response = terminationService.submitTerminationApplication(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }


}
