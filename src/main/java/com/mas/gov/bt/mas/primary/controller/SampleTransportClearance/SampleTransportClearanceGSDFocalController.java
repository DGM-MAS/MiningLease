package com.mas.gov.bt.mas.primary.controller.SampleTransportClearance;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.SampleTransportClearanceGSDFocalReviewRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.SampleTransportClearanceResponseDTO;
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
@RequestMapping("/api/sample-transport-clearance/gsd-focal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transport clearance GSD Focal ", description = "Sample Transport clearance Endpoints Are Placed Here for FOCAL GSD")
@SecurityRequirement(name = "bearerAuth")
public class SampleTransportClearanceGSDFocalController{

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
                sampleTransportClearanceService.getAssignedToGSDFocal(userId, pageable, search)
        );
    }

    @PostMapping("/review")
    @Operation(summary = "Review application", description = "Review mining lease application by Mining Engineer")
    public ResponseEntity<SuccessResponse<SampleTransportClearanceResponseDTO>> reviewApplication(
            @Valid @RequestBody SampleTransportClearanceGSDFocalReviewRequestDTO request) {

        Long userId = userContext.getCurrentUserId();
        SampleTransportClearanceResponseDTO response = sampleTransportClearanceService.reviewApplicationGSDFocal(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }

}
