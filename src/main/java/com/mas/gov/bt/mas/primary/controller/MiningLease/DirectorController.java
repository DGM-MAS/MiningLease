package com.mas.gov.bt.mas.primary.controller.MiningLease;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.AssignTaskDirector;
import com.mas.gov.bt.mas.primary.dto.request.ReviewMiningLeaseApplicationDirector;
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
@RequestMapping("/api/mining-lease/director")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Mining Lease - Director", description = "Director Review APIs")
@SecurityRequirement(name = "bearerAuth")
public class DirectorController {

    private final UserContext userContext;
    private final MiningLeaseService miningLeaseService;


    @PostMapping("/review")
    @Operation(summary = "Review application", description = "Review mining lease application by Director")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> reviewApplication(
            @Valid @RequestBody ReviewMiningLeaseApplicationDirector request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseService.reviewApplicationDirector(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }

    @PostMapping("/assignTask")
    @Operation(summary = "Assign application", description = "Assign mining lease application by Director to MPCD and Mine Engineer")
    public ResponseEntity<SuccessResponse<MiningLeaseResponse>> assignApplication(
            @Valid @RequestBody AssignTaskDirector request) {

        Long userId = userContext.getCurrentUserId();
        MiningLeaseResponse response = miningLeaseService.assignApplicationDirector(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application assigned successfully", response));
    }

    // ** Dashboard information for Director mining lease application ** //
    // ** Assigned application to director ** //
    @GetMapping("/assigned")
    public ResponseEntity<SuccessResponse<List<MiningLeaseResponse>>> assignedToDirector(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(miningLeaseService.getAssignedToDirector(userId, pageable, search)
        );
    }
}
