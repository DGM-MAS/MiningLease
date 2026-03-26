package com.mas.gov.bt.mas.primary.controller.MiningLease;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.services.MiningLeaseService;
import com.mas.gov.bt.mas.primary.utility.PageRequest1Based;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/mining-lease/mining-engineer")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mining Lease - Mining Engineer", description = "Mining Engineer Review APIs")
@SecurityRequirement(name = "bearerAuth")
public class MiningEngineerController {

    private final MiningLeaseService miningLeaseService;
    private final UserContext userContext;

    @PostMapping("/review")
    @Operation(summary = "Review application", description = "Review mining lease application by Mining Engineer")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> reviewApplication(
            @Valid @RequestBody ReviewMiningLeaseApplicationME request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseService.reviewApplicationME(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }

    // ** Dashboard information for MPCD quarry lease application ** //
    // ** Assigned application ** //
    // ** Assigned by director ** //
    @GetMapping("/assigned")
    public ResponseEntity<SuccessResponse<List<MiningLeaseResponse>>> assignedToMPCD(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(
                miningLeaseService.getAssignedToMineEngineer(userId, pageable, search)
        );
    }

    // Used by user to submit LLC
    @PostMapping("/applicationLLC")
    @Operation(summary = "Submit LLC file ", description = "Submit LLC file for quarry lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitFMFSFile(
            @Valid @RequestBody MiningLeaseLLCRequest request) {

        MiningLeaseResponse response = miningLeaseService.submitLLC(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("LLC submitted successfully", response));
    }

    // Used by user to submit LLC
    @PostMapping("/applicationNoteSheet")
    @Operation(summary = "Submit note sheet file ", description = "Submit Note Sheet file and any additional information for quarry lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitNoteSheetFile(
            @Valid @RequestBody MiningLeaseNoteSheetRequest request) {

        MiningLeaseResponse response = miningLeaseService.submitNoteSheetAndAdditionalDetails(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("LLC submitted successfully", response));
    }

    // Used by user to submit work order
    @PostMapping("/applicationsWorkOrder")
    @Operation(summary = "Submit work order file application", description = "Submit work order file for quarry lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitWorkOrderFile(
            @Valid @RequestBody MiningLeaseWorkOrderRequest request) {

        MiningLeaseResponse response = miningLeaseService.submitWorkOrder(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    // Used by user to submit FMFS
    @PostMapping("/applicationsMLA")
    @Operation(summary = "Submit MLA file application", description = "Submit MLA file for quarry lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitMLAFile(
            @Valid @RequestBody MiningLeaseMLARequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseService.submitMLA(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }


}
