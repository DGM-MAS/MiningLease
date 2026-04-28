package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.request.ManualMiningEntryRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.ReviewManualEntryRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.ManualMiningEntryResponseDTO;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ManualMiningEntryService {
    ManualMiningEntryResponseDTO createApplication(
            ManualMiningEntryRequestDTO request,
            Long userId);

    SuccessResponse<List<ManualMiningEntryResponseDTO>> getAssignedToChief(Long userId, Pageable pageable, String search);

    ManualMiningEntryResponseDTO reviewApplicationChief(@Valid ReviewManualEntryRequestDTO request, Long userId);

    SuccessResponse<List<ManualMiningEntryResponseDTO>> getAssignedToDirector(Long userId, Pageable pageable, String search);

    ManualMiningEntryResponseDTO reviewApplicationDirector(@Valid ReviewManualEntryRequestDTO request, Long userId);
}
