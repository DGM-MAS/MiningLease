package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.entity.*;
import com.mas.gov.bt.mas.primary.repository.*;
import com.mas.gov.bt.mas.primary.utility.CustomRuntimeException;


import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.SampleTransportClearanceResponseDTO;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.mapper.SampleTransportClearanceMapper;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import com.mas.gov.bt.mas.primary.utility.LookupHelper;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class SampleTransportClearanceServiceImpl
        implements SampleTransportClearanceService {

    private static final String SERVICE_CODE = "SAMPLE_TRANSPORT_CLEARANCE";
    private final SampleTransportClearanceRepository repository;
    private final SampleTransportClearanceMapper sampleTransportClearanceMapper;
    private final ApplicationMasterRepository applicationMasterRepository;
    private final TaskManagementRepository taskManagementRepository;
    private final NotificationClient notificationClient;


    private final DzongkhagLookupRepository dzongkhagLookupRepository;

    private final GewogLookupRepository gewogLookupRepository;

    private final VillageLookupRepository villageLookupRepository;

    private final RegionMasterRepository regionMasterRepository;

    private final LookupHelper lookupHelper;

        @Override
        public SampleTransportClearanceResponseDTO createApplication(
                SampleTransportClearanceDTO request,
                Long userId) {

            Long regionId;

            DzongkhagLookup dzongkhagLookup =
                    lookupHelper.fetchLookup(request.getDzongkhagID(), dzongkhagLookupRepository, "Dzongkhag");

            regionId = dzongkhagLookup.getRegion().getId();

            GewogLookup gewogLookup =
                    lookupHelper.fetchLookup(request.getGewogID(), gewogLookupRepository, "Gewog");

            VillageLookup villageLookup =
                    lookupHelper.fetchLookup(request.getVillageID(), villageLookupRepository, "Village");

            RegionMaster regionMaster =
                    lookupHelper.fetchLookup(dzongkhagLookup.getRegion().getId(), regionMasterRepository, "RegionMaster");

            SampleTransportClearanceEntity entity =
                    sampleTransportClearanceMapper.toEntity(request);


            // Site Details
//            entity.setSiteApplicationNo(request.getSiteApplicationNo());
//            entity.setSiteName(request.getSiteName());



            entity.setDzongkhagId(dzongkhagLookup);
            entity.setGewogId(gewogLookup);
            entity.setVillageId(villageLookup);
            entity.setRegionMaster(regionMaster);

            entity.setRegionId(regionId);

            entity.setApplicationNo(generateApplicationNumber());
            entity.setCreatedOn(LocalDateTime.now());
            entity.setCreatedBy(userId);
            entity.setStatus("SUBMITTED");

            SampleTransportClearanceEntity saved =
                    repository.save(entity);

            UserWorkloadProjection assignedChief = null;

            assignedChief = assignChief(regionId);

            if (assignedChief == null) {
                assignedChief = assignChief(9L);
            }

            entity.setAssignedGSDChiefId(assignedChief.getUserId());

            repository.save(entity);

            ApplicationMaster master = createApplicationMaster(entity, userId);

            entity.setApplicationMaster(master);

            createTask(
                    master,
                    entity,
                    "GSD_CHIEF",
                    userId,
                    assignedChief.getUserId()
            );

            return sampleTransportClearanceMapper.toResponseDTO(saved);
        }

    @Override
    public Page<SampleTransportClearanceResponseDTO> getMyApplications(Long userId, Pageable pageable, String search) {
        List<String> ApplicationStatus = List.of(
                "SUBMITTED", "ASSIGNED", "ACCEPTED");
        Page<SampleTransportClearanceEntity> applications;

        if (search == null || search.isBlank()) {
            applications = repository.findByCreatedByAndStatusIn(
                    userId,
                    ApplicationStatus,
                    pageable);
        } else {

            applications = repository.findByAssignedToUserAndSearch(
                    userId,
                    ApplicationStatus,
                    search.trim(),
                    pageable
            );
        }
        return applications.map(sampleTransportClearanceMapper::toListResponse);
    }

    @Override
    public Page<SampleTransportClearanceResponseDTO> getMyArchivedApplications(Long userId, Pageable pageable, String search) {
        List<String> archivedStatuses = List.of("APPROVED", "REJECTED");
        Page<SampleTransportClearanceEntity> applications;

        if (search == null || search.isBlank()) {
            applications = repository.findByCreatedByAndStatusIn(userId, archivedStatuses, pageable);
        } else {
            applications = repository.findByCreatedByAndSearch(userId, archivedStatuses, search.trim(), pageable);
        }

        return applications.map(sampleTransportClearanceMapper::toListResponse);
    }

    @Override
    public SuccessResponse<List<SampleTransportClearanceResponseDTO>> getAllApplicationAdmin(Pageable pageable, String search) {
        Page<SampleTransportClearanceEntity> page;

        if (search == null || search.isBlank()) {
            page = repository.findAll(pageable);
        } else {
            page = repository.findAllBySearch(search.trim(), pageable);
        }

        return SuccessResponse.fromPage("Applications fetched successfully", page.map(sampleTransportClearanceMapper::toListResponse));
    }

    @Override
    public SuccessResponse<List<SampleTransportClearanceResponseDTO>> getAssignedToGSDChief(Long userId, Pageable pageable, String search) {
        Page<SampleTransportClearanceEntity> page;

        List<String> ApplicationStatus = List.of(
                "SUBMITTED",
                "ASSIGNED",
                "ACCEPTED",
                "DRAFT",
                "PAYMENT PENDING");

        if (search == null || search.isBlank()) {

            page = repository
                    .findByAssignedGSDChiefIdAndStatusIn(userId, pageable, ApplicationStatus);

        } else {

            page = repository
                    .findByAssignedGSDChiefIdAndApplicationNoContainingIgnoreCaseAndStatusIn(
                            userId,
                            search.trim(),
                            pageable,
                            ApplicationStatus
                    );
        }

        Page<SampleTransportClearanceResponseDTO> responsePage =
                page.map(sampleTransportClearanceMapper::toListResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Override
    public SampleTransportClearanceResponseDTO assignApplicationChief(AssignedTaskChiefDTO request, Long userId) {
        SampleTransportClearanceEntity sampleTransportClearanceEntity;
        ApplicationMaster applicationMaster;

        sampleTransportClearanceEntity = repository.findByApplicationNo(request.getApplicationNo())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        applicationMaster = sampleTransportClearanceEntity.getApplicationMaster();

        if (request.getGsdFocalId() != null) {
            sampleTransportClearanceEntity.setStatus("ASSIGNED");
            sampleTransportClearanceEntity.setAssignedGSDFocalId(request.getGsdFocalId());
        } else {
            throw new BusinessException("GSD FocalId is null");
        }

        repository.save(sampleTransportClearanceEntity);

        completeCurrentTask(sampleTransportClearanceEntity.getApplicationNo(), "GSD_CHIEF", "ASSIGNED", null);

        if (applicationMaster != null) {
            applicationMaster.setCurrentStatus("ASSIGNED");
            applicationMasterRepository.save(applicationMaster);
        }

        if (request.getGsdFocalId() != null) {
            createTask(
                    applicationMaster,
                    sampleTransportClearanceEntity,
                    "GSD_FOCAL",
                    userId,
                    request.getGsdFocalId()
            );

            UserWorkloadProjection applicantDetails = repository.findUserDetails(sampleTransportClearanceEntity.getCreatedBy());

            UserWorkloadProjection assignedGHDFocal = repository.findUserDetails(request.getGsdFocalId());

            if (sampleTransportClearanceEntity.getCreatedBy() != null) {
                notificationClient.sendStatusUpdateNotification(
                        applicantDetails.getEmail(),
                        applicantDetails.getUsername(),
                        sampleTransportClearanceEntity.getApplicationNo(),
                        sampleTransportClearanceEntity.getStatus(),
                        "ASSIGNED"
                );

                String title = "Application has been status has been updated.";
                String message = "Application number " + sampleTransportClearanceEntity.getApplicationNo() + "GHD focal has been assigned.";
                String serviceId = "78";
                notificationClient.sendUserNotification(title, message, sampleTransportClearanceEntity.getCreatedBy(), serviceId, "CITIZEN");
            }
            if (assignedGHDFocal != null) {

                notificationClient.sendAssignmentNotification(
                        assignedGHDFocal.getEmail(),
                        assignedGHDFocal.getUsername(),
                        sampleTransportClearanceEntity.getApplicationNo(),
                        "ASSIGNED");

                String title = "A new geo physics application has been assigned.";
                String message = "A new geo physics application has been assigned.";
                String serviceId = "78";
                notificationClient.sendUserNotification(title, message, assignedGHDFocal.getUserId(), serviceId, "STAFF");
            }
        }

        return sampleTransportClearanceMapper.toListResponse(sampleTransportClearanceEntity);
    }

    @Override
    public SampleTransportClearanceResponseDTO reviewApplicationChief(ReviewSampleTransportClearanceRequestDTO request, Long userId) {
        SampleTransportClearanceEntity sampleTransportClearanceEntity = repository.findByApplicationNo(request.getApplicationNo())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        ApplicationMaster applicationMaster = sampleTransportClearanceEntity.getApplicationMaster();

        UserWorkloadProjection applicantDetails = repository.findUserDetails(sampleTransportClearanceEntity.getCreatedBy());

        UserWorkloadProjection assignedGHDFocal = repository.findUserDetails(sampleTransportClearanceEntity.getAssignedGSDFocalId());

        if (request.getStatus() != null) {
            switch (request.getStatus()) {
                case "REJECTED" -> {
                    sampleTransportClearanceEntity.setStatus("REJECTED");
                    sampleTransportClearanceEntity.setAssignedGSDChiefRemarks(request.getRemarks());
                    repository.save(sampleTransportClearanceEntity);

                    completeCurrentTask(sampleTransportClearanceEntity.getApplicationNo(), "GSD_CHIEF", "REJECTED", request.getRemarks());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("REJECTED");
                        applicationMaster.setRejectionRemarks(request.getRemarks());
                        applicationMaster.setRejectedAt(LocalDateTime.now());
                        applicationMasterRepository.save(applicationMaster);
                    }
                    if (applicantDetails != null) {
                        notificationClient.sendRejectionSampleTransportNotification(
                                applicantDetails.getEmail(),
                                applicantDetails.getUsername(),
                                sampleTransportClearanceEntity.getApplicationNo(),
                                "REJECTED"
                        );

                        String title = "Your sample transport clearance application has been rejected by GSD Chief.";
                        String message = "Your sample transport clearance application has been rejected by GSD Chief. Application No : " + sampleTransportClearanceEntity.getApplicationNo();
                        String serviceId = "78";
                        notificationClient.sendUserNotification(title, message, applicantDetails.getUserId(), serviceId, "CITIZEN");

                    } else {
                        throw new BusinessException("Applicant details not found for notification");
                    }

                }
                case "APPROVED" -> {
                    sampleTransportClearanceEntity.setStatus("APPROVED");
                    sampleTransportClearanceEntity.setAssignedGSDChiefRemarks(request.getRemarks());

                    repository.save(sampleTransportClearanceEntity);

                    completeCurrentTask(sampleTransportClearanceEntity.getApplicationNo(), "GSD_CHIEF", "APPROVED", request.getRemarks());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("APPROVED");
                        applicationMaster.setApprovedAt(LocalDateTime.now());
                        applicationMasterRepository.save(applicationMaster);
                    }

                    if (applicantDetails.getUserId() != null) {
                        assert applicationMaster != null;
                        createTask(
                                applicationMaster,
                                sampleTransportClearanceEntity,
                                "APPLICANT",
                                userId,
                                applicantDetails.getUserId()
                        );

                        notificationClient.sendApprovalSampleTransportNotification(
                                applicantDetails.getEmail(),
                                applicantDetails.getUsername(),
                                sampleTransportClearanceEntity.getApplicationNo());
                    } else {
                        throw new BusinessException("Assigned GHD Chief details not found for notification");
                    }
                }
                default -> throw new BusinessException("Invalid status value");
            }
        }
        return sampleTransportClearanceMapper.toListResponse(sampleTransportClearanceEntity);
    }

    @Override
    public void reassignTaskGSDChief(ReassignTaskRequest request, Long userId) {
        log.info("GHD chief reassigning task: {} by user: {}", request.getApplicationNumber(), userId);

        List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(request.getApplicationNumber(),"SUBMITTED","GSD_CHIEF", SERVICE_CODE);

        SampleTransportClearanceEntity sampleTransportClearanceEntity = repository.findByApplicationNo(request.getApplicationNumber())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        sampleTransportClearanceEntity.setAssignedGSDChiefId(request.getNewAssigneeUserId());
        repository.save(sampleTransportClearanceEntity);

        if (task == null) {
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

        TaskManagement firstTask = task.getFirst();

        UserWorkloadProjection userDetails = repository.findUserDetails(request.getNewAssigneeUserId());

        notificationClient.sendTaskReassignmentNotification(
                userDetails.getEmail(), userDetails.getUsername(),
                firstTask.getApplicationNumber(),
                firstTask.getAssignedToRole(),
                request.getRemarks());

        if(userDetails.getUserId()!= null) {
            String title = "An new application has been reassigned.";
            String message = "An application for sample transport clearance has been assigned for review. Application No. "+request.getApplicationNumber()+" Please login in review the application";
            String serviceId = "78";
            notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId, "STAFF");
        }else {
            throw new CustomRuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }

        log.info("Task reassigned by chief to user {}", request.getNewAssigneeUserId());
    }

    @Override
    public SuccessResponse<List<SampleTransportClearanceResponseDTO>> getAssignedToGSDFocal(Long userId, Pageable pageable, String search) {
        Page<SampleTransportClearanceEntity> page;

        List<String> ApplicationStatus = List.of(
                "SUBMITTED",
                "ASSIGNED",
                "ACCEPTED");

        if (search == null || search.isBlank()) {

            page = repository
                    .findByAssignedGSDFocalIdAndStatusIn(userId, pageable, ApplicationStatus);

        } else {

            page = repository
                    .findByAssignedGSDFocalIdAndApplicationNoContainingIgnoreCaseAndStatusIn(
                            userId,
                            search.trim(),
                            pageable,
                            ApplicationStatus
                    );
        }

        Page<SampleTransportClearanceResponseDTO> responsePage =
                page.map(sampleTransportClearanceMapper::toListResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Override
    public SampleTransportClearanceResponseDTO reviewApplicationGSDFocal(SampleTransportClearanceGSDFocalReviewRequestDTO request, Long userId) {
        SampleTransportClearanceEntity sampleTransportClearanceEntity = repository.findByApplicationNo(request.getApplicationNo())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        ApplicationMaster applicationMaster = sampleTransportClearanceEntity.getApplicationMaster();

        UserWorkloadProjection applicantDetails = repository.findUserDetails(sampleTransportClearanceEntity.getCreatedBy());

        UserWorkloadProjection assignedChiefDetails = repository.findUserDetails(sampleTransportClearanceEntity.getAssignedGSDChiefId());

        if (request.getStatus() != null) {
            switch (request.getStatus()) {
                case "REJECTED" -> {
                    sampleTransportClearanceEntity.setStatus("REJECTED");
                    sampleTransportClearanceEntity.setAssignedGSDFocalRemarks(request.getRemarksGSDFocal());
                    repository.save(sampleTransportClearanceEntity);

                    completeCurrentTask(sampleTransportClearanceEntity.getApplicationNo(), "GSD_FOCAL", "REJECTED", request.getRemarksGSDFocal());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("REJECTED");
                        applicationMaster.setRejectionRemarks(request.getRemarksGSDFocal());
                        applicationMaster.setRejectedAt(LocalDateTime.now());
                        applicationMasterRepository.save(applicationMaster);
                    }
                    if (applicantDetails != null) {
                        notificationClient.sendRejectionSampleTransportNotification(
                                applicantDetails.getEmail(),
                                applicantDetails.getUsername(),
                                sampleTransportClearanceEntity.getApplicationNo(),
                                "REJECTED"
                        );

                        String title = "Your sample transport clearance application has been rejected by GSD focal.";
                        String message = " Your sample transport clearance application has been rejected by GSD focal. Application No : " + sampleTransportClearanceEntity.getApplicationNo();
                        String serviceId = "78";
                        notificationClient.sendUserNotification(title, message, applicantDetails.getUserId(), serviceId, "CITIZEN");

                    } else {
                        throw new BusinessException("Applicant details not found for notification");
                    }

                }
                case "ACCEPTED" -> {
                    sampleTransportClearanceEntity.setStatus("ACCEPTED");
                    sampleTransportClearanceEntity.setAssignedGSDFocalRemarks(request.getRemarksGSDFocal());
                    repository.save(sampleTransportClearanceEntity);

                    completeCurrentTask(sampleTransportClearanceEntity.getApplicationNo(), "GSD_FOCAL", "ACCEPTED", request.getRemarksGSDFocal());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("ACCEPTED");
                        applicationMasterRepository.save(applicationMaster);
                    }

                    if (applicantDetails.getUserId() != null) {
                        assert applicationMaster != null;
                        createTask(
                                applicationMaster,
                                sampleTransportClearanceEntity,
                                "GSD_CHIEF",
                                userId,
                                assignedChiefDetails.getUserId());

                        notificationClient.GSDFocalAcceptedReviewedNotification(
                                assignedChiefDetails.getEmail(),
                                assignedChiefDetails.getUsername(),
                                sampleTransportClearanceEntity.getApplicationNo());
                    } else {
                        throw new BusinessException("Assigned GHD Chief details not found for notification");
                    }

                    if (assignedChiefDetails.getUserId() != null) {

                        String title = "Sample Transport clearance Application has been accepted. Application No. "+ sampleTransportClearanceEntity.getApplicationNo();
                        String message = "GSD Focal has accepted this application. Please review and approve the application to end the process.";
                        String serviceId = "78";
                        notificationClient.sendUserNotification(title, message, assignedChiefDetails.getUserId(), serviceId, "STAFF");
                    } else {
                        throw new BusinessException("Applicant details not found for notification");
                    }
                }
                default -> throw new BusinessException("Invalid status value");
            }
        }
        return sampleTransportClearanceMapper.toListResponse(sampleTransportClearanceEntity);
    }

    @Override
    public SuccessResponse<List<SampleTransportClearanceResponseDTO>> getArchivedForGSDChief(Long userId, Pageable pageable, String search) {
        List<String> terminalStatuses = List.of("APPROVED", "REJECTED");
        Page<SampleTransportClearanceEntity> page;

        if (search == null || search.isBlank()) {
            page = repository.findByAssignedGSDChiefIdAndStatusIn(userId, pageable, terminalStatuses);
        } else {
            page = repository.findByAssignedGSDChiefIdAndApplicationNoContainingIgnoreCaseAndStatusIn(
                    userId, search.trim(), pageable, terminalStatuses);
        }

        return SuccessResponse.fromPage("Archived applications fetched successfully",
                page.map(sampleTransportClearanceMapper::toListResponse));
    }

    @Override
    public SuccessResponse<List<SampleTransportClearanceResponseDTO>> getArchivedForGSDFocal(Long userId, Pageable pageable, String search) {
        List<String> terminalStatuses = List.of("APPROVED", "REJECTED");
        Page<SampleTransportClearanceEntity> page;

        if (search == null || search.isBlank()) {
            page = repository.findByAssignedGSDFocalIdAndStatusIn(userId, pageable, terminalStatuses);
        } else {
            page = repository.findByAssignedGSDFocalIdAndApplicationNoContainingIgnoreCaseAndStatusIn(
                    userId, search.trim(), pageable, terminalStatuses);
        }

        return SuccessResponse.fromPage("Archived applications fetched successfully",
                page.map(sampleTransportClearanceMapper::toListResponse));
    }

    @Override
    public SampleTransportClearanceResponseDTO getApplicationByNo(String applicationNo) {
        SampleTransportClearanceEntity entity = repository.findByApplicationNo(applicationNo)
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));
        return sampleTransportClearanceMapper.toResponseDTO(entity);
    }

    @Override
    public void reassignTaskGSDFocal(ReassignTaskRequest request, Long userId) {
        log.info("GSD focal reassigning task: {} by user: {}", request.getApplicationNumber(), userId);

        List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(
                request.getApplicationNumber(), "ASSIGNED", "GSD_FOCAL", SERVICE_CODE);

        SampleTransportClearanceEntity entity = repository.findByApplicationNo(request.getApplicationNumber())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        entity.setAssignedGSDFocalId(request.getNewAssigneeUserId());
        repository.save(entity);

        if (task == null || task.isEmpty()) {
            throw new BusinessException("No Task Found");
        }

        for (TaskManagement taskManagement : task) {
            taskManagement.setAssignedToUserId(request.getNewAssigneeUserId());
            taskManagement.setAssignedByUserId(userId);
            taskManagement.setAssignedAt(LocalDateTime.now());
            taskManagement.setReassignmentCount(taskManagement.getReassignmentCount() + 1);
            taskManagement.setActionRemarks(request.getRemarks());
        }

        taskManagementRepository.saveAll(task);

        UserWorkloadProjection userDetails = repository.findUserDetails(request.getNewAssigneeUserId());

        if (userDetails == null || userDetails.getUserId() == null) {
            throw new BusinessException("New assignee user not found");
        }

        notificationClient.sendTaskReassignmentNotification(
                userDetails.getEmail(), userDetails.getUsername(),
                request.getApplicationNumber(),
                "GSD_FOCAL",
                request.getRemarks());

        String title = "A new application has been reassigned.";
        String message = "An application for sample transport clearance has been assigned for review. Application No. "
                + request.getApplicationNumber() + " Please login to review the application";
        String serviceId = "78";
        notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId, "STAFF");

        log.info("Task reassigned by focal to user {}", request.getNewAssigneeUserId());
    }

    private void completeCurrentTask(String applicationNo, String role, String actionTaken, String remarks) {
        List<TaskManagement> tasks = taskManagementRepository.findByApplicationNumber(applicationNo);
        List<TaskManagement> toUpdate = tasks.stream()
                .filter(t -> role.equals(t.getAssignedToRole())
                          && SERVICE_CODE.equals(t.getServiceCode())
                          && !"COMPLETED".equals(t.getTaskStatus()))
                .toList();
        toUpdate.forEach(t -> {
            t.setTaskStatus("COMPLETED");
            t.setActionTaken(actionTaken);
            t.setActionRemarks(remarks);
        });
        if (!toUpdate.isEmpty()) {
            taskManagementRepository.saveAll(toUpdate);
        }
    }

    private synchronized String generateApplicationNumber() {

        int year = Year.now().getValue();
        String prefix = String.format("SAM-TRN-%d-", year);

        Integer maxSequence =
                repository.findMaxSequenceByPrefix(prefix);

        long nextSequence =
                (maxSequence == null ? 0 : maxSequence) + 1;

        return String.format(
                "SAM-TRN-%d-%06d",
                year,
                nextSequence
        );
    }

    private UserWorkloadProjection assignChief(Long regionId) {
        UserWorkloadProjection chief =
                repository.findChiefGSDSample(regionId);

        if (chief == null && regionId == 9L) {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "GSD Chief with required permission permission, role and region not found");
        }
        return chief;
    }

    @Transactional
    private ApplicationMaster createApplicationMaster(SampleTransportClearanceEntity sampleTransportClearanceEntity, Long userId) {
        ApplicationMaster master = new ApplicationMaster();
        master.setApplicationNumber(sampleTransportClearanceEntity.getApplicationNo());
        master.setServiceCode(SERVICE_CODE);
        master.setApplicantUserId(userId);
        master.setCurrentStatus(sampleTransportClearanceEntity.getStatus());
        master.setSubmittedOn(LocalDateTime.now());
        return applicationMasterRepository.save(master);
    }

    private void createTask(ApplicationMaster master, SampleTransportClearanceEntity geoPhysicsApplication1, String role, Long assignedBy, Long assignedTo) {
        LocalDateTime now = LocalDateTime.now();

        TaskManagement task = new TaskManagement();
        task.setApplicationNumber(geoPhysicsApplication1.getApplicationNo());
        task.setServiceCode(SERVICE_CODE);
        task.setAssignedToRole(role);
        task.setAssignedByUserId(assignedBy);
        task.setAssignedToUserId(assignedTo);
        task.setAssignedAt(now);
        task.setTaskStatus(master.getCurrentStatus());
        task.setCreatedBy(assignedBy);

        taskManagementRepository.save(task);
        log.info("Created task for role {}", role);
    }

}