package com.mas.gov.bt.mas.primary.controller.ManualEntry;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.ReviewManualEntryRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.ManualMiningEntryResponseDTO;
import com.mas.gov.bt.mas.primary.services.ManualMiningEntryService;
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
@RequestMapping("/api/manual-mining/chief")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Manual Entry", description = "Manual Entry endpoints are placed here for chief")
@SecurityRequirement(name = "bearerAuth")
public class ManualMiningEntryChiefController {

    private final UserContext userContext;
    private final ManualMiningEntryService manualMiningEntryService;

    @GetMapping("/assigned")
    public ResponseEntity<SuccessResponse<List<ManualMiningEntryResponseDTO>>> assignedToGHDChief(
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
                manualMiningEntryService.getAssignedToChief(userId, pageable, search)
        );
    }

    @PostMapping("/review")
    @Operation(summary = "Review application", description = "Review manual entry application by chief")
    public ResponseEntity<SuccessResponse<ManualMiningEntryResponseDTO>> reviewApplication(
            @Valid @RequestBody ReviewManualEntryRequestDTO request) {

        Long userId = userContext.getCurrentUserId();
        ManualMiningEntryResponseDTO response = manualMiningEntryService.reviewApplicationChief(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }

}
