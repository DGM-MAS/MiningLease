package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.request.ManualMiningEntryRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.ManualMiningEntryResponseDTO;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ManualMiningEntryService {

    ManualMiningEntryResponseDTO createApplication(@Valid ManualMiningEntryRequestDTO request, Long userId);

    SuccessResponse<List<ManualMiningEntryResponseDTO>> getApplications(Long userId, Pageable pageable, String search);

    SuccessResponse<List<ManualMiningEntryResponseDTO>> getAllApplications(Pageable pageable, String search);

    ManualMiningEntryResponseDTO getApplicationByNo(String applicationNo);
}
