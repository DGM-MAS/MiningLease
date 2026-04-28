package com.mas.gov.bt.mas.primary.controller.SampleTransportClearance;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.SampleTransportClearanceDTO;
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
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/sample-transport-clearance")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transport clearance ", description = "Sample Transport clearance Endpoints Are Placed Here")
@SecurityRequirement(name = "bearerAuth")
public class SampleTransportClearanceController {

    private final UserContext userContext;
    private final SampleTransportClearanceService sampleTransportClearanceService;

    @PostMapping("/applications")
    @Operation(
            summary = "Create sample transport clearance application",
            description = "Create a new sample transport clearance application"
    )
    public ResponseEntity<SuccessResponse<SampleTransportClearanceResponseDTO>> createApplication(
            @Valid @RequestBody SampleTransportClearanceDTO request) {

        Long userId = userContext.getCurrentUserId();

        SampleTransportClearanceResponseDTO response =
                sampleTransportClearanceService.createApplication(request,userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }

    @GetMapping("/my-applications")
    @Operation(summary = "Get my applications", description = "Get list of applications. Agency users get all applications, others get only their own.")
    public ResponseEntity<SuccessResponse<List<SampleTransportClearanceResponseDTO>>> getMyApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Page<SampleTransportClearanceResponseDTO> applications;

        // Regular users can only see their own applications
        Long userId = userContext.getCurrentUserId();
        applications = sampleTransportClearanceService.getMyApplications(userId, pageable, search);

        return ResponseEntity.ok(SuccessResponse.fromPage("Applications retrieved successfully", applications));
    }

    @GetMapping("/archived-applications")
    @Operation(summary = "Get archived applications", description = "Get list of archived (APPROVED/REJECTED) applications.")
    public ResponseEntity<SuccessResponse<List<SampleTransportClearanceResponseDTO>>> getArchivedApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Page<SampleTransportClearanceResponseDTO> applications;

        // Regular users can only see their own archived applications
        Long userId = userContext.getCurrentUserId();
        applications = sampleTransportClearanceService.getMyArchivedApplications(userId, pageable, search);


        return ResponseEntity.ok(SuccessResponse.fromPage("Archived applications retrieved successfully", applications));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all sample transport applications", description = "Get paginated list of all transport clearance (admin view)")
    public ResponseEntity<SuccessResponse<List<SampleTransportClearanceResponseDTO>>> getAllApplications(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        return ResponseEntity.ok(sampleTransportClearanceService.getAllApplicationAdmin(pageable, search));
    }



}
