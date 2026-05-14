package com.mas.gov.bt.mas.primary.controller.RenewalEnviromentalClearance;


import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.ApproveECRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.EnvironmentClearanceRenewalResponseDTO;
import com.mas.gov.bt.mas.primary.services.RenewalEnvironmentalClearanceService;
import com.mas.gov.bt.mas.primary.utility.PageRequest1Based;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/renewal-environmental-clearance/md")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Renewal environmental clearance MD", description = "Renewal environmental clearance Application Management APIs MD")
@SecurityRequirement(name = "bearerAuth")
public class RenewalEnvironmentalClearanceMDController {

    private final UserContext userContext;

    private final RenewalEnvironmentalClearanceService renewalEnvironmentalClearanceService;

    @GetMapping("/assigned")
    public ResponseEntity<SuccessResponse<List<EnvironmentClearanceRenewalResponseDTO>>> assignedToMD(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {

        Pageable pageable = PageRequest1Based.of(
                page,
                size,
                Sort.Direction.fromString(sortDirection),
                sortBy.trim()
        );

        Long userId = userContext.getCurrentUserId();

        return ResponseEntity.ok(
                renewalEnvironmentalClearanceService
                        .getAssignedToMD(userId, pageable, search)
        );
    }

    @PostMapping("/approve-ec")
    public ResponseEntity<SuccessResponse<EnvironmentClearanceRenewalResponseDTO>> approveEC(
            @Valid @RequestBody ApproveECRequestDTO request
    ) {

        Long userId = userContext.getCurrentUserId();

        EnvironmentClearanceRenewalResponseDTO response =
                renewalEnvironmentalClearanceService
                        .approveEC(request, userId);

        return ResponseEntity.ok(
                new SuccessResponse<>(
                        "EC approved successfully",
                        response
                )
        );
    }
}
