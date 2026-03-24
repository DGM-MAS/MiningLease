package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.TemporaryClosureNotificationResponse;
import com.mas.gov.bt.mas.primary.entity.*;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.exception.ResourceNotFoundException;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.mapper.TemporaryClosureMapper;
import com.mas.gov.bt.mas.primary.repository.*;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TemporaryClosureService {

    private static final String SERVICE_CODE = "TEMPORARY CLOSURE SERVICE";
    private static final int DEFAULT_TAT_DAYS = 2;

    private final TemporaryClosureRepository temporaryClosureRepository;
    private final TaskManagementRepository taskManagementRepository;
    private final ApplicationMasterRepository  applicationMasterRepository;
    private final NotificationClient notificationClient;
    private  final TemporaryClosureMapper TemporaryClosureMapper;
    private final MiningLeaseApplicationRepository miningLeaseApplicationRepository;
    private final ApplicationRevisionHistoryRepository revisionHistoryRepository;

    @Transactional
    public TemporaryClosureNotificationResponse submitApplication(@Valid TemporaryClosureNotificationRequest request, Long userId, String email, String applicantType) {
        Optional<MiningLeaseApplication> miningLeaseApplication = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationId());
        MiningLeaseApplication miningLeaseApplication1 = null;
        if (miningLeaseApplication.isPresent()) {
            miningLeaseApplication1 = miningLeaseApplication.get();
        }

        TemporaryClosureEntity temporaryClosureEntity = new TemporaryClosureEntity();
        temporaryClosureEntity.setSubmittedAt(LocalDateTime.now());
        temporaryClosureEntity.setCurrentStatus("SUBMITTED");
        temporaryClosureEntity.setApplicantUserId(userId);
        assert miningLeaseApplication1 != null;
        temporaryClosureEntity.setApplicantName(miningLeaseApplication1.getApplicantName());
        temporaryClosureEntity.setApplicantCid(miningLeaseApplication1.getApplicantCid());
        temporaryClosureEntity.setApplicantEmail(email);
        temporaryClosureEntity.setApplicantType(applicantType);
        temporaryClosureEntity.setApplicationId(request.getApplicationId());

        // Closure files, reason and duration of closure
        temporaryClosureEntity.setApplicantFileId(request.getFileId());
        temporaryClosureEntity.setReasonForClosure(request.getReasonForClosure());
        temporaryClosureEntity.setNumberOfMonthsForClosure(request.getNumberOfMonthsForClosure());
        temporaryClosureEntity.setRemarksApplicant(request.getRemarksApplicant());

        temporaryClosureRepository.save(temporaryClosureEntity);

        // =====================================================
        // 2. ASSIGN RC
        // =====================================================
        UserWorkloadProjection assignedRC = assignRC();

        // =====================================================
        // 3. Application master and create task for director
        // =====================================================
        ApplicationMaster master = miningLeaseApplication1.getApplicationMaster();
        master.setSubmittedAt(LocalDateTime.now());
        master.setCurrentStatus("SUBMITTED");
        master.setApplicantUserId(userId);
        master.setServiceCode(SERVICE_CODE);
        applicationMasterRepository.save(master);
        temporaryClosureEntity.setApplicationMaster(master);
        createTask(master,temporaryClosureEntity,"RC", userId, assignedRC.getUserId());

        if (assignedRC.getEmail() != null) {
            notificationClient.sendMailToDirectorAssigned(
                    assignedRC.getEmail(),
                    assignedRC.getUsername(),
                    temporaryClosureEntity.getApplicationId());
        }

        if(assignedRC.getUserId()!= null) {
            String title = "Temporary closure application has been assigned.";
            String message = "An application for temporary has been assigned for review. Application No. "+ temporaryClosureEntity.getApplicationId()+" Please login to review the temporary closure application.";
            String serviceId = "108";
            notificationClient.sendUserNotification(title, message, assignedRC.getUserId(), serviceId);
        }else {
            throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }

        return TemporaryClosureMapper.toResponse(temporaryClosureEntity);
    }

    @Transactional
    public UserWorkloadProjection assignRC() {

        UserWorkloadProjection rc =
                temporaryClosureRepository.findRCTemporaryClosure();

        if (rc == null) {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
        }
        return rc;
    }


    @Transactional
    private void createTask(ApplicationMaster master, TemporaryClosureEntity application, String role, Long userId, Long directorId) {
        LocalDateTime now = LocalDateTime.now();

        TaskManagement task = new TaskManagement();
        task.setApplicationNumber(application.getApplicationId());
        task.setServiceCode(SERVICE_CODE);
        task.setAssignedToRole(role);
        task.setAssignedByUserId(userId);
        task.setAssignedToUserId(directorId);
        task.setAssignedAt(now);
        task.setTaskStatus(master.getCurrentStatus());

        task.setDeadlineDate(now.plusDays(DEFAULT_TAT_DAYS));
        task.setCreatedBy(userId);

        taskManagementRepository.save(task);
        log.info("Created task for role {}", role);
    }

    /**
     * Get all applications (for Agency users).
     */
    @Transactional(readOnly = true)
    public Page<TemporaryClosureNotificationResponse> getAllApplications(Long currentUserId, Pageable pageable) {

        Page<TemporaryClosureEntity> applications =
                temporaryClosureRepository.findApplicationsAssignedToUser(currentUserId, pageable);
        return applications.map(TemporaryClosureMapper::toListResponse);
    }

    /**
     * Get applications for an applicant.
     */
    @Transactional(readOnly = true)
    public Page<TemporaryClosureNotificationResponse> getMyApplications(Long userId, Pageable pageable, String search) {
        List<String> ApplicationStatus = List.of(
                "SUBMITTED",
                "RC ASSIGNED",
                "RECTIFICATION BY RC");
        Page<TemporaryClosureEntity> applications;

        if (search == null || search.isBlank()) {
            applications = temporaryClosureRepository.findByApplicantUserIdAndStatusIn(
                    userId,
                    ApplicationStatus,
                    pageable);
        }
        else {

            applications = temporaryClosureRepository.findByAssignedToUserAndSearch(
                    userId,
                    search.trim(),
                    pageable
            );
        }
        return applications.map(TemporaryClosureMapper::toListResponse);
    }

    public SuccessResponse<List<TemporaryClosureNotificationResponse>> getAssignedToRC(Long userId, Pageable pageable, String search) {
        Page<TemporaryClosureEntity> page;

        if (search == null || search.isBlank()) {

            page = temporaryClosureRepository
                    .findAssignedToUserRC(userId, pageable);

        } else {

            page = temporaryClosureRepository
                    .findAssignedToUserAndSearchRC(
                            userId,
                            search.trim(),
                            pageable
                    );
        }

        Page<TemporaryClosureNotificationResponse> responsePage =
                page.map(TemporaryClosureMapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Transactional
    public TemporaryClosureNotificationResponse assignApplicationDirector(@Valid AssignedTaskRC request, Long userId) {
        TemporaryClosureEntity temporaryClosureEntity;

        temporaryClosureEntity = findApplicationById(request.getId());
        ApplicationMaster applicationMaster ;
        if(temporaryClosureEntity != null) {
            applicationMaster = temporaryClosureEntity.getApplicationMaster();
        }else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
        }
        if (request.getMIFocalId() != null ) {
            temporaryClosureEntity.setCurrentStatus("MI ASSIGNED");
            temporaryClosureEntity.setRemarksRC(request.getRemarksRC());
            applicationMaster.setCurrentStatus("MI ASSIGNED");
        }
        applicationMasterRepository.save(applicationMaster);
        temporaryClosureRepository.save(temporaryClosureEntity);

        if (request.getMIFocalId() != null) {
            createTask(
                    applicationMaster,
                    temporaryClosureEntity,
                    "MI",
                    userId,
                    request.getMIFocalId());

            UserWorkloadProjection userMI = temporaryClosureRepository.findUserDetailsMI(request.getMIFocalId());

            if (temporaryClosureEntity.getApplicantEmail() != null) {
                notificationClient.sendStatusUpdateNotification(
                        temporaryClosureEntity.getApplicantEmail(),
                        temporaryClosureEntity.getApplicantName(),
                        temporaryClosureEntity.getApplicationId(),
                        temporaryClosureEntity.getCurrentStatus(),
                        "MI ASSIGNED");
            }

            if (userMI != null) {
                notificationClient.sendAssignmentNotification(
                        userMI.getEmail(),
                        userMI.getUsername(),
                        temporaryClosureEntity.getApplicationId(),
                        "MI ASSIGNED");

                String title = "A new temporary closure application has been assigned.";
                String message = "A new temporary closure application has been assigned.";
                String serviceId = "108";
                notificationClient.sendUserNotification(title, message, userMI.getUserId(), serviceId);
            }
        }
        return TemporaryClosureMapper.toResponse(temporaryClosureEntity);
    }

    private TemporaryClosureEntity findApplicationById(Long id) {
        return temporaryClosureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));
    }

    @Transactional
    public TemporaryClosureNotificationResponse reviewApplicationRC(@Valid ReviewTemporaryClosureRCRequest request, Long userId) {
        log.info("Reviewing temporary closure application by RC user: {}", userId);

        TemporaryClosureEntity app = findApplicationById(request.getId());
        ApplicationMaster master = app.getApplicationMaster();
        if (request.getStatus() != null) {
            switch (request.getStatus()) {
                case "Approved" -> {
                    LocalDateTime now = LocalDateTime.now();
                    app.setCurrentStatus("APPROVED BY RC");
                    app.setRemarksRC(request.getRemarks());
                    app.setFileUploadIdRC(request.getFileUploadIdRC());
                    app.setRcReviewedAt(now);
                    app.setApprovedAt(now);

                    if (master != null) {
                        master.setCurrentStatus("APPROVED BY RC");
                        master.setApprovedAt(now);
                        master.setCompletedAt(now);
                        applicationMasterRepository.save(master);
                    }

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendApprovalNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationId());
                    }
                }
                case "Rectification" -> {
                    app.setCurrentStatus("RECTIFICATION BY RC");
                    app.setRemarksRC(request.getRemarks());
                    app.setRcReviewedAt(LocalDateTime.now());
                    app.setApplicantFileId(null);

                    createRevisionRecord(app, "RC REVIEWED", app.getCurrentStatus(), userId, request.getRemarks() );

                    if (master != null) {
                        master.setCurrentStatus("RECTIFICATION BY RC");
                        applicationMasterRepository.save(master);
                    }

                    if (app.getApplicantEmail() != null) {
                        assert master != null;
                        notificationClient.sendRevisionRequestNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationId(),
                                master.getCurrentStatus(),
                                request.getRemarks()
                                );
                    }
                    assert master != null;
                    createTask(master, app, "APPLICANT", userId, app.getApplicantUserId());
                }
                default -> throw new IllegalArgumentException("Application status not recognized");
            }
            temporaryClosureRepository.save(app);
        }
        return TemporaryClosureMapper.toResponse(app);
    }

    @Transactional
    public TemporaryClosureNotificationResponse reviewApplicationMI(@Valid ReviewTemporaryClosureMIRequest request, Long userId) {
        log.info("Reviewing temporary closure application by mining inspector user: {}", userId);

        TemporaryClosureEntity app = findApplicationById(request.getId());
        ApplicationMaster master = app.getApplicationMaster();

        if (request.getStatus() != null) {
            if (request.getStatus().equals("Review")) {
                LocalDateTime now = LocalDateTime.now();
                app.setCurrentStatus("MI REVIEWED");
                app.setRemarksMI(request.getRemarks());
                app.setFileIdMI(request.getFileId());
                app.setMiReviewedAt(now);
                app.setApprovedAt(now);

                List<TaskManagement> taskManagement = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(app.getApplicationId(), "MI ASSIGNED", "MI", SERVICE_CODE);
                Long RCUserId = null;
                if (taskManagement != null) {
                    TaskManagement taskManagement1 = taskManagement.getFirst();
                    RCUserId = taskManagement1.getAssignedToUserId();
                }

                if (master != null) {
                    master.setCurrentStatus("MI REVIEWED");
                    master.setApprovedAt(now);
                    master.setCompletedAt(now);
                    applicationMasterRepository.save(master);
                }

                if (app.getApplicantEmail() != null) {
                    notificationClient.sendApprovalNotification(
                            app.getApplicantEmail(),
                            app.getApplicantName(),
                            app.getApplicationId());
                }
                assert master != null;
                createTask(master, app, "RC", userId, RCUserId);
            } else {
                throw new IllegalArgumentException("Application status not recognized");
            }
            temporaryClosureRepository.save(app);
        }
        return TemporaryClosureMapper.toResponse(app);
    }

    public SuccessResponse<List<TemporaryClosureNotificationResponse>> getAssignedToMI(Long userId, Pageable pageable, String search) {
        Page<TemporaryClosureEntity> page;

        if (search == null || search.isBlank()) {

            page = temporaryClosureRepository
                    .findAssignedToUserMI(userId, pageable);

        } else {

            page = temporaryClosureRepository
                    .findAssignedToUserAndSearchMI(
                            userId,
                            search.trim(),
                            pageable
                    );
        }

        Page<TemporaryClosureNotificationResponse> responsePage =
                page.map(TemporaryClosureMapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Transactional
    public TemporaryClosureNotificationResponse submitRectification(@Valid TemporaryClosureNotificationRequest request, Long userId) {
        TemporaryClosureEntity temporaryClosureEntity = temporaryClosureRepository.findByApplicationId(request.getApplicationId());
        temporaryClosureEntity.setReasonForClosure(request.getReasonForClosure());
        temporaryClosureEntity.setApplicantFileId(request.getFileId());
        temporaryClosureEntity.setNumberOfMonthsForClosure(request.getNumberOfMonthsForClosure());
        temporaryClosureEntity.setRemarksApplicant(request.getRemarksApplicant());
        temporaryClosureEntity.setCurrentStatus("RESUBMITTED");

        temporaryClosureRepository.save(temporaryClosureEntity);

        ApplicationMaster applicationMaster = temporaryClosureEntity.getApplicationMaster();
        applicationMaster.setCurrentStatus("RESUBMITTED");
        applicationMasterRepository.save(applicationMaster);

        List<TaskManagement> taskManagement = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(request.getApplicationId(),"MI ASSIGNED", "MI", SERVICE_CODE);
        new TaskManagement();
        TaskManagement taskManagement1;
        Long MIUserId = null;
        if (taskManagement != null) {
            taskManagement1 = taskManagement.getFirst();
            MIUserId = taskManagement1.getAssignedToUserId();

            createTask(applicationMaster,temporaryClosureEntity, "RESUBMITTED", userId, MIUserId);
        }
        UserWorkloadProjection assignedMIDetails = temporaryClosureRepository.findUserDetailsMI(MIUserId);
        if(assignedMIDetails.getUserId() != null) {
            String title = "Mining lease application has been assigned for MLA review.";
            String message = "Mining lease application has been  assigned for MLA review.";
            String serviceId = "108";
            notificationClient.sendUserNotification(title, message, assignedMIDetails.getUserId(), serviceId);
        }

        return  TemporaryClosureMapper.toResponse(temporaryClosureRepository.save(temporaryClosureEntity));

    }

    @Transactional
    public void reassignTaskRC(@Valid ReassignTaskRequest request, Long userId) {
        List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(request.getApplicationNumber(),"SUBMITTED","RC", SERVICE_CODE);

        TaskManagement taskManagement = new TaskManagement();

        if (task != null) {
            taskManagement = task.getFirst();
        }

        taskManagement.setAssignedToUserId(request.getNewAssigneeUserId());
        taskManagement.setAssignedByUserId(userId);
        taskManagement.setAssignedAt(LocalDateTime.now());
        taskManagement.setReassignmentCount(taskManagement.getReassignmentCount() + 1);
        taskManagement.setActionRemarks(request.getRemarks());

        taskManagementRepository.save(taskManagement);

        UserWorkloadProjection userDetails = miningLeaseApplicationRepository.findUserDetails(request.getNewAssigneeUserId());
        notificationClient.sendTaskReassignmentNotification(
                userDetails.getEmail(), userDetails.getUsername(),
                taskManagement.getApplicationNumber(),
                taskManagement.getAssignedToRole());

        if(userDetails.getUserId()!= null) {
            String title = "An new application has been reassigned.";
            String message = "An application for temporary closure has been assigned for review. Application No. "+ request.getApplicationNumber()+" Please login in review the application";
            String serviceId = "108";
            notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId);
        }else {
            throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }

        log.info("RC task {} reassigned to user {}", taskManagement.getId(), request.getNewAssigneeUserId());

    }

    @Transactional(readOnly = true)
    public TemporaryClosureNotificationResponse getApplicationByNumber(String applicationNo) {
        TemporaryClosureEntity application = temporaryClosureRepository.findByApplicationId(applicationNo);

        if(application == null) {
            throw new RuntimeException(ErrorCodes.RECORD_NOT_FOUND);
        }

        return TemporaryClosureMapper.toResponse(application);
    }

    public SuccessResponse<List<TemporaryClosureNotificationResponse>> getAllApplicationAdmin(Pageable pageable, String search) {
        Page<TemporaryClosureEntity> page;

        if (search == null || search.isBlank()) {
            page = temporaryClosureRepository.findAll(pageable);
        } else {
            page = temporaryClosureRepository.findAllBySearch(search.trim(), pageable);
        }

        return SuccessResponse.fromPage("Applications fetched successfully", page.map(TemporaryClosureMapper::toResponse));
    }

    @Transactional
    public void reassignTaskMI(@Valid ReassignTaskRequest request, Long userId) {
        List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(request.getApplicationNumber(),"MI ASSIGNED","MI", SERVICE_CODE);

        TaskManagement taskManagement = new TaskManagement();

        if (task != null) {
            taskManagement = task.getFirst();
        }

        taskManagement.setAssignedToUserId(request.getNewAssigneeUserId());
        taskManagement.setAssignedByUserId(userId);
        taskManagement.setAssignedAt(LocalDateTime.now());
        taskManagement.setReassignmentCount(taskManagement.getReassignmentCount() + 1);
        taskManagement.setActionRemarks(request.getRemarks());

        taskManagementRepository.save(taskManagement);

        UserWorkloadProjection userDetails = miningLeaseApplicationRepository.findUserDetails(request.getNewAssigneeUserId());
        notificationClient.sendTaskReassignmentNotification(
                userDetails.getEmail(), userDetails.getUsername(),
                taskManagement.getApplicationNumber(),
                taskManagement.getAssignedToRole());

        if(userDetails.getUserId()!= null) {
            String title = "An new application has been reassigned.";
            String message = "An application for temporary closure has been assigned for review. Application No. "+ request.getApplicationNumber()+" Please login in review the application";
            String serviceId = "108";
            notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId);
        }else {
            throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }

        log.info("MI task {} reassigned to user {}", taskManagement.getId(), request.getNewAssigneeUserId());
    }


    @Transactional
    private void createRevisionRecord(TemporaryClosureEntity miningLeaseApplication, String geologistReview, String geologistRemarks, Long userId, String remarks) {
        long revisionCount = revisionHistoryRepository.countByApplicationIdAndRevisionStage(miningLeaseApplication.getId(), geologistReview);

        ApplicationRevisionHistory revision = ApplicationRevisionHistory.builder()
                .applicationId(miningLeaseApplication.getId())
                .applicationNumber(miningLeaseApplication.getApplicationId())
                .revisionStage(geologistReview)
                .revisionNumber((int) revisionCount + 1)
                .remarks(remarks)
                .requestedBy(userId)
                .requestedAt(LocalDateTime.now())
                .status(geologistRemarks)
                .createdBy(userId)
                .build();

        revisionHistoryRepository.save(revision);
        log.info("Created revision record #{} for application {} at stage {}", revision.getRevisionNumber(), miningLeaseApplication.getApplicationId(), geologistReview);
    }

    public Page<TemporaryClosureNotificationResponse> getArchivedApplications(Pageable pageable, String search, Long userId) {
        List<String> archivedStatuses = List.of("APPROVED BY RC");
        Page<TemporaryClosureEntity> applications;

        if (search == null || search.isBlank()) {
            applications = temporaryClosureRepository.findArchivedAssignedToUser(
                    userId,
                    archivedStatuses,
                    pageable);
        }
        else {

            applications = temporaryClosureRepository.findArchivedAssignedToUserAndSearch(
                    userId,
                    search.trim(),
                    pageable
            );
        }
        return applications.map(TemporaryClosureMapper::toListResponse);
    }

    public Page<TemporaryClosureNotificationResponse> getMyArchivedApplications(Long userId, Pageable pageable, String search) {
        List<String> archivedStatuses = List.of("APPROVED BY RC");
        Page<TemporaryClosureEntity> applications ;

        if (search == null || search.isBlank()) {
            applications =  temporaryClosureRepository.findByApplicantUserIdAndStatusIn(userId, archivedStatuses, pageable);
        } else {
            applications = temporaryClosureRepository.findByApplicantUserIdAndSearch(userId, archivedStatuses, search.trim(), pageable);
        }

        return applications.map(TemporaryClosureMapper::toListResponse);
    }
}
