package com.mas.gov.bt.mas.primary.controller.RenewalMiningLease;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.ApplicationListResponse;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseRenewalApplicationResponse;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.services.MiningLeaseRenewalService;
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

@RestController
@RequestMapping("/api/mining-lease/renewal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mining Lease Renewal", description = "Renewal Review APIs")
@SecurityRequirement(name = "bearerAuth")
public class MiningLeaseRenewalController {

    private final UserContext userContext;
    private final MiningLeaseRenewalService miningLeaseRenewalService;

    // 1. First step where data from mining lease application is retrieved
    // Get all approved mining lease application for particular applicant id
    // Pagination added start from 1
    @GetMapping("/application")
    @Operation(summary = "Get application by number", description = "Get application details by application number. Agency users can view any application, others can only view their own.")
    public ResponseEntity<SuccessResponse<List<ApplicationListResponse>>> getApplicationByNumber(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Page<ApplicationListResponse> applications;

        Long userId = userContext.getCurrentUserId();

        applications = miningLeaseRenewalService.getApplicationByApplicantId(userId , pageable, search);

        return ResponseEntity.ok(SuccessResponse.fromPage("Applications retrieved successfully", applications));
    }

    // 2.
    // Submit application for mining lease renewal
    @PostMapping("/applications")
    @Operation(summary = "Create renewal application", description = "Create a renewal mining lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> createApplication(
            @Valid @RequestBody RenewalMiningLeaseRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseRenewalService.createRenewalApplication(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }


    // ** Dashboard information for mining lease renewal application ** //
    // ** End of process flow MINING LEASE APPROVED application only ** //
    @GetMapping("/archived")
    public ResponseEntity<SuccessResponse<List<MiningLeaseResponse>>> archived(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
            ) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(
                miningLeaseRenewalService.getArchivedApplication(userId, pageable, search)
        );
    }


    // Used by user to submit FMFS
    // Used for submitting FMFS by client or promoter
    @PostMapping("/applicationsFMFS")
    @Operation(summary = "Submit FMFS file application", description = "Submit FMFS file for mining lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitFMFSFile(
            @Valid @RequestBody MiningLeaseFMFSRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseRenewalService.submitFMFS(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    // Used to save application in draft
    // These application will not be saved in task management or application master
    @PostMapping("/draft")
    @Operation(summary = "Save renewal application as draft", description = "Save renewal mining lease application as draft")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> saveDraft(
            @Valid @RequestBody RenewalMiningLeaseRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseRenewalService.saveDraft(request, userId);

        return ResponseEntity.ok(new SuccessResponse<>("Draft saved successfully", response));
    }

    // Resubmit
    @PostMapping("/resubmit")
    @Operation(summary = "Resubmit renewal application", description = "Resubmit renewal application after ME sends it back for revision")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> resubmitApplication(
            @Valid @RequestBody RenewalApplicationResubmitRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseRenewalService.resubmitApplication(request, userId);

        return ResponseEntity.ok(new SuccessResponse<>("Application resubmitted successfully", response));
    }

    @PostMapping("/resubmitFMFS")
    @Operation(summary = "Resubmit FMFS", description = "Resubmit FMFS after ME sends it back for revision")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> resubmitFMFS(
            @Valid @RequestBody MiningLeaseFMFSRequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseRenewalService.resubmitFMFS(request, userId);

        return ResponseEntity.ok(new SuccessResponse<>("FMFS resubmitted successfully", response));
    }

    @PostMapping("/signMLA")
    @Operation(summary = "Applicant sign MLA", description = "Applicant digitally signs the Mining Lease Agreement")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> signMLA(
            @Valid @RequestBody MiningLeaseMLARequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseRenewalService.applicantSignMLA(request, userId);

        return ResponseEntity.ok(new SuccessResponse<>("MLA signed successfully", response));
    }

    @GetMapping("/my-applications")
    @Operation(summary = "Get my renewal applications", description = "Get paginated list of applicant's own renewal applications")
    public ResponseEntity<SuccessResponse<List<MiningLeaseResponse>>> getMyApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdBy") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(miningLeaseRenewalService.getMyApplications(userId, pageable, search));
    }

    @GetMapping("/{applicationNumber}")
    @Operation(summary = "Get renewal application by application number", description = "Returns all fields of the renewal application")
    public ResponseEntity<SuccessResponse<MiningLeaseRenewalApplicationResponse>> getByApplicationNumber(
            @PathVariable String applicationNumber) {

        MiningLeaseRenewalApplicationResponse response = miningLeaseRenewalService.getByApplicationNumber(applicationNumber);
        return ResponseEntity.ok(new SuccessResponse<>("Application retrieved successfully", response));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all renewal applications", description = "Get paginated list of all renewal applications (admin view)")
    public ResponseEntity<SuccessResponse<List<MiningLeaseResponse>>> getAllApplications(
            @RequestParam(required = false) String search,
            Pageable pageable) {

        return ResponseEntity.ok(miningLeaseRenewalService.getAllApplications(pageable, search));
    }

    @PostMapping("/deleteFileUpload")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> deleteFileUploadFMFS(
            @RequestBody DeleteFileRequest request
    ){
        MiningLeaseResponse response = miningLeaseRenewalService.deleteFileUpload(request);
        return  ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("FILE SUCCESSFULLY DELETED",response));
    }

    // ========== Task Reassignment ==========

    @PutMapping("/tasks/reassignGeologist")
    @Operation(summary = "Reassign task", description = "Reassign a pending task to another user")
    public ResponseEntity<SuccessResponse<Void>> reassignTaskGeologist(
            @Valid @RequestBody ReassignTaskRequest request) {

        Long userId = userContext.getCurrentUserId();
        miningLeaseRenewalService.reassignTaskGeologist(request, userId);

        return SuccessResponse.buildSuccessResponse("Task reassigned successfully");
    }

    @PutMapping("/tasks/reassignME")
    @Operation(summary = "Reassign task", description = "Reassign a pending task to another user")
    public ResponseEntity<SuccessResponse<Void>> reassignTaskME(
            @Valid @RequestBody ReassignTaskRequest request) {

        Long userId = userContext.getCurrentUserId();
        miningLeaseRenewalService.reassignTaskME(request, userId);

        return SuccessResponse.buildSuccessResponse("Task reassigned successfully");
    }

    @PutMapping("/tasks/reassignMineChief")
    @Operation(summary = "Reassign task", description = "Reassign a pending task to another user")
    public ResponseEntity<SuccessResponse<Void>> reassignTaskMineChief(
            @Valid @RequestBody ReassignTaskRequest request) {

        Long userId = userContext.getCurrentUserId();
        miningLeaseRenewalService.reassignTaskMineChief(request, userId);

        return SuccessResponse.buildSuccessResponse("Task reassigned successfully");
    }
}
