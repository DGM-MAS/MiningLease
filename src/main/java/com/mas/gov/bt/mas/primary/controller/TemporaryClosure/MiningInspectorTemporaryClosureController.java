package com.mas.gov.bt.mas.primary.controller.TemporaryClosure;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.ReviewTemporaryClosureMIRequest;
import com.mas.gov.bt.mas.primary.dto.response.TemporaryClosureNotificationResponse;
import com.mas.gov.bt.mas.primary.services.TemporaryClosureService;
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
 * REST Controller for Temporary closure management.
 */
@RestController
@RequestMapping("/api/temporary-closure/mi")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Temporary closure Mining Inspector", description = "Temporary closure applicant endpoints are placed here for Mining Inspector")
@SecurityRequirement(name = "bearerAuth")
public class MiningInspectorTemporaryClosureController {

    private final UserContext userContext;
    private final TemporaryClosureService temporaryClosureService;

    @PostMapping("/review")
    @Operation(summary = "Review application", description = "Review mining lease application by Director")
    public ResponseEntity<SuccessResponse<TemporaryClosureNotificationResponse>> reviewApplication(
            @Valid @RequestBody ReviewTemporaryClosureMIRequest request) {

        Long userId = userContext.getCurrentUserId();
        TemporaryClosureNotificationResponse response = temporaryClosureService.reviewApplicationMI(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }


    // ** Dashboard information for mining inspector temporary closure application ** //
    // ** Assigned application to Mining Inspector ** //
    @GetMapping("/assigned")
    public ResponseEntity<SuccessResponse<List<TemporaryClosureNotificationResponse>>> assignedToDirector(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(temporaryClosureService.getAssignedToMI(userId, pageable, search)
        );
    }

}
