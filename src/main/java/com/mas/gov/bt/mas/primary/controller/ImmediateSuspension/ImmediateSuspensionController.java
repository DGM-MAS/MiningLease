package com.mas.gov.bt.mas.primary.controller.ImmediateSuspension;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.AssignedTaskRC;
import com.mas.gov.bt.mas.primary.dto.request.ImmediateSuspensionApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.request.PromoterImmediateSuspensionRequest;
import com.mas.gov.bt.mas.primary.dto.request.RcMeImmediateSuspensionRequest;
import com.mas.gov.bt.mas.primary.dto.response.ImmediateSuspensionApplicationResponse;
import com.mas.gov.bt.mas.primary.dto.response.TemporaryClosureNotificationResponse;
import com.mas.gov.bt.mas.primary.services.ImmediateSuspensionService;
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
 * REST Controller for Immediate Suspension Application management.
 */
@RestController
@RequestMapping("/api/immediate-suspension")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Immediate Suspension RC/Mining Engineer User", description = "Immediate Suspension RC/Mining Engineer User Application Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class ImmediateSuspensionController {

    private final UserContext userContext;
    private final ImmediateSuspensionService immediateSuspensionService;

    // ========== Applicant APIs ==========

    // Used by RC/ Mining engineer to submit immediate suspension report
    // The initial stage of immediate suspension application submission
    @PostMapping("/applications")
    @Operation(summary = "Submit immediate suspension application submission", description = "Submit immediate suspension application")
    public ResponseEntity<SuccessResponse<ImmediateSuspensionApplicationResponse>> submitApplication(
            @Valid @RequestBody ImmediateSuspensionApplicationRequest request) {

        Long userId = userContext.getCurrentUserId();
        ImmediateSuspensionApplicationResponse response = immediateSuspensionService.submitImmediateSuspensionApplication(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("The report has been successfully submitted to CMS Head.", response));
    }

    @PostMapping("/assignTask")
    @Operation(summary = "Assign application", description = "Assign immediate suspension application assign task to MI.")
    public ResponseEntity<SuccessResponse<ImmediateSuspensionApplicationResponse>> assignApplication(
            @Valid @RequestBody AssignedTaskRC request) {

        Long userId = userContext.getCurrentUserId();
        ImmediateSuspensionApplicationResponse response = immediateSuspensionService.assignApplicationDirector(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application assigned successfully", response));
    }


    @PostMapping("/review")
    @Operation(summary = "Rectification application", description = "Rectification Termination application by RC/ME")
    public ResponseEntity<SuccessResponse<ImmediateSuspensionApplicationResponse>> reviewApplication(
            @Valid @RequestBody RcMeImmediateSuspensionRequest request) {

        Long userId = userContext.getCurrentUserId();
        ImmediateSuspensionApplicationResponse response = immediateSuspensionService.reviewApplicationRCME(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }


}
