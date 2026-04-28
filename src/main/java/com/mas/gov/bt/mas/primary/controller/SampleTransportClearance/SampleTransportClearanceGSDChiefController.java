package com.mas.gov.bt.mas.primary.controller.SampleTransportClearance;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.AssignedTaskChiefDTO;
import com.mas.gov.bt.mas.primary.dto.request.ReassignTaskRequest;
import com.mas.gov.bt.mas.primary.dto.request.ReviewSampleTransportClearanceRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.SampleTransportClearanceResponseDTO;
import com.mas.gov.bt.mas.primary.repository.SampleTransportClearanceRepository;
import com.mas.gov.bt.mas.primary.services.SampleTransportClearanceService;
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

/**
 * REST Controller for Transport Clearance management.
 */
@RestController
@RequestMapping("/api/sample-transport-clearance/gsd-chief")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transport clearance GSD Chief ", description = "Sample Transport clearance Endpoints Are Placed Here for CHIEF GSD")
@SecurityRequirement(name = "bearerAuth")
public class SampleTransportClearanceGSDChiefController {

    private final UserContext userContext;
    private final SampleTransportClearanceService sampleTransportClearanceService;

    @GetMapping("/assigned")
    public ResponseEntity<SuccessResponse<List<SampleTransportClearanceResponseDTO>>> assignedToGSDChief(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(
                sampleTransportClearanceService.getAssignedToGSDChief(userId, pageable, search)
        );
    }

    @PostMapping("/assignTask")
    @Operation(summary = "Assign application", description = "Assign geo physics application by chief to GHD Focal")
    public ResponseEntity<SuccessResponse<SampleTransportClearanceResponseDTO>> assignApplication(
            @Valid @RequestBody AssignedTaskChiefDTO request) {

        Long userId = userContext.getCurrentUserId();
        SampleTransportClearanceResponseDTO response = sampleTransportClearanceService.assignApplicationChief(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application assigned successfully", response));
    }

    @PostMapping("/review")
    @Operation(summary = "Review application", description = "Review geo physics application by chief")
    public ResponseEntity<SuccessResponse<SampleTransportClearanceResponseDTO>> reviewApplication(
            @Valid @RequestBody ReviewSampleTransportClearanceRequestDTO request) {

        Long userId = userContext.getCurrentUserId();
        SampleTransportClearanceResponseDTO response = sampleTransportClearanceService.reviewApplicationChief(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }

    // ========== Task Reassignment ==========
    @PutMapping("/tasks/reassignGHDChief")
    @Operation(summary = "Reassign task", description = "Reassign a pending task to another user")
    public ResponseEntity<SuccessResponse<Void>> reassignTaskMineChief(
            @Valid @RequestBody ReassignTaskRequest request) {

        Long userId = userContext.getCurrentUserId();
        sampleTransportClearanceService.reassignTaskGSDChief(request, userId);

        return SuccessResponse.buildSuccessResponse("Task reassigned successfully");
    }
}
