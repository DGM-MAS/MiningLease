package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.utility.CustomRuntimeException;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.EnvironmentClearanceRenewalResponseDTO;
import com.mas.gov.bt.mas.primary.entity.ApplicationMaster;
import com.mas.gov.bt.mas.primary.entity.EnvironmentClearanceRenewal;
import com.mas.gov.bt.mas.primary.entity.TaskManagement;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.mapper.EnvironmentClearanceRenewalMapper;
import com.mas.gov.bt.mas.primary.repository.ApplicationMasterRepository;
import com.mas.gov.bt.mas.primary.repository.RenewalEnvironmentalClearanceRepository;
import com.mas.gov.bt.mas.primary.repository.TaskManagementRepository;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class RenewalEnvironmentalClearanceServiceImpl implements RenewalEnvironmentalClearanceService {

    private static final String SERVICE_CODE = "RENEWAL_ENV_CLEARANCE";

    private final EnvironmentClearanceRenewalMapper environmentClearanceRenewalMapper;

    private final RenewalEnvironmentalClearanceRepository renewalEnvironmentalClearanceRepository;

    private final TaskManagementRepository taskManagementRepository;

    private final ApplicationMasterRepository applicationMasterRepository;

    private final NotificationClient notificationClient;

    @Override
    public EnvironmentClearanceRenewalResponseDTO saveDraft(
            EnvironmentClearanceRenewalRequestDTO request,
            Long userId)
    {
        EnvironmentClearanceRenewal entity =
                environmentClearanceRenewalMapper.toEntity(request);

        entity.setStatus("DRAFT");
        entity.setCreatedBy(userId);

        EnvironmentClearanceRenewal saved =
                renewalEnvironmentalClearanceRepository.save(entity);

        return environmentClearanceRenewalMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public EnvironmentClearanceRenewalResponseDTO submitApplication(EnvironmentClearanceRenewalRequestDTO request, Long userId) {
        validateSubmission(request);

        EnvironmentClearanceRenewal entity =
                environmentClearanceRenewalMapper.toEntity(request);

        entity.setApplicationNo(generateApplicationNumber());
        entity.setCreatedBy(userId);
        entity.setSubmittedOn(LocalDateTime.now());

        routeApplication(entity);

        ApplicationMaster master =
                createApplicationMaster(entity.getApplicationNo(), userId, entity.getStatus() );

        entity.setApplicationMaster(master);

        if (entity.getServiceType().equals("MINING LEASE") || entity.getServiceType().equals("QUARRY LEASE")) {
            UserWorkloadProjection assignedMDEngineer = assignMD();
            if (assignedMDEngineer != null) {
                createTask(master, entity, "MINING ENGINEER", userId, assignedMDEngineer.getUserId());
                entity.setAssignedMDId(assignedMDEngineer.getUserId());

                notificationClient.sendEnvironmentalClearanceAssignmentNotification(
                        assignedMDEngineer.getEmail(),
                        assignedMDEngineer.getUsername(),
                        entity.getApplicationNo(),
                        "REVIEW APPLICATION");

                String title = "Renewal environmental clearance application has been assigned.";
                String message = "An application for environmental clearance lease has been assigned for review. Application No. "+ entity.getApplicationNo()+" Please login in review the Geological report.";
                String serviceId = "78";
                notificationClient.sendUserNotification(title, message, assignedMDEngineer.getUserId(), serviceId);
            }

        }else {
            UserWorkloadProjection assignedMPCD = assignMPCD();

            if(assignedMPCD != null) {
                createTask(master, entity, "MPCD", userId, assignedMPCD.getUserId());
                entity.setAssignedMPCDId(assignedMPCD.getUserId());

                notificationClient.sendEnvironmentalClearanceAssignmentNotification(
                        assignedMPCD.getEmail(),
                        assignedMPCD.getUsername(),
                        entity.getApplicationNo(),
                        "REVIEW APPLICATION");

                String title = "Renewal environmental clearance application has been assigned.";
                String message = "An application for environmental clearance lease has been assigned for review. Application No. "+ entity.getApplicationNo()+" Please login in review the Geological report.";
                String serviceId = "78";
                notificationClient.sendUserNotification(title, message, assignedMPCD.getUserId(), serviceId);
            }

        }

        EnvironmentClearanceRenewal saved =
                renewalEnvironmentalClearanceRepository.save(entity);

        return environmentClearanceRenewalMapper.toResponseDTO(saved);
    }

    private ApplicationMaster createApplicationMaster(String applicationNumber, Long userId, String status) {
        ApplicationMaster master = new ApplicationMaster();
        master.setApplicationNumber(applicationNumber);
        master.setServiceCode(SERVICE_CODE);
        master.setApplicantUserId(userId);
        master.setCurrentStatus(status);
        master.setSubmittedOn(LocalDateTime.now());
        return applicationMasterRepository.save(master);
    }

    private void validateSubmission(
            EnvironmentClearanceRenewalRequestDTO request
    ) {

        if (request.getServiceType() == null) {
            throw new IllegalArgumentException(
                    "Service type is required"
            );
        }

        if (request.getLocation() == null ||
                request.getLocation().isBlank()) {
            throw new IllegalArgumentException(
                    "Location is required"
            );
        }

        if (request.getArea() == null ||
                request.getArea().isBlank()) {
            throw new IllegalArgumentException(
                    "Area is required"
            );
        }

        if (request.getPreviousEcFileId() == null) {
            throw new IllegalArgumentException(
                    "Previous EC file is required"
            );
        }

        if (request.getSelfMonitoringReportFileId() == null) {
            throw new IllegalArgumentException(
                    "Self monitoring report is required"
            );
        }
    }

    private void routeApplication(
            EnvironmentClearanceRenewal entity
    ) {

        if (Objects.equals(entity.getServiceType(), "MINING LEASE") || Objects.equals(entity.getServiceType(), "QUARRY LEASE")) {

            entity.setStatus(
                    "ASSIGNED TO MD"
            );

        } else {

            entity.setStatus(
                    "ASSIGNED TO MPCD"
            );
        }
    }

    private synchronized String generateApplicationNumber() {

        int year = Year.now().getValue();

        String prefix = String.format("ECR-%d-", year);

        // Query t_application_master — it is always written first,
        // so it is the authoritative source of which numbers are taken.
        // Querying the service table would miss numbers from failed
        // transactions where ApplicationMaster committed but the entity did not.
        Integer maxSequence =
                applicationMasterRepository
                        .findMaxSequenceByApplicationNumberPrefix(prefix);

        long nextSequence =
                (maxSequence == null ? 0 : maxSequence) + 1;

        return String.format(
                "ECR-%d-%06d",
                year,
                nextSequence
        );
    }

    private UserWorkloadProjection assignMD() {

        UserWorkloadProjection md =
                renewalEnvironmentalClearanceRepository.findMDEnvironmentalClearance();

        if (md == null) {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND,
                    "No active MD engineer available for assignment. Please ensure an MD engineer is registered in the system.");
        }
        return md;
    }

    private UserWorkloadProjection assignMPCD() {

        UserWorkloadProjection mpcd =
                renewalEnvironmentalClearanceRepository.findMPCDEnvironmentalClearance();

        if (mpcd == null) {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND,
                    "No active MPCD officer available for assignment. Please ensure an MPCD officer is registered in the system.");
        }
        return mpcd;
    }

    private void createTask(ApplicationMaster master, EnvironmentClearanceRenewal application, String role, Long userId, Long directorId) {
        LocalDateTime now = LocalDateTime.now();

        TaskManagement task = new TaskManagement();
        task.setApplicationNumber(application.getApplicationNo());
        task.setServiceCode(SERVICE_CODE);
        task.setAssignedToRole(role);
        task.setAssignedByUserId(userId);
        task.setAssignedToUserId(directorId);
        task.setAssignedAt(now);
        task.setTaskStatus(master.getCurrentStatus());

        task.setCreatedBy(userId);

        taskManagementRepository.save(task);
        log.info("Created task for role {}", role);
    }

    @Override
    public EnvironmentClearanceRenewalResponseDTO reviewApplicationMPCD(
            ReviewEnvironmentClearanceMPCDRequest request,
            Long userId
    ) {

        EnvironmentClearanceRenewal entity =
                renewalEnvironmentalClearanceRepository
                        .findById(request.getRenewalId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Application not found"
                                ));

        ApplicationMaster applicationMaster = entity.getApplicationMaster();

        validateMPCDAssignment(entity, userId);

        if (request.getMpcdSiteReportFileId() != null) {
            entity.setMpcdSiteReportFileId(
                    request.getMpcdSiteReportFileId()
            );
            entity.setMpcdReportSubmittedOn(
                    LocalDateTime.now()
            );
        }

        if (request.getAdditionalRemarks() != null) {
            entity.setRemarkMPCD(
                    request.getAdditionalRemarks()
            );
        }

        if (Boolean.TRUE.equals(request.getSubmitIOM())) {

            validateIOMSubmission(request);

            entity.setIomFileId(request.getIomFileId());
            entity.setIomSubmittedOn(LocalDateTime.now());
            entity.setStatus("IOM_SUBMITTED_TO_MD");

            UserWorkloadProjection assignedMD = assignMD();
            entity.setAssignedMDId(assignedMD.getUserId());

            applicationMaster.setCurrentStatus(entity.getStatus());
            applicationMasterRepository.save(applicationMaster);

            EnvironmentClearanceRenewal saved =
                    renewalEnvironmentalClearanceRepository.save(entity);

            createTask(applicationMaster, saved, "MINING ENGINEER", userId, assignedMD.getUserId());

            notificationClient.sendEnvironmentalClearanceAssignmentNotification(
                    assignedMD.getEmail(),
                    assignedMD.getUsername(),
                    entity.getApplicationNo(),
                    "REVIEW IOM");

            notificationClient.sendUserNotification(
                    "Renewal environmental clearance IOM has been assigned.",
                    "An IOM for environmental clearance renewal has been forwarded for your review. Application No. " + entity.getApplicationNo(),
                    assignedMD.getUserId(),
                    "78");

            return environmentClearanceRenewalMapper.toResponseDTO(saved);

        } else if (Boolean.TRUE.equals(request.getApproveApplication())) {

            entity.setStatus("APPROVED_BY_MPCD");
            entity.setMpcdApprovedOn(LocalDateTime.now());

        } else {

            entity.setStatus("UNDER_MPCD_REVIEW");
        }

        applicationMaster.setCurrentStatus(entity.getStatus());
        applicationMasterRepository.save(applicationMaster);

        EnvironmentClearanceRenewal saved =
                renewalEnvironmentalClearanceRepository
                        .save(entity);

        return environmentClearanceRenewalMapper
                .toResponseDTO(saved);
    }

    private void validateIOMSubmission(
            ReviewEnvironmentClearanceMPCDRequest request
    ) {

        if (request.getIomFileId() == null) {
            throw new BusinessException(
                    "IOM file is required"
            );
        }
    }

    private void validateMPCDAssignment(
            EnvironmentClearanceRenewal entity,
            Long userId
    ) {

        if (entity.getAssignedMPCDId() == null) {
            throw new CustomRuntimeException(
                    "Application is not assigned"
            );
        }

        if (!entity.getAssignedMPCDId().equals(userId)) {
            throw new CustomRuntimeException(
                    "You are not assigned to this application"
            );
        }
    }

    @Override
    public SuccessResponse<List<EnvironmentClearanceRenewalResponseDTO>>
    getAssignedToMPCD(
            Long userId,
            Pageable pageable,
            String search
    ) {

        Page<EnvironmentClearanceRenewal> page;

        if (search == null || search.isBlank()) {

            page = renewalEnvironmentalClearanceRepository
                    .findByAssignedMPCDId(
                            userId,
                            pageable
                    );

        } else {

            page = renewalEnvironmentalClearanceRepository
                    .findByAssignedMPCDIdAndApplicationNoContainingIgnoreCase(
                            userId,
                            search,
                            pageable
                    );
        }

        List<EnvironmentClearanceRenewalResponseDTO> response =
                page.getContent()
                        .stream()
                        .map(environmentClearanceRenewalMapper::toResponseDTO)
                        .toList();

        return new SuccessResponse<>(
                "Assigned applications retrieved successfully",
                response
        );
    }

    @Override
    public void reassignTaskMPCD(
            ReassignTaskRequest request,
            Long userId
    ) {

        List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(request.getApplicationNumber(),"MPCD","ASSIGNED TO MPCD", SERVICE_CODE);
        if (task.isEmpty()) {
            throw new BusinessException("No Task Found");
        }

        for(TaskManagement taskManagement : task) {
            taskManagement.setAssignedToUserId(request.getNewAssigneeUserId());
            taskManagement.setAssignedByUserId(userId);
            taskManagement.setAssignedAt(LocalDateTime.now());
            taskManagement.setReassignmentCount(taskManagement.getReassignmentCount() + 1);
            taskManagement.setActionRemarks(request.getRemarks());
        }

        taskManagementRepository.saveAll(task);

        EnvironmentClearanceRenewal entity =
                renewalEnvironmentalClearanceRepository
                        .findByApplicationNo(
                                request.getApplicationNumber()
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Application not found"
                                ));

        entity.setAssignedMPCDId(
                request.getNewAssigneeUserId()
        );

        renewalEnvironmentalClearanceRepository.save(entity);

        TaskManagement firstTask = task.getFirst();

        UserWorkloadProjection userDetails = renewalEnvironmentalClearanceRepository.findUserDetails(request.getNewAssigneeUserId());

        notificationClient.sendTaskRenewalEnvironmentalClearanceReassignmentNotification(
                userDetails.getEmail(), userDetails.getUsername(),
                firstTask.getApplicationNumber(),
                firstTask.getAssignedToRole(),
                request.getRemarks());

        if(userDetails.getUserId()!= null) {
            String title = "An new application has been reassigned.";
            String message = "An application for environmental clearance renewal has been assigned for review. Application No. "+ request.getApplicationNumber()+" Please login in review the application";
            String serviceId = "78";
            notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId);
        }else {
            throw new CustomRuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }

        log.info("MPCD task {} reassigned to user {}", firstTask.getId(), request.getNewAssigneeUserId());
    }


    @Override
    public Page<EnvironmentClearanceRenewalResponseDTO> getMyApplications(Long userId, Pageable pageable, String search) {
        List<String> ApplicationStatus = List.of(
                "DRAFT",
                "ASSIGNED TO MD",
                "ASSIGNED TO MPCD",
                "UNDER_MPCD_REVIEW",
                "APPROVED_BY_MPCD",
                "IOM_SUBMITTED_TO_MD",
                "RESUBMISSION_REQUIRED",
                "ASSIGNED_TO_RC",
                "RC_REPORT_SUBMITTED",
                "ASSIGNED_TO_MI",
                "MI_REPORT_SUBMITTED",
                "UNDER_MD_REVIEW",
                "PAID",
                "FORWARDED_TO_DECC",
                "EC_RENEWED");
        Page<EnvironmentClearanceRenewal> applications;

        if (search == null || search.isBlank()) {
            applications = renewalEnvironmentalClearanceRepository.findByApplicantUserIdAndStatusIn(
                    userId,
                    ApplicationStatus,
                    pageable);
        }
        else {

            applications = renewalEnvironmentalClearanceRepository.findByAssignedToUserAndSearch(
                    userId,
                    ApplicationStatus,
                    search.trim(),
                    pageable
            );
        }
        return applications.map(environmentClearanceRenewalMapper::toResponseDTO);
    }

    @Override
    public EnvironmentClearanceRenewalResponseDTO getApplicationById(Long id, Long userId) {
        EnvironmentClearanceRenewal entity =
                renewalEnvironmentalClearanceRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException("Application not found"));

        boolean isCreator   = userId.equals(entity.getCreatedBy());
        boolean isAssignedMPCD = userId.equals(entity.getAssignedMPCDId());
        boolean isAssignedRC   = userId.equals(entity.getAssignedRCId());
        boolean isAssignedMI   = userId.equals(entity.getAssignedMIId());
        boolean isAssignedMD   = userId.equals(entity.getAssignedMDId());

        if (!isCreator && !isAssignedMPCD && !isAssignedRC && !isAssignedMI && !isAssignedMD) {
            throw new CustomRuntimeException("You are not authorized to access this application");
        }

        return environmentClearanceRenewalMapper
                .toResponseDTO(entity);
    }

    @Override
    public Page<EnvironmentClearanceRenewalResponseDTO> getArchivedApplications(Pageable pageable, String search, Long userId) {
        Page<EnvironmentClearanceRenewal> page;

        if (search == null || search.isBlank()) {

            page = renewalEnvironmentalClearanceRepository
                    .findByStatusIn(
                            List.of("APPROVED", "REJECTED"),
                            pageable
                    );

        } else {

            page = renewalEnvironmentalClearanceRepository
                    .findByStatusInAndApplicationNoContainingIgnoreCase(
                            List.of("APPROVED", "REJECTED"),
                            search,
                            pageable
                    );
        }

        return page.map(
                environmentClearanceRenewalMapper::toResponseDTO
        );
    }

    @Override
    public Page<EnvironmentClearanceRenewalResponseDTO> getMyArchivedApplications(Long userId, Pageable pageable, String search) {
        Page<EnvironmentClearanceRenewal> page;

        if (search == null || search.isBlank()) {

            page = renewalEnvironmentalClearanceRepository
                    .findByCreatedByAndStatusIn(
                            userId,
                            List.of("APPROVED", "REJECTED"),
                            pageable
                    );

        } else {

            page = renewalEnvironmentalClearanceRepository
                    .findByCreatedByAndStatusInAndApplicationNoContainingIgnoreCase(
                            userId,
                            List.of("APPROVED", "REJECTED"),
                            search,
                            pageable
                    );
        }

        return page.map(
                environmentClearanceRenewalMapper::toResponseDTO
        );
    }

    @Override
    public EnvironmentClearanceRenewalResponseDTO updateDraft(
            Long id,
            EnvironmentClearanceRenewalRequestDTO request,
            Long userId
    ) {

        EnvironmentClearanceRenewal entity =
                getOwnedApplication(id, userId);

        validateDraftStatus(entity);

        entity.setServiceType(request.getServiceType());
        entity.setLocation(request.getLocation());
        entity.setArea(request.getArea());
        entity.setPreviousEcFileId(
                request.getPreviousEcFileId()
        );
        entity.setSelfMonitoringReportFileId(
                request.getSelfMonitoringReportFileId()
        );

        EnvironmentClearanceRenewal saved =
                renewalEnvironmentalClearanceRepository
                        .save(entity);

        return environmentClearanceRenewalMapper
                .toResponseDTO(saved);
    }

    @Override
    public void deleteDraft(
            Long id,
            Long userId
    ) {

        EnvironmentClearanceRenewal entity =
                getOwnedApplication(id, userId);

        validateDraftStatus(entity);

        renewalEnvironmentalClearanceRepository
                .delete(entity);
    }

    @Override
    public EnvironmentClearanceRenewalResponseDTO resubmitApplication(
            Long id,
            EnvironmentClearanceRenewalRequestDTO request,
            Long userId
    ) {

        EnvironmentClearanceRenewal entity =
                getOwnedApplication(id, userId);

        validateResubmissionStatus(entity);

        entity.setLocation(request.getLocation());
        entity.setArea(request.getArea());
        entity.setPreviousEcFileId(
                request.getPreviousEcFileId()
        );
        entity.setSelfMonitoringReportFileId(
                request.getSelfMonitoringReportFileId()
        );

        routeApplication(entity);

        EnvironmentClearanceRenewal saved =
                renewalEnvironmentalClearanceRepository
                        .save(entity);

        return environmentClearanceRenewalMapper
                .toResponseDTO(saved);
    }

    @Override
    public Page<EnvironmentClearanceRenewalResponseDTO>
    getArchivedApplicationsMPCD(
            Long userId,
            Pageable pageable,
            String search
    ) {

        List<String> archivedStatuses = List.of(
                "APPROVED",
                "REJECTED"
        );

        Page<EnvironmentClearanceRenewal> page;

        if (search == null || search.isBlank()) {

            page = renewalEnvironmentalClearanceRepository
                    .findByAssignedMPCDIdAndStatusIn(
                            userId,
                            archivedStatuses,
                            pageable
                    );

        } else {

            page = renewalEnvironmentalClearanceRepository
                    .findByAssignedMPCDIdAndApplicationNoContainingIgnoreCaseAndStatusIn(
                            userId,
                            search.trim(),
                            archivedStatuses,
                            pageable
                    );
        }

        return page.map(
                environmentClearanceRenewalMapper::toResponseDTO
        );
    }

    @Override
    public EnvironmentClearanceRenewalResponseDTO requestResubmission(
            RequestResubmissionDTO request,
            Long userId
    ) {

        EnvironmentClearanceRenewal entity =
                renewalEnvironmentalClearanceRepository
                        .findById(request.getRenewalId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Application not found"
                                ));

        validateMPCDAssignment(entity, userId);

        entity.setRemarkMPCD(request.getRemarks());
        entity.setStatus("RESUBMISSION_REQUIRED");

        entity.getApplicationMaster()
                .setCurrentStatus("RESUBMISSION_REQUIRED");

        EnvironmentClearanceRenewal saved =
                renewalEnvironmentalClearanceRepository
                        .save(entity);

        return environmentClearanceRenewalMapper
                .toResponseDTO(saved);
    }

    @Override
    public EnvironmentClearanceRenewalResponseDTO rejectApplicationMPCD(
            RejectApplicationDTO request,
            Long userId
    ) {

        EnvironmentClearanceRenewal entity =
                renewalEnvironmentalClearanceRepository
                        .findById(request.getRenewalId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Application not found"
                                ));

        validateMPCDAssignment(entity, userId);

        entity.setRemarkMPCD(
                request.getRejectionRemarks()
        );

        entity.setStatus("REJECTED");

        entity.getApplicationMaster()
                .setCurrentStatus("REJECTED");
        // Terminal state — mark completion so the citizen tracking dashboard archives it
        entity.getApplicationMaster()
                .setCompletedOn(LocalDateTime.now());

        EnvironmentClearanceRenewal saved =
                renewalEnvironmentalClearanceRepository
                        .save(entity);

        return environmentClearanceRenewalMapper
                .toResponseDTO(saved);
    }

    @Override
    public EnvironmentClearanceRenewalResponseDTO assignRC(
            AssignRCRequestDTO request,
            Long userId
    ) {

        EnvironmentClearanceRenewal entity =
                renewalEnvironmentalClearanceRepository
                        .findById(request.getRenewalId())
                        .orElseThrow(() ->
                                new RuntimeException("Application not found"));

        validateMPCDAssignment(entity, userId);

        entity.setAssignedRCId(request.getRcUserId());
        entity.setRemarkRC(request.getRemarks());
        entity.setStatus("ASSIGNED_TO_RC");

        entity.getApplicationMaster()
                .setCurrentStatus("ASSIGNED_TO_RC");

        createTask(
                entity.getApplicationMaster(),
                entity,
                "RC",
                userId,
                request.getRcUserId()
        );

        EnvironmentClearanceRenewal saved =
                renewalEnvironmentalClearanceRepository.save(entity);

        return environmentClearanceRenewalMapper.toResponseDTO(saved);
    }

    @Override
    public SuccessResponse<List<EnvironmentClearanceRenewalResponseDTO>>
    getAssignedToRC(
            Long userId,
            Pageable pageable,
            String search
    ) {

        Page<EnvironmentClearanceRenewal> page;

        List<String> applicationStatuses = List.of(
                "ASSIGNED_TO_RC",
                "MI_REPORT_SUBMITTED"
        );

        if (search == null || search.isBlank()) {

            page = renewalEnvironmentalClearanceRepository
                    .findByAssignedRCIdAndStatusIn(
                            userId,
                            applicationStatuses,
                            pageable
                    );

        } else {

            page = renewalEnvironmentalClearanceRepository
                    .findByAssignedRCIdAndApplicationNoContainingIgnoreCaseAndStatusIn(
                            userId,
                            search.trim(),
                            applicationStatuses,
                            pageable
                    );
        }

        Page<EnvironmentClearanceRenewalResponseDTO> responsePage =
                page.map(
                        environmentClearanceRenewalMapper::toResponseDTO
                );

        return SuccessResponse.fromPage(
                "Assigned RC applications retrieved successfully",
                responsePage
        );
    }

    @Override
    public EnvironmentClearanceRenewalResponseDTO submitRCReport(
            SubmitRCReportDTO request,
            Long userId
    ) {

        EnvironmentClearanceRenewal entity =
                renewalEnvironmentalClearanceRepository
                        .findById(request.getRenewalId())
                        .orElseThrow(() ->
                                new RuntimeException("Application not found"));

        if (!entity.getAssignedRCId().equals(userId)) {
            throw new CustomRuntimeException("You are not assigned");
        }

        entity.setRcSiteReportFileId(
                request.getRcSiteReportFileId()
        );

        entity.setRemarkRC(
                request.getRemarks()
        );

        entity.setRcReportSubmittedOn(
                LocalDateTime.now()
        );

        entity.setStatus("RC_REPORT_SUBMITTED");

        entity.getApplicationMaster()
                .setCurrentStatus("RC_REPORT_SUBMITTED");

        EnvironmentClearanceRenewal saved =
                renewalEnvironmentalClearanceRepository.save(entity);

        return environmentClearanceRenewalMapper.toResponseDTO(saved);
    }

    @Override
    public EnvironmentClearanceRenewalResponseDTO assignMI(
            AssignMIRequestDTO request,
            Long userId
    ) {

        EnvironmentClearanceRenewal entity =
                renewalEnvironmentalClearanceRepository
                        .findById(request.getRenewalId())
                        .orElseThrow(() ->
                                new RuntimeException("Application not found"));

        if (!entity.getAssignedRCId().equals(userId)) {
            throw new CustomRuntimeException("You are not assigned as RC");
        }

        entity.setAssignedMIId(request.getMiUserId());
        entity.setRemarkMI(request.getRemarks());
        entity.setStatus("ASSIGNED_TO_MI");

        entity.getApplicationMaster()
                .setCurrentStatus("ASSIGNED_TO_MI");

        createTask(
                entity.getApplicationMaster(),
                entity,
                "MI",
                userId,
                request.getMiUserId()
        );

        EnvironmentClearanceRenewal saved =
                renewalEnvironmentalClearanceRepository.save(entity);

        return environmentClearanceRenewalMapper.toResponseDTO(saved);
    }

    @Override
    public SuccessResponse<List<EnvironmentClearanceRenewalResponseDTO>>
    getAssignedToMI(
            Long userId,
            Pageable pageable,
            String search
    ) {

        Page<EnvironmentClearanceRenewal> page;

        List<String> applicationStatuses = List.of(
                "ASSIGNED_TO_MI"
        );

        if (search == null || search.isBlank()) {

            page = renewalEnvironmentalClearanceRepository
                    .findByAssignedMIIdAndStatusIn(
                            userId,
                            applicationStatuses,
                            pageable
                    );

        } else {

            page = renewalEnvironmentalClearanceRepository
                    .findByAssignedMIIdAndApplicationNoContainingIgnoreCaseAndStatusIn(
                            userId,
                            search.trim(),
                            applicationStatuses,
                            pageable
                    );
        }

        Page<EnvironmentClearanceRenewalResponseDTO> responsePage =
                page.map(
                        environmentClearanceRenewalMapper::toResponseDTO
                );

        return SuccessResponse.fromPage(
                "Assigned MI applications retrieved successfully",
                responsePage
        );
    }

    @Override
    public EnvironmentClearanceRenewalResponseDTO submitMIReport(
            SubmitMIReportDTO request,
            Long userId
    ) {

        EnvironmentClearanceRenewal entity =
                renewalEnvironmentalClearanceRepository
                        .findById(request.getRenewalId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Application not found"
                                ));

        if (!entity.getAssignedMIId().equals(userId)) {
            throw new CustomRuntimeException(
                    "You are not assigned as MI"
            );
        }

        entity.setMiSiteReportFileId(
                request.getMiSiteReportFileId()
        );

        entity.setRemarkMI(
                request.getRemarks()
        );

        entity.setMiReportSubmittedOn(
                LocalDateTime.now()
        );

        entity.setStatus("MI_REPORT_SUBMITTED");

        entity.getApplicationMaster()
                .setCurrentStatus("MI_REPORT_SUBMITTED");

        EnvironmentClearanceRenewal saved =
                renewalEnvironmentalClearanceRepository
                        .save(entity);

        return environmentClearanceRenewalMapper
                .toResponseDTO(saved);
    }

    @Override
    public SuccessResponse<List<EnvironmentClearanceRenewalResponseDTO>>
    getAssignedToMD(Long userId, Pageable pageable, String search) {

        Page<EnvironmentClearanceRenewal> page;

        List<String> statuses = List.of(
                "ASSIGNED_TO_MD",
                "UNDER_MD_REVIEW",
                "IOM_SUBMITTED_TO_MD",
                "PAID"
        );

        if (search == null || search.isBlank()) {

            page = renewalEnvironmentalClearanceRepository
                    .findByAssignedMDIdAndStatusIn(userId, statuses, pageable);

        } else {

            page = renewalEnvironmentalClearanceRepository
                    .findByAssignedMDIdAndApplicationNoContainingIgnoreCaseAndStatusIn(
                            userId,
                            search.trim(),
                            statuses,
                            pageable
                    );
        }

        Page<EnvironmentClearanceRenewalResponseDTO> response =
                page.map(environmentClearanceRenewalMapper::toResponseDTO);

        return SuccessResponse.fromPage(
                "MD applications retrieved successfully",
                response
        );
    }

    @Override
    public EnvironmentClearanceRenewalResponseDTO approveEC(
            ApproveECRequestDTO request,
            Long userId
    ) {

        EnvironmentClearanceRenewal entity =
                renewalEnvironmentalClearanceRepository
                        .findById(request.getRenewalId())
                        .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!userId.equals(entity.getAssignedMDId())) {
            throw new CustomRuntimeException("You are not assigned as MD");
        }

        entity.setEcCertificateFileId(request.getEcCertificateFileId());
        entity.setEcGeneratedOn(LocalDateTime.now());
        entity.setMdApprovedOn(LocalDateTime.now());

        if (Boolean.TRUE.equals(request.getForwardToDECC())) {
            entity.setStatus("FORWARDED_TO_DECC");
        } else {
            entity.setStatus("EC_RENEWED");
            // Terminal state — mark completion so the citizen tracking dashboard archives it
            entity.getApplicationMaster()
                    .setCompletedOn(LocalDateTime.now());
        }

        entity.getApplicationMaster()
                .setCurrentStatus(entity.getStatus());

        EnvironmentClearanceRenewal saved =
                renewalEnvironmentalClearanceRepository.save(entity);

        return environmentClearanceRenewalMapper.toResponseDTO(saved);
    }

    private EnvironmentClearanceRenewal getOwnedApplication(
            Long id,
            Long userId
    ) {

        return renewalEnvironmentalClearanceRepository
                .findByIdAndCreatedBy(id, userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Application not found"
                        ));
    }

    private void validateOwnership(
            EnvironmentClearanceRenewal entity,
            Long userId
    ) {

        if (!entity.getCreatedBy().equals(userId)) {
            throw new CustomRuntimeException(
                    "You are not authorized to access this application"
            );
        }
    }

    private void validateDraftStatus(
            EnvironmentClearanceRenewal entity
    ) {

        if (!"DRAFT".equals(entity.getStatus())) {
            throw new CustomRuntimeException(
                    "Only draft application can be modified"
            );
        }
    }

    private void validateResubmissionStatus(
            EnvironmentClearanceRenewal entity
    ) {

        if (!"RESUBMISSION_REQUIRED".equals(
                entity.getStatus())) {
            throw new CustomRuntimeException(
                    "Application is not in resubmission state"
            );
        }
    }
}
