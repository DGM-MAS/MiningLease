package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.ApplicationListResponse;
import com.mas.gov.bt.mas.primary.dto.response.EnvironmentClearanceRenewalResponseDTO;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RenewalEnvironmentalClearanceService {
    EnvironmentClearanceRenewalResponseDTO saveDraft(@Valid EnvironmentClearanceRenewalRequestDTO request, Long userId);

    EnvironmentClearanceRenewalResponseDTO submitApplication(@Valid EnvironmentClearanceRenewalRequestDTO request, Long userId);

    EnvironmentClearanceRenewalResponseDTO reviewApplicationMPCD(
            ReviewEnvironmentClearanceMPCDRequest request,
            Long userId
    );

    SuccessResponse<List<EnvironmentClearanceRenewalResponseDTO>> getAssignedToMPCD(
            Long userId,
            Pageable pageable,
            String search
    );

    void reassignTaskMPCD(
            ReassignTaskRequest request,
            Long userId
    );

    Page<EnvironmentClearanceRenewalResponseDTO> getMyApplications(Long userId, Pageable pageable, String search);

    EnvironmentClearanceRenewalResponseDTO getApplicationById(Long id, Long userId);

    Page<EnvironmentClearanceRenewalResponseDTO> getArchivedApplications(Pageable pageable, String search, Long userId);

    Page<EnvironmentClearanceRenewalResponseDTO> getMyArchivedApplications(Long userId, Pageable pageable, String search);

    EnvironmentClearanceRenewalResponseDTO updateDraft(Long id, @Valid EnvironmentClearanceRenewalRequestDTO request, Long userId);

    void deleteDraft(Long id, Long userId);

    EnvironmentClearanceRenewalResponseDTO resubmitApplication(Long id, @Valid EnvironmentClearanceRenewalRequestDTO request, Long userId);

    Page<EnvironmentClearanceRenewalResponseDTO> getArchivedApplicationsMPCD(Long userId, Pageable pageable, String search);

    EnvironmentClearanceRenewalResponseDTO requestResubmission(@Valid RequestResubmissionDTO request, Long userId);

    EnvironmentClearanceRenewalResponseDTO rejectApplicationMPCD(@Valid RejectApplicationDTO request, Long userId);

    EnvironmentClearanceRenewalResponseDTO assignRC(@Valid AssignRCRequestDTO request, Long userId);

    SuccessResponse<List<EnvironmentClearanceRenewalResponseDTO>> getAssignedToRC(Long userId, Pageable pageable, String search);

    EnvironmentClearanceRenewalResponseDTO submitRCReport(@Valid SubmitRCReportDTO request, Long userId);

    EnvironmentClearanceRenewalResponseDTO assignMI(@Valid AssignMIRequestDTO request, Long userId);

    SuccessResponse<List<EnvironmentClearanceRenewalResponseDTO>> getAssignedToMI(Long userId, Pageable pageable, String search);

    EnvironmentClearanceRenewalResponseDTO submitMIReport(@Valid SubmitMIReportDTO request, Long userId);

    SuccessResponse<List<EnvironmentClearanceRenewalResponseDTO>> getAssignedToMD(Long userId, Pageable pageable, String search);

    EnvironmentClearanceRenewalResponseDTO approveEC(@Valid ApproveECRequestDTO request, Long userId);
}
