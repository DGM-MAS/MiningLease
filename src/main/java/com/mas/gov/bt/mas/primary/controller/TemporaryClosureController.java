package com.mas.gov.bt.mas.primary.controller;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.ReassignTaskRequest;
import com.mas.gov.bt.mas.primary.dto.request.TemporaryClosureNotificationRequest;
import com.mas.gov.bt.mas.primary.dto.response.ApplicationListResponse;
import com.mas.gov.bt.mas.primary.dto.response.TemporaryClosureNotificationResponse;
import com.mas.gov.bt.mas.primary.services.TemporaryClosureService;
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
 * REST Controller for Temporary closure management.
 */
@RestController
@RequestMapping("/api/temporary-closure")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Temporary closure", description = "Temporary closure applicant endpoints are placed here")
@SecurityRequirement(name = "bearerAuth")
public class TemporaryClosureController {

    private final UserContext userContext;
    private final TemporaryClosureService temporaryClosureService;

    // ========== Applicant APIs ==========

    // Used by user to submit temporary closure notification with relevant documents
    // The initial stage of temporary closure submission

    @PostMapping("/application")
    @Operation(summary = "Submit temporary closure application", description = "Submit temporary closure application with reason are documents")
    public ResponseEntity<SuccessResponse<TemporaryClosureNotificationResponse>> submitGRFile(
            @Valid @RequestBody TemporaryClosureNotificationRequest request) {

        Long userId = userContext.getCurrentUserId();
        String email = userContext.getCurrentUserEmail();
        String applicantType = userContext.getCurrentUserType();
        TemporaryClosureNotificationResponse response = temporaryClosureService.submitApplication(request, userId, email, applicantType);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Temporary closure application submitted successfully", response));
    }


    @GetMapping("/my-applications")
    @Operation(summary = "Get my applications", description = "Get list of applications. Agency users get all applications, others get only their own.")
    public ResponseEntity<SuccessResponse<List<TemporaryClosureNotificationResponse>>> getMyApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Page<TemporaryClosureNotificationResponse> applications;
        if (userContext.isAgencyUser()) {
            // Agency users can see all applications
            applications = temporaryClosureService.getAllApplications(userContext.getCurrentUserId(), pageable);
        } else {
            // Regular users can only see their own applications
            Long userId = userContext.getCurrentUserId();
            applications = temporaryClosureService.getMyApplications(userId, pageable, search);
        }

        return ResponseEntity.ok(SuccessResponse.fromPage("Applications retrieved successfully", applications));
    }

    @PostMapping("/rectification")
    @Operation(summary = "Submit rectification for temporary closure application", description = "Submit rectification temporary closure application with reason are documents")
    public ResponseEntity<SuccessResponse<TemporaryClosureNotificationResponse>> submitRectification(
            @Valid @RequestBody TemporaryClosureNotificationRequest request) {

        Long userId = userContext.getCurrentUserId();
        TemporaryClosureNotificationResponse response = temporaryClosureService.submitRectification(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Rectification submitted successfully", response));
    }

    @GetMapping("/applications/{applicationNo}")
    @Operation(summary = "Get application by number", description = "Get application details by application number. Agency users can view any application, others can only view their own.")
    public ResponseEntity<SuccessResponse<TemporaryClosureNotificationResponse>> getApplicationByNumber(
            @PathVariable String applicationNo) {

        TemporaryClosureNotificationResponse response = temporaryClosureService.getApplicationByNumber(applicationNo);

        return ResponseEntity.ok(new SuccessResponse<>("Application retrieved successfully", response));
    }

    @PutMapping("/tasks/reassignRC")
    @Operation(summary = "Reassign task", description = "Reassign a pending task to another user")
    public ResponseEntity<SuccessResponse<Void>> reassignTaskRC(
            @Valid @RequestBody ReassignTaskRequest request) {

        Long userId = userContext.getCurrentUserId();
        temporaryClosureService.reassignTaskRC(request, userId);

        return SuccessResponse.buildSuccessResponse("Task reassigned successfully");
    }

    @PutMapping("/tasks/reassignMI")
    @Operation(summary = "Reassign task", description = "Reassign a pending task to another user")
    public ResponseEntity<SuccessResponse<Void>> reassignTaskMI(
            @Valid @RequestBody ReassignTaskRequest request) {

        Long userId = userContext.getCurrentUserId();
        temporaryClosureService.reassignTaskMI(request, userId);

        return SuccessResponse.buildSuccessResponse("Task reassigned successfully");
    }

    @GetMapping("/all")
    @Operation(summary = "Get all renewal applications", description = "Get paginated list of all renewal applications (admin view)")
    public ResponseEntity<SuccessResponse<List<TemporaryClosureNotificationResponse>>> getAllApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        return ResponseEntity.ok(temporaryClosureService.getAllApplicationAdmin(pageable, search));
    }

    @GetMapping("/archived-applications")
    @Operation(summary = "Get archived applications", description = "Get list of archived (APPROVED/REJECTED) applications. Agency users get all archived applications, others get only their own.")
    public ResponseEntity<SuccessResponse<List<TemporaryClosureNotificationResponse>>> getArchivedApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Page<TemporaryClosureNotificationResponse> applications;
        if (userContext.isAgencyUser()) {
            // Agency users can see all archived applications
            Long userId = userContext.getCurrentUserId();
            applications = temporaryClosureService.getArchivedApplications(pageable, search, userId);
        } else {
            // Regular users can only see their own archived applications
            Long userId = userContext.getCurrentUserId();
            applications = temporaryClosureService.getMyArchivedApplications(userId, pageable, search);
        }

        return ResponseEntity.ok(SuccessResponse.fromPage("Archived applications retrieved successfully", applications));
    }

}
