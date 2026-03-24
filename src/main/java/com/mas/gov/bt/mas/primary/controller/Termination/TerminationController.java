package com.mas.gov.bt.mas.primary.controller.Termination;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.MiningLeaseGRRequest;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
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
@Tag(name = "Termination", description = "Termination Application Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class TerminationController {

    private final UserContext userContext;
    private final TerminationService terminationService;

    // ========== Applicant APIs ==========

    // Used by user to submit Geological report
    // The initial stage of mining lease application submission
//    @PostMapping("/applicationsGR")
//    @Operation(summary = "Submit GR file application", description = "Submit PA/FC file for mining lease application")
//    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitGRFile(
//            @Valid @RequestBody MiningLeaseGRRequest request) {
//
//        Long userId = userContext.getCurrentUserId();
//        MiningLeaseResponse response = miningLeaseService.submitGR(request, userId);
//
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(new SuccessResponse<>("Application created successfully", response));
//    }
}
