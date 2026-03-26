package com.mas.gov.bt.mas.primary.controller.MiningLease;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.MiningLeaseMARequest;
import com.mas.gov.bt.mas.primary.dto.request.ReviewMiningLeaseApplicationMPCD;
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
@RequestMapping("/api/mining-lease/mpcd")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mining Lease MPCD", description = "Mining Lease Application Management APIs MPCD")
@SecurityRequirement(name = "bearerAuth")
public class MPCDFocalController {

    private final MiningLeaseService miningLeaseService;
    private final UserContext userContext;

    // ** Review quarry lease application by MPCD ** //
    // ** MPCD will review the application once the director has assigned the application ** //
    @PostMapping("/review")
    @Operation(summary = "Review new application", description = "Review new quarry lease application by MPCD")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> createApplication(
            @Valid @RequestBody ReviewMiningLeaseApplicationMPCD reviewQuarryLeaseApplication) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseService.reviewApplicationMPCD(reviewQuarryLeaseApplication, userId);

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
                miningLeaseService.getAssignedToMPCD(userId, pageable, search)
        );
    }

    // Used by user to submit MA
    @PostMapping("/applicationsMA")
    @Operation(summary = "Submit MA file application", description = "Submit MA file for quarry lease application")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> submitFMFSFile(
            @Valid @RequestBody MiningLeaseMARequest request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseService.submitMA(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    // ** Dashboard information for MPCD quarry lease application ** //
    // ** End of process flow ACCEPTED application only ** //
    @GetMapping("/archivedMPCD")
    public ResponseEntity<SuccessResponse<List<MiningLeaseResponse>>> archivedMPCD(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(
                miningLeaseService.getArchivedApplicationToMPCD(userId, pageable, search)
        );
    }


}
