package com.mas.gov.bt.mas.primary.controller.Termination;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.TerminationApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.dto.response.TerminationApplicationResponse;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.services.TerminationService;
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
    public ResponseEntity<SuccessResponse<List<TerminationApplicationResponse>>> submitApplication(
            @Valid @RequestBody TerminationApplicationRequest request) {

        Long userId = userContext.getCurrentUserId();
        List<TerminationApplicationResponse> response = terminationService.submitTerminationApplication(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("The report has been successfully submitted to CMS Head.", response));
    }

    @GetMapping("/my-applications")
    @Operation(summary = "Get my applications", description = "Get list of applications. Agency users get all applications, others get only their own.")
    public ResponseEntity<SuccessResponse<List<TerminationApplicationResponse>>> getMyApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Pageable pageable = PageRequest1Based.of(page, size);

        if (userContext.isAgencyUser()) {
            return ResponseEntity.ok(terminationService.getAllApplications(userContext.getCurrentUserId(), pageable, search)) ;
        }else {
            throw new BusinessException("The Current user is not allowed to access these data");
        }
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
            // Regular users can only see their own archived applications
            Long userId = userContext.getCurrentUserId();
            applications = terminationService.getMyArchivedApplications(userId, pageable, search);
        }

        return ResponseEntity.ok(SuccessResponse.fromPage("Archived applications retrieved successfully", applications));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all Termination applications", description = "Get paginated list of all termination applications (admin view)")
    public ResponseEntity<SuccessResponse<List<TerminationApplicationResponse>>> getAllApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        return ResponseEntity.ok(terminationService.getAllApplicationAdmin(pageable, search));
    }

}
