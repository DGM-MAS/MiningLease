package com.mas.gov.bt.mas.primary.controller.Termination;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.ReassignTaskRequest;
import com.mas.gov.bt.mas.primary.dto.request.ReviewTerminationApplicationCMSHead;
import com.mas.gov.bt.mas.primary.dto.response.TerminationApplicationResponse;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.services.TerminationService;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
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
 * REST Controller for MI Termination Application management.
 */
@RestController
@RequestMapping("/api/termination/cms-head")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CMS HEAD Termination", description = "CMS HEAD Termination Application Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class CMSHeadTerminationController {

    private final UserContext userContext;
    private final TerminationService terminationService;

    // ** Dashboard information for CMS HEAD Termination application ** //
    // ** Assigned application to CMS HEAD ** //
    @GetMapping("/assigned")
    public ResponseEntity<SuccessResponse<List<TerminationApplicationResponse>>> assignedToMI(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(terminationService.getAssignedToCMSHead(userId, pageable, search)
        );
    }

    // ========== Task Reassignment ==========

    @PutMapping("/tasks/reassignCMS")
    @Operation(summary = "Reassign task", description = "Reassign a pending task to another user")
    public ResponseEntity<SuccessResponse<Void>> reassignTaskCMS(
            @Valid @RequestBody ReassignTaskRequest request) {

        Long userId = userContext.getCurrentUserId();
        terminationService.reassignTaskCMS(request, userId);

        return SuccessResponse.buildSuccessResponse("Task reassigned successfully");
    }

    @PostMapping("/review")
    @Operation(summary = "Review application", description = "Review Termination application by CMS Head")
    public ResponseEntity<SuccessResponse<TerminationApplicationResponse>> reviewApplication(
            @Valid @RequestBody ReviewTerminationApplicationCMSHead request) {

        Long userId = userContext.getCurrentUserId();
        TerminationApplicationResponse response = terminationService.reviewApplicationCMSHead(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }

    @GetMapping("/archived-applications")
    @Operation(summary = "Get archived applications", description = "Get list of archived (APPROVED/REJECTED) applications. Agency users get all archived applications, others get only their own.")
    public ResponseEntity<SuccessResponse<List<TerminationApplicationResponse>>> getArchivedApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Page<TerminationApplicationResponse> applications;
        if (userContext.isAgencyUser()) {
            // Agency users can see all archived applications
            Long userId = userContext.getCurrentUserId();
            applications = terminationService.getArchivedApplications(pageable, search, userId);
        } else {
          throw new BusinessException(ErrorCodes.BUSINESS_RULE_VIOLATION);
        }

        return ResponseEntity.ok(SuccessResponse.fromPage("Archived applications retrieved successfully", applications));
    }
}
