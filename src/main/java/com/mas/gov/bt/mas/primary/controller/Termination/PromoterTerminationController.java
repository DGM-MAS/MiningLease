package com.mas.gov.bt.mas.primary.controller.Termination;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.ReviewTerminationApplicationCMSHead;
import com.mas.gov.bt.mas.primary.dto.response.TerminationApplicationResponse;
import com.mas.gov.bt.mas.primary.services.TerminationService;
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
 * REST Controller for Termination promoter Application management.
 */
@RestController
@RequestMapping("/api/termination/promoter")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Termination promoter User", description = "Termination promoter User Application Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class PromoterTerminationController {

    private final UserContext userContext;
    private final TerminationService terminationService;

    // ** Dashboard information for CMS HEAD Termination application ** //
    // ** Assigned application to CMS HEAD ** //
    @GetMapping("/assigned")
    public ResponseEntity<SuccessResponse<List<TerminationApplicationResponse>>> assignedToMI(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(terminationService.getAssignedToPromoter(userId, pageable, search)
        );
    }

    @PostMapping("/review")
    @Operation(summary = "Rectification application", description = "Rectification Termination application by promoter")
    public ResponseEntity<SuccessResponse<TerminationApplicationResponse>> reviewApplication(
            @Valid @RequestBody ReviewTerminationApplicationCMSHead request) {

        Long userId = userContext.getCurrentUserId();
        TerminationApplicationResponse response = terminationService.reviewApplicationPromoter(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }
}
