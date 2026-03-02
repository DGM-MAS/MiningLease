package com.mas.gov.bt.mas.primary.controller;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.ApplicationListResponse;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.dto.response.TaskManagementAssignedUser;
import com.mas.gov.bt.mas.primary.services.MiningLeaseService;
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
 * REST Controller for Mining Lease Application management.
 */
@RestController
@RequestMapping("/api/mining-lease")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mining Lease", description = "Mining Lease Application Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class MiningLeaseController {


    private final UserContext userContext;
    private final MiningLeaseService miningLeaseService;

    // ========== Applicant APIs ==========

    // Used by user to submit Geological report
    // The initial stage of mining lease application submission
    @PostMapping("/applicationsGR")
    @Operation(summary = "Submit GR file application", description = "Submit PA/FC file for mining lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitGRFile(
            @Valid @RequestBody MiningLeaseGRRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseService.submitGR(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    // After geological report is accepted then the client will be able to submit mining lease application
    @PostMapping("/applications")
    @Operation(summary = "Create new application", description = "Create a new mining lease application in draft state")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> createApplication(
            @Valid @RequestBody MiningLeaseApplicationRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseService.createApplication(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    @GetMapping("/my-applications")
    @Operation(summary = "Get my applications", description = "Get list of applications. Agency users get all applications, others get only their own.")
    public ResponseEntity<SuccessResponse<List<ApplicationListResponse>>> getMyApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Page<ApplicationListResponse> applications;
        if (userContext.isAgencyUser()) {
            // Agency users can see all applications
            applications = miningLeaseService.getAllApplications(userContext.getCurrentUserId(), pageable);
        } else {
            // Regular users can only see their own applications
            Long userId = userContext.getCurrentUserId();
            applications = miningLeaseService.getMyApplications(userId, pageable);
        }

        return ResponseEntity.ok(SuccessResponse.fromPage("Applications retrieved successfully", applications));
    }

    @GetMapping("/applications/{applicationNo}")
    @Operation(summary = "Get application by number", description = "Get application details by application number. Agency users can view any application, others can only view their own.")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> getApplicationByNumber(
            @PathVariable String applicationNo) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseService.getApplicationByNumber(applicationNo, userId, userContext.isAgencyUser());

        return ResponseEntity.ok(new SuccessResponse<>("Application retrieved successfully", response));
    }


    @GetMapping("/archived-applications")
    @Operation(summary = "Get archived applications", description = "Get list of archived (APPROVED/REJECTED) applications. Agency users get all archived applications, others get only their own.")
    public ResponseEntity<SuccessResponse<List<ApplicationListResponse>>> getArchivedApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Page<ApplicationListResponse> applications;
        if (userContext.isAgencyUser()) {
            // Agency users can see all archived applications
            applications = miningLeaseService.getArchivedApplications(pageable);
        } else {
            // Regular users can only see their own archived applications
            Long userId = userContext.getCurrentUserId();
            applications = miningLeaseService.getMyArchivedApplications(userId, pageable);
        }

        return ResponseEntity.ok(SuccessResponse.fromPage("Archived applications retrieved successfully", applications));
    }

    // ** Dashboard information for MPCD mining lease application ** //
    // ** End of process flow ACCEPTED application only ** //
    @GetMapping("/archived")
    public ResponseEntity<SuccessResponse<List<MiningLeaseResponse>>> archivedMPCD(
            @RequestParam(required = false) String search,
            Pageable pageable) {

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(
                miningLeaseService.getArchivedApplicationToMPCD(userId, pageable, search)
        );
    }

    // ========== Task Reassignment ==========

    @PutMapping("/tasks/reassignGeologist")
    @Operation(summary = "Reassign task", description = "Reassign a pending task to another user")
    public ResponseEntity<SuccessResponse<Void>> reassignTaskGeologist(
            @Valid @RequestBody ReassignTaskRequest request) {

        Long userId = userContext.getCurrentUserId();
        miningLeaseService.reassignTaskGeologist(request, userId);

        return SuccessResponse.buildSuccessResponse("Task reassigned successfully");
    }

    @PutMapping("/tasks/reassignMPCD")
    @Operation(summary = "Reassign task", description = "Reassign a pending task to another user")
    public ResponseEntity<SuccessResponse<Void>> reassignTaskMPCD(
            @Valid @RequestBody ReassignTaskRequest request) {

        Long userId = userContext.getCurrentUserId();
        miningLeaseService.reassignTaskMPCD(request, userId);

        return SuccessResponse.buildSuccessResponse("Task reassigned successfully");
    }

    @PutMapping("/tasks/reassignME")
    @Operation(summary = "Reassign task", description = "Reassign a pending task to another user")
    public ResponseEntity<SuccessResponse<Void>> reassignTaskME(
            @Valid @RequestBody ReassignTaskRequest request) {

        Long userId = userContext.getCurrentUserId();
        miningLeaseService.reassignTaskME(request, userId);

        return SuccessResponse.buildSuccessResponse("Task reassigned successfully");
    }

    @PutMapping("/tasks/reassignMineChief")
    @Operation(summary = "Reassign task", description = "Reassign a pending task to another user")
    public ResponseEntity<SuccessResponse<Void>> reassignTaskMineChief(
            @Valid @RequestBody ReassignTaskRequest request) {

        Long userId = userContext.getCurrentUserId();
        miningLeaseService.reassignTaskMineChief(request, userId);

        return SuccessResponse.buildSuccessResponse("Task reassigned successfully");
    }

    // Used by user to submit PA/FC
    @PostMapping("/applicationsPAFC")
    @Operation(summary = "Submit PA/FC file application", description = "Submit PA/FC file for Mining lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitPAFCFile(
            @Valid @RequestBody MiningLeasePAFCRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseService.submitPAFC(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }


    // Used by user to submit FMFS
    @PostMapping("/applicationsFMFS")
    @Operation(summary = "Submit FMFS file application", description = "Submit FMFS file for mining lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitFMFSFile(
            @Valid @RequestBody MiningLeaseFMFSRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseService.submitFMFS(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }


    // Used by user to submit FMFS
    @PostMapping("/applicationsBankDetails")
    @Operation(summary = "Submit MLA file application", description = "Submit MLA file for mining lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitBankDetailsFile(
            @Valid @RequestBody MiningLeaseBankDetailsRequest request) {

        MiningLeaseResponse response = miningLeaseService.submitBankDetails(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    // ========== Resubmission ==========

    @PutMapping("/applications/resubmit")
    @Operation(summary = "Resubmit application", description = "Resubmit an application after providing additional data")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> resubmitApplication(
            @RequestBody FileUploadRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseService.resubmitApplication(request, userId);
        return ResponseEntity.ok(new SuccessResponse<>("Application resubmitted successfully", response));
    }

    @PostMapping("/deleteFileUpload")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> deleteFileUploadPFS(
            @RequestBody DeleteFileRequest request
    ){
        MiningLeaseResponse response = miningLeaseService.deleteFileUpload(request);
        return  ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("FILE SUCCESSFULLY DELETED",response));
    }

    @GetMapping("/geologistAssigned/{applicationNo}")
    @Operation(summary = "Get assigned geologist by number", description = "Get assigned geologist details by application number. Agency users can view any application, others can only view their own.")
    public ResponseEntity<SuccessResponse<TaskManagementAssignedUser>> getAssignedGeologist(
            @PathVariable String applicationNo) {

        TaskManagementAssignedUser response = miningLeaseService.getAssignedGeologist(applicationNo, userContext.isAgencyUser());

        return ResponseEntity.ok(new SuccessResponse<>("Assigned geologist retrieved successfully", response));
    }

}
