package com.mas.gov.bt.mas.primary.controller;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.services.MiningLeaseRenewalService;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mining-lease-renewal/mining-engineer")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mining Lease Renewal- Mining Engineer", description = "Mining Engineer renewal Review APIs")
@SecurityRequirement(name = "bearerAuth")
public class MiningEngineerRenewalController {

    private final UserContext userContext;
    private final MiningLeaseRenewalService miningLeaseRenewalService;

    @PostMapping("/review")
    @Operation(summary = "Review renewal application", description = "Review  renewal mining lease application by Mining Engineer")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> reviewApplication(
            @Valid @RequestBody ReviewMiningLeaseApplicationME request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseRenewalService.reviewApplicationME(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }

    // ** Dashboard information for MPCD quarry lease application ** //
    // ** Assigned application ** //
    // ** Assigned by director ** //
    @GetMapping("/assigned")
    public ResponseEntity<SuccessResponse<List<MiningLeaseResponse>>> assignedToMPCD(
            @RequestParam(required = false) String search,
            Pageable pageable) {

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(
                miningLeaseRenewalService.getAssignedToMineEngineer(userId, pageable, search)
        );
    }

    // Used by user to submit LLC
    @PostMapping("/applicationLLC")
    @Operation(summary = "Submit LLC file ", description = "Submit LLC file for quarry lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitFMFSFile(
            @Valid @RequestBody MiningLeaseLLCRequest request) {

        MiningLeaseResponse response = miningLeaseRenewalService.submitLLC(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("LLC submitted successfully", response));
    }

    // Used by user to submit LLC
//    @PostMapping("/applicationNoteSheet")
//    @Operation(summary = "Submit note sheet file ", description = "Submit Note Sheet file and any additional information for quarry lease application")
//    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitNoteSheetFile(
//            @Valid @RequestBody MiningLeaseNoteSheetRequest request) {
//
//        MiningLeaseResponse response = miningLeaseRenewalService.submitNoteSheetAndAdditionalDetails(request);
//
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(new SuccessResponse<>("LLC submitted successfully", response));
//    }

    // Used by user to submit work order
    @PostMapping("/applicationsWorkOrder")
    @Operation(summary = "Submit work order file application", description = "Submit work order file for quarry lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitWorkOrderFile(
            @Valid @RequestBody MiningLeaseWorkOrderRequest request) {

        MiningLeaseResponse response = miningLeaseRenewalService.submitWorkOrder(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    // Used by user to submit FMFS
    @PostMapping("/applicationsMLA")
    @Operation(summary = "Submit MLA file application", description = "Submit MLA file for quarry lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitMLAFile(
            @Valid @RequestBody MiningLeaseMLARequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseRenewalService.submitMLA(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    @PostMapping("/notesheet")
    @Operation(summary = "Upload signed notesheet", description = "Upload signed notesheet for mining lease renewal application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> uploadNoteSheet(
            @Valid @RequestBody MiningLeaseNoteSheetRequest request) {

        MiningLeaseResponse response = miningLeaseRenewalService.submitNoteSheet(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Notesheet uploaded successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application by ID", description = "Get renewal application details by ID")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> getApplicationById(@PathVariable Long id) {
        return ResponseEntity.ok(new SuccessResponse<>("Application fetched successfully",
                miningLeaseRenewalService.getApplicationById(id)));
    }
}
