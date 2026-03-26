package com.mas.gov.bt.mas.primary.controller.TemporaryClosure;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.AssignedTaskRC;
import com.mas.gov.bt.mas.primary.dto.request.ReviewTemporaryClosureRCRequest;
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
 * REST Controller for Temporary Closure Application management.
 */
@RestController
@RequestMapping("/api/temporary-closure/rc")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RC Temporary Closure", description = "RC Temporary closure Application Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class RCTemporaryClosureController {

    private final UserContext userContext;
    private final TemporaryClosureService temporaryClosureService;

    // ** Dashboard information for RC temporary closure application ** //
    // ** Assigned application to RC ** //
    @GetMapping("/assigned")
    public ResponseEntity<SuccessResponse<List<TemporaryClosureNotificationResponse>>> assignedToRC(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(temporaryClosureService.getAssignedToRC(userId, pageable, search)
        );
    }

    @PostMapping("/assignTask")
    @Operation(summary = "Assign application", description = "Assign mining lease application by Director to MPCD and Mine Engineer")
    public ResponseEntity<SuccessResponse<TemporaryClosureNotificationResponse>> assignApplication(
            @Valid @RequestBody AssignedTaskRC request) {

        Long userId = userContext.getCurrentUserId();
        TemporaryClosureNotificationResponse response = temporaryClosureService.assignApplicationDirector(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application assigned successfully", response));
    }

    @PostMapping("/review")
    @Operation(summary = "Review application", description = "Review temporary closure application by RC")
    public ResponseEntity<SuccessResponse<TemporaryClosureNotificationResponse>> reviewApplication(
            @Valid @RequestBody ReviewTemporaryClosureRCRequest request) {

        Long userId = userContext.getCurrentUserId();
        TemporaryClosureNotificationResponse response = temporaryClosureService.reviewApplicationRC(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }


}
