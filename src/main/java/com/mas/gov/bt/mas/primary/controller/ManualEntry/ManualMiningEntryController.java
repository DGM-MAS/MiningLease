package com.mas.gov.bt.mas.primary.controller.ManualEntry;

import com.mas.gov.bt.mas.primary.config.UserContext;
import com.mas.gov.bt.mas.primary.dto.request.ManualMiningEntryRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.ManualMiningEntryResponseDTO;
import com.mas.gov.bt.mas.primary.services.ManualMiningEntryService;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manual-mining")
@RequiredArgsConstructor
public class ManualMiningEntryController {

    private final ManualMiningEntryService service;
    private final UserContext userContext;

    @PostMapping("/applications")
    @Operation(
            summary = "Create manual mining entry",
            description = "Save manual mining / quarry / SC / stock lifting entry with attachments"
    )
    public ResponseEntity<SuccessResponse<ManualMiningEntryResponseDTO>> createApplication(
            @Valid @RequestBody ManualMiningEntryRequestDTO request) {

        Long userId = userContext.getCurrentUserId();

        ManualMiningEntryResponseDTO response =
                service.createApplication(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SuccessResponse<>("Application created successfully", response));
    }


}