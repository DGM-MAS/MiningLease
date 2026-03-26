package com.mas.gov.bt.mas.primary.controller.ImmediateSuspension;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.PromoterImmediateSuspensionRequest;
import com.mas.gov.bt.mas.primary.dto.request.ReviewTerminationApplicationCMSHead;
import com.mas.gov.bt.mas.primary.dto.response.ImmediateSuspensionApplicationResponse;
import com.mas.gov.bt.mas.primary.dto.response.TerminationApplicationResponse;
import com.mas.gov.bt.mas.primary.services.ImmediateSuspensionService;
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
@RequestMapping("/api/immediate-suspension/promoter")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Immediate suspension promoter User", description = "Immediate promoter User Application Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class PromoterImmediateSuspensionController {

    private final UserContext userContext;
    private final ImmediateSuspensionService immediateSuspensionService;

    // ** Dashboard information for promoter immediate suspension application ** //
    // ** Assigned application to promoter ** //
    @GetMapping("/assigned")
    public ResponseEntity<SuccessResponse<List<ImmediateSuspensionApplicationResponse>>> assignedToMI(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Pageable pageable = PageRequest1Based.of(page, size,
                Sort.Direction.fromString(sortDirection), sortBy);

        Long userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(immediateSuspensionService.getAssignedToPromoter(userId, pageable, search)
        );
    }

    @PostMapping("/review")
    @Operation(summary = "Rectification application", description = "Rectification Termination application by promoter")
    public ResponseEntity<SuccessResponse<ImmediateSuspensionApplicationResponse>> reviewApplication(
            @Valid @RequestBody PromoterImmediateSuspensionRequest request) {

        Long userId = userContext.getCurrentUserId();
        ImmediateSuspensionApplicationResponse response = immediateSuspensionService.reviewApplicationPromoter(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application reviewed successfully", response));
    }


}
