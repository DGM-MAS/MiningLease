package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.SampleTransportClearanceResponseDTO;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SampleTransportClearanceService {

    SampleTransportClearanceResponseDTO createApplication(
            SampleTransportClearanceDTO request, Long userId);

    Page<SampleTransportClearanceResponseDTO> getMyApplications(Long userId, Pageable pageable, String search);

    Page<SampleTransportClearanceResponseDTO> getMyArchivedApplications(Long userId, Pageable pageable, String search);

    SuccessResponse<List<SampleTransportClearanceResponseDTO>> getAllApplicationAdmin(Pageable pageable, String search);

    SuccessResponse<List<SampleTransportClearanceResponseDTO>> getAssignedToGSDChief(Long userId, Pageable pageable, String search);

    SampleTransportClearanceResponseDTO assignApplicationChief(@Valid AssignedTaskChiefDTO request, Long userId);

    SampleTransportClearanceResponseDTO reviewApplicationChief(@Valid ReviewSampleTransportClearanceRequestDTO request, Long userId);

    void reassignTaskGSDChief(@Valid ReassignTaskRequest request, Long userId);

    SuccessResponse<List<SampleTransportClearanceResponseDTO>> getAssignedToGSDFocal(Long userId, Pageable pageable, String search);

    SampleTransportClearanceResponseDTO reviewApplicationGSDFocal(@Valid SampleTransportClearanceGSDFocalReviewRequestDTO request, Long userId);
}