package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.ImmediateSuspensionApplicationResponse;
import com.mas.gov.bt.mas.primary.entity.*;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.exception.ResourceNotFoundException;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.mapper.ImmediateSuspensionMapper;
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
public class ImmediateSuspensionService {

    private static final String SERVICE_CODE = "IMMEDIATE_SUSPENSION";
    private static final int DEFAULT_TAT_DAYS = 2;

    private final ImmediateSuspensionApplicationRepository immediateSuspensionApplicationRepository;

    private final MiningLeaseApplicationRepository miningLeaseApplicationRepository;

    private final QuarryLeaseApplicationRepository quarryLeaseApplicationRepository;

    private final ApplicationMasterRepository applicationMasterRepository;

    private final TaskManagementRepository taskManagementRepository;

    private final NotificationClient notificationClient;

    private final ImmediateSuspensionMapper immediateSuspensionMapper;

    private final ImmediateSuspensionReasonRepository immediateSuspensionReasonRepository;

    private final TerminationApplicationRepository terminationApplicationRepository;

    private final SurfaceCollectionPermitRepository surfaceCollectionPermitRepository;

    @Transactional
    public ImmediateSuspensionApplicationResponse submitImmediateSuspensionApplication(@Valid ImmediateSuspensionApplicationRequest request, Long userId) {
        switch (request.getApplicationFrom().toUpperCase()) {

            case "M":
                return processMiningSuspension(request, userId);

            case "Q":
                return processQuarrySuspension(request, userId);

            case "S":
                return processSurfaceCollectionSuspension(request, userId);

//            case "SL":
//                return processStockLiftingSuspension(request, userId);

            default:
                throw new BusinessException(ErrorCodes.BAD_REQUEST);
        }
    }

    // Used to process Mining lease application during application submission
    @Transactional
    private ImmediateSuspensionApplicationResponse processMiningSuspension(
            ImmediateSuspensionApplicationRequest request, Long userId) {

        MiningLeaseApplication miningLeaseApplication =
                miningLeaseApplicationRepository
                        .findByApplicationNumber(request.getApplicationNumber())
                        .orElseThrow(() -> new BusinessException(ErrorCodes.BAD_REQUEST));

        if (!"MINING LEASE APPROVED".equalsIgnoreCase(miningLeaseApplication.getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.BUSINESS_RULE_VIOLATION);
        }

        ImmediateSuspensionApplication suspension =
                buildSuspensionApplication(request, userId,
                        miningLeaseApplication.getApplicantUserId(),
                        miningLeaseApplication.getApplicantName(),
                        miningLeaseApplication.getApplicantCid(),
                        miningLeaseApplication.getApplicantEmail(),
                        miningLeaseApplication.getApplicationNumber());

        ApplicationMaster master = updateApplicationMaster(miningLeaseApplication.getApplicationMaster(), userId);

        suspension.setApplicationMaster(master);
        immediateSuspensionApplicationRepository.save(suspension);

        miningLeaseApplication.setCurrentStatus("Under-Review-Suspension");
        miningLeaseApplicationRepository.save(miningLeaseApplication);

        createTask(master, suspension, "APPLICANT", userId, miningLeaseApplication.getApplicantUserId());

        sendNotifications(miningLeaseApplication.getApplicantEmail(),
                miningLeaseApplication.getApplicantName(),
                miningLeaseApplication.getApplicantUserId(),
                request.getApplicationNumber());

        return immediateSuspensionMapper.toResponse(suspension);
    }

    // Used to process Quarrying lease application during application submission
    @Transactional
    private ImmediateSuspensionApplicationResponse processQuarrySuspension(
            ImmediateSuspensionApplicationRequest request, Long userId) {

        QuarryLeaseApplication quarryLeaseApplication =
                quarryLeaseApplicationRepository
                        .findByApplicationNumber(request.getApplicationNumber())
                        .orElseThrow(() -> new BusinessException(ErrorCodes.BAD_REQUEST));

        if (!"QUARRY LEASE APPROVED".equalsIgnoreCase(quarryLeaseApplication.getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.BUSINESS_RULE_VIOLATION);
        }

        ImmediateSuspensionApplication suspension =
                buildSuspensionApplication(request, userId,
                        quarryLeaseApplication.getApplicantUserId(),
                        quarryLeaseApplication.getApplicantName(),
                        quarryLeaseApplication.getApplicantCid(),
                        quarryLeaseApplication.getApplicantEmail(),
                        quarryLeaseApplication.getApplicationNumber());

        ApplicationMaster master = updateApplicationMaster(quarryLeaseApplication.getApplicationMaster(), userId);

        suspension.setApplicationMaster(master);
        immediateSuspensionApplicationRepository.save(suspension);

        quarryLeaseApplication.setCurrentStatus("Under-Review-Suspension");
        quarryLeaseApplicationRepository.save(quarryLeaseApplication);

        createTask(master, suspension, "APPLICANT", userId, quarryLeaseApplication.getApplicantUserId());

        sendNotifications(quarryLeaseApplication.getApplicantEmail(),
                quarryLeaseApplication.getApplicantName(),
                quarryLeaseApplication.getApplicantUserId(),
                request.getApplicationNumber());

        return immediateSuspensionMapper.toResponse(suspension);
    }

    // Used to process Quarrying lease application during application submission
    @Transactional
    private ImmediateSuspensionApplicationResponse processSurfaceCollectionSuspension(
            ImmediateSuspensionApplicationRequest request, Long userId) {

        SurfaceCollectionPermitEntity surfaceCollectionPermitEntity =
                surfaceCollectionPermitRepository
                        .findByApplicationNo(request.getApplicationNumber())
                        .orElseThrow(() -> new BusinessException(ErrorCodes.BAD_REQUEST));

        if (!"PERMIT_ISSUED".equalsIgnoreCase(surfaceCollectionPermitEntity.getStatus())) {
            throw new BusinessException(ErrorCodes.BUSINESS_RULE_VIOLATION);
        }

        ImmediateSuspensionApplication suspension =
                buildSuspensionApplication(request, userId,
                        surfaceCollectionPermitEntity.getCreatedBy(),
                        surfaceCollectionPermitEntity.getApplicantName(),
                        surfaceCollectionPermitEntity.getApplicantCid(),
                        surfaceCollectionPermitEntity.getEmail(),
                        surfaceCollectionPermitEntity.getApplicationNo());

        Optional<ApplicationMaster> applicationMaster = applicationMasterRepository.findByApplicationNumberAndServiceCode(request.getApplicationNumber(), "SURFACE_COLLECTION_PERMIT");

        ApplicationMaster applicationMasterEntity = applicationMaster.orElseThrow(() -> new BusinessException(ErrorCodes.BAD_REQUEST));

        ApplicationMaster master = updateApplicationMaster(applicationMasterEntity, userId);

        suspension.setApplicationMaster(master);
        immediateSuspensionApplicationRepository.save(suspension);

        surfaceCollectionPermitEntity.setStatus("Under-Review-Suspension");
        surfaceCollectionPermitRepository.save(surfaceCollectionPermitEntity);

        createTask(master, suspension, "APPLICANT", userId, surfaceCollectionPermitEntity.getCreatedBy());

        sendNotifications(surfaceCollectionPermitEntity.getEmail(),
                surfaceCollectionPermitEntity.getApplicantName(),
                surfaceCollectionPermitEntity.getCreatedBy(),
                request.getApplicationNumber());

        return immediateSuspensionMapper.toResponse(suspension);
    }

    // Immediate Suspension data saving part where data from
    // 1. Quarry
    // 2. Mining
    // 3. Stock lifting
    // 4. Surface collection can be saved
    private ImmediateSuspensionApplication buildSuspensionApplication(
            ImmediateSuspensionApplicationRequest request,
            Long userId,
            Long promoterId,
            String applicantName,
            String cid,
            String email,
            String applicationNumber) {

        ImmediateSuspensionApplication suspension = new ImmediateSuspensionApplication();

        suspension.setApplicationNumber(applicationNumber);
        suspension.setApplicationFrom(request.getApplicationFrom());
        suspension.setPromoterUserId(promoterId);
        suspension.setApplicantName(applicantName);
        suspension.setApplicantCid(cid);
        suspension.setApplicantEmail(email);
        suspension.setRemarksRcMi(request.getRcMiRemark());
        suspension.setCreatedBy(userId);
        suspension.setCreatedAt(LocalDateTime.now());
        suspension.setCurrentStatus("SUBMITTED");

        ImmediateSuspensionReasonMaster reason =
                immediateSuspensionReasonRepository
                        .findById(request.getSuspensionReasonId())
                        .orElseThrow(() -> new BusinessException(ErrorCodes.BAD_REQUEST));

        suspension.setSuspensionReasonMaster(reason);
        suspension.setSuspensionReasonId(reason.getId());

        return suspension;
    }

    // Application master table would be updated when a new action is being taken
    private ApplicationMaster updateApplicationMaster(ApplicationMaster master, Long userId) {

        master.setSubmittedAt(LocalDateTime.now());
        master.setCurrentStatus("SUBMITTED");
        master.setApplicantUserId(userId);
        master.setServiceCode(SERVICE_CODE);

        return applicationMasterRepository.save(master);
    }

    // Notification logic
    private void sendNotifications(String email, String name, Long userId, String appNo) {

        if (email != null) {
            notificationClient.sendMiningLeaseMailToDirectorAssigned(
                    email,
                    name,
                    appNo);
        }

        if (userId != null) {
            String title = "Immediate Suspension application has been issued related to your lease.";
            String message = "Application No. " + appNo;

            notificationClient.sendUserNotification(title, message, userId, "116");
        }
    }

    @Transactional
    private void createTask(ApplicationMaster master, ImmediateSuspensionApplication application, String role, Long userId, Long directorId) {
        LocalDateTime now = LocalDateTime.now();

        TaskManagement task = new TaskManagement();
        task.setApplicationNumber(application.getApplicationNumber());
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

    @Transactional(readOnly = true)
    public SuccessResponse<List<ImmediateSuspensionApplicationResponse>> getAssignedToPromoter(Long userId, Pageable pageable, String search) {
        Page<ImmediateSuspensionApplication> page;

        if (search == null || search.isBlank()) {

            page = immediateSuspensionApplicationRepository
                    .findAssignedToUserPromoter(userId, pageable);

        } else {

            page = immediateSuspensionApplicationRepository
                    .findAssignedToUserAndSearchPromoter(
                            userId,
                            search.trim(),
                            pageable
                    );
        }

        Page<ImmediateSuspensionApplicationResponse> responsePage =
                page.map(immediateSuspensionMapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    public ImmediateSuspensionApplicationResponse reviewApplicationPromoter(@Valid PromoterImmediateSuspensionRequest request, Long userId) {
        log.info("Reviewing Termination application by Promoter: {}", userId);

        ImmediateSuspensionApplication app = findApplicationById(request.getId());
        ApplicationMaster master = app.getApplicationMaster();

        if (request.getStatus() != null) {

            if (request.getStatus().equals("Rectification")) {
                LocalDateTime now = LocalDateTime.now();
                app.setCurrentStatus("RECTIFICATION BY PROMOTER");
                app.setPromoterReviewedAt(now);
                app.setPromoterRemarks(request.getRemarks());
                app.setPromoterFileId(request.getFileId());

                if (master != null) {
                    master.setCurrentStatus("RECTIFICATION BY PROMOTER");
                    applicationMasterRepository.save(master);
                }

                if (app.getApplicantEmail() != null) {
                    notificationClient.sendImmediateSuspensionRevisionRequestPromoterNotification(
                            app.getApplicantEmail(),
                            app.getApplicantName(),
                            app.getApplicationNumber(),
                            app.getCurrentStatus(),
                            "Rectification Submitted");
                }

                assert master != null;
                createTask(master, app, "RC/MI", userId, app.getCreatedBy());
            } else {
                throw new IllegalArgumentException("Application status not recognized");
            }
            immediateSuspensionApplicationRepository.save(app);
        }
        return immediateSuspensionMapper.toResponse(app);
    }

    private ImmediateSuspensionApplication findApplicationById(Long id) {
        return immediateSuspensionApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));
    }

    public ImmediateSuspensionApplicationResponse assignApplicationDirector(@Valid AssignedTaskRC request, Long userId) {
        ImmediateSuspensionApplication immediateSuspensionApplication;

        immediateSuspensionApplication = findApplicationById(request.getId());
        ApplicationMaster applicationMaster ;
        if(immediateSuspensionApplication != null) {
            applicationMaster = immediateSuspensionApplication.getApplicationMaster();
        }else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
        }
        if (request.getMIFocalId() != null ) {
            immediateSuspensionApplication.setCurrentStatus("MI ASSIGNED");
            immediateSuspensionApplication.setRemarksRcMi(request.getRemarksRC());
            applicationMaster.setCurrentStatus("MI ASSIGNED");
        }
        applicationMasterRepository.save(applicationMaster);
        immediateSuspensionApplicationRepository.save(immediateSuspensionApplication);

        if (request.getMIFocalId() != null) {
            createTask(
                    applicationMaster,
                    immediateSuspensionApplication,
                    "MI",
                    userId,
                    request.getMIFocalId());

            UserWorkloadProjection userMI = immediateSuspensionApplicationRepository.findUserDetailsMI(request.getMIFocalId());

            if (immediateSuspensionApplication.getApplicantEmail() != null) {
                notificationClient.sendStatusUpdateNotification(
                        immediateSuspensionApplication.getApplicantEmail(),
                        immediateSuspensionApplication.getApplicantName(),
                        immediateSuspensionApplication.getApplicationNumber(),
                        immediateSuspensionApplication.getCurrentStatus(),
                        "MI ASSIGNED");
            }

            if (userMI != null) {
                notificationClient.sendAssignmentNotification(
                        userMI.getEmail(),
                        userMI.getUsername(),
                        immediateSuspensionApplication.getApplicationNumber(),
                        "MI ASSIGNED");

                String title = "A new immediate suspension application has been assigned.";
                String message = "A new immediate suspension application has been assigned.";
                String serviceId = "116";
                notificationClient.sendUserNotification(title, message, userMI.getUserId(), serviceId);
            }
        }
        return immediateSuspensionMapper.toResponse(immediateSuspensionApplication);

    }

    public SuccessResponse<List<ImmediateSuspensionApplicationResponse>> getAssignedToMI(Long userId, Pageable pageable, String search) {
        Page<ImmediateSuspensionApplication> page;

        if (search == null || search.isBlank()) {

            page = immediateSuspensionApplicationRepository
                    .findAssignedToUserMI(userId, pageable);

        } else {

            page = immediateSuspensionApplicationRepository
                    .findAssignedToUserAndSearchMI(
                            userId,
                            search.trim(),
                            pageable
                    );
        }

        Page<ImmediateSuspensionApplicationResponse> responsePage =
                page.map(immediateSuspensionMapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    public ImmediateSuspensionApplicationResponse reviewApplicationMI(@Valid PromoterImmediateSuspensionRequest request, Long userId) {
        log.info("Reviewing Termination application by MI: {}", userId);

        ImmediateSuspensionApplication app = findApplicationById(request.getId());
        ApplicationMaster master = app.getApplicationMaster();

        if (request.getStatus() != null) {

            if (request.getStatus().equals("Rectification")) {
                LocalDateTime now = LocalDateTime.now();
                app.setCurrentStatus("RECTIFICATION NEEDED");
                app.setMiReviewedAt(now);
                app.setMiRemarks(request.getRemarks());
                app.setMiFileId(request.getFileId());
                app.setPromoterFileId(null);

                if (master != null) {
                    master.setCurrentStatus("RECTIFICATION NEEDED");
                    applicationMasterRepository.save(master);
                }

                if (app.getApplicantEmail() != null) {
                    notificationClient.sendImmediateSuspensionRevisionRequestPromoterNotification(
                            app.getApplicantEmail(),
                            app.getApplicantName(),
                            app.getApplicationNumber(),
                            app.getCurrentStatus(),
                            "Rectification Needed");
                }

                assert master != null;
                createTask(master, app, "APPLICANT", userId, app.getPromoterUserId());
            } else if (request.getStatus().equals("Lifting")) {
                LocalDateTime now = LocalDateTime.now();
                app.setCurrentStatus("SUSPENSION LIFTING");
                app.setMiReviewedAt(now);
                app.setMiFileId(request.getFileId());
                app.setMiRemarks(request.getRemarks());

                if (master != null) {
                    master.setCurrentStatus("SUSPENSION LIFTING");
                    applicationMasterRepository.save(master);
                }

                if (app.getApplicantEmail() != null) {
                    notificationClient.sendUpliftingSuspensionNotification(
                            app.getApplicantEmail(),
                            app.getApplicantName(),
                            app.getApplicationNumber());
                }

                assert master != null;
                createTask(master, app, "APPLICANT", userId, app.getCreatedBy());
            } else {
                throw new IllegalArgumentException("Application status not recognized");
            }
            immediateSuspensionApplicationRepository.save(app);
        }
        return immediateSuspensionMapper.toResponse(app);
    }

    @Transactional
    public ImmediateSuspensionApplicationResponse reviewApplicationRCME(@Valid RcMeImmediateSuspensionRequest request, Long userId) {
        log.info("Reviewing Termination application by RC/ME: {}", userId);

        ImmediateSuspensionApplication app = findApplicationById(request.getId());
        ApplicationMaster master = app.getApplicationMaster();

        if (request.getStatus() != null) {

            switch (request.getStatus()) {
                case "Suspended" -> {
                    LocalDateTime now = LocalDateTime.now();
                    app.setCurrentStatus("SUSPENDED");
                    app.setPromoterReviewedAt(now);

                    if (app.getApplicationFrom().equalsIgnoreCase("M")) {
                        Optional<MiningLeaseApplication> miningLeaseApplication =
                                miningLeaseApplicationRepository.findByApplicationNumber(app.getApplicationNumber());

                        if (miningLeaseApplication.isEmpty()) {
                            throw new RuntimeException("Invalid application number: " + app.getApplicationNumber());
                        }

                        MiningLeaseApplication miningLeaseApplication1 = miningLeaseApplication.get();

                        miningLeaseApplication1.setCurrentStatus("SUSPENDED");
                        miningLeaseApplicationRepository.save(miningLeaseApplication1);
                    } else if (app.getApplicationFrom().equalsIgnoreCase("Q")) {
                        Optional<QuarryLeaseApplication> application =
                                quarryLeaseApplicationRepository.findByApplicationNumber(app.getApplicationNumber());

                        if (application.isEmpty()) {
                            throw new RuntimeException("Invalid application number: " + app.getApplicationNumber());
                        }

                        QuarryLeaseApplication quarryLeaseApplication = application.get();
                        quarryLeaseApplication.setCurrentStatus("SUSPENDED");
                        quarryLeaseApplicationRepository.save(quarryLeaseApplication);
                    }

                    if (master != null) {
                        master.setCurrentStatus("SUSPENDED");
                        applicationMasterRepository.save(master);
                    }

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendApplicationSuspendedNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber());
                    }

                    assert master != null;
                    createTask(master, app, "APPLICANT", userId, app.getPromoterUserId());
                }
                case "Rectification" -> {
                    LocalDateTime now = LocalDateTime.now();
                    app.setCurrentStatus("RECTIFICATION NEEDED");
                    app.setRcMiReviewedAt(now);
                    app.setRemarksRcMi(request.getRemarks());
                    app.setPromoterFileId(null);

                    if (master != null) {
                        master.setCurrentStatus("RECTIFICATION NEEDED");
                        applicationMasterRepository.save(master);
                    }

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendImmediateSuspensionRevisionRequestPromoterNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber(),
                                app.getCurrentStatus(),
                                "Rectification Needed");
                    }

                    assert master != null;
                    createTask(master, app, "APPLICANT", userId, app.getPromoterUserId());
                }
                case "Lifting" -> {
                    LocalDateTime now = LocalDateTime.now();
                    app.setCurrentStatus("SUSPENSION LIFTED");
                    app.setPromoterReviewedAt(now);

                    if (master != null) {
                        master.setCurrentStatus("SUSPENSION LIFTED");
                        applicationMasterRepository.save(master);
                    }

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendUpliftingSuspensionNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber());
                    }

                    assert master != null;
                    createTask(master, app, "APPLICANT", userId, app.getCreatedBy());
                }
                default -> throw new IllegalArgumentException("Application status not recognized");
            }
            immediateSuspensionApplicationRepository.save(app);
        }
        return immediateSuspensionMapper.toResponse(app);
    }

    public SuccessResponse<List<ImmediateSuspensionApplicationResponse>> getAllApplications(Long currentUserId, Pageable pageable, String search) {
        Page<ImmediateSuspensionApplication> page;

        if (search == null || search.isBlank()) {

            page = immediateSuspensionApplicationRepository
                    .findAssignedToUserRCME(currentUserId, pageable);

        } else {

            page = immediateSuspensionApplicationRepository
                    .findAssignedToUserAndSearchRCME(
                            currentUserId,
                            search.trim(),
                            pageable
                    );
        }

        Page<ImmediateSuspensionApplicationResponse> responsePage =
                page.map(immediateSuspensionMapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    public Page<ImmediateSuspensionApplicationResponse> getArchivedApplications(Pageable pageable, String search, Long userId) {
        List<String> archivedStatuses = List.of("SUSPENSION LIFTED", "SUSPENDED");
        Page<ImmediateSuspensionApplication> applications;
        List<Long> role_id = terminationApplicationRepository.findUserDetails(userId);
        if (role_id != null && role_id.contains(22L)){

            if (search == null || search.isBlank()) {
                applications = immediateSuspensionApplicationRepository.findArchivedAssignedToUserMI(
                        userId,
                        archivedStatuses,
                        pageable);
            } else {

                applications = immediateSuspensionApplicationRepository.findArchivedAssignedToUserAndSearchMI(
                        userId,
                        search.trim(),
                        archivedStatuses,
                        pageable
                );
            }

        }else {
            if (search == null || search.isBlank()) {
                applications = immediateSuspensionApplicationRepository.findArchivedAssignedToUser(
                        userId,
                        archivedStatuses,
                        pageable);
            }
            else {

                applications = immediateSuspensionApplicationRepository.findArchivedAssignedToUserAndSearch(
                        userId,
                        search.trim(),
                        archivedStatuses,
                        pageable
                );
            }
        }
        return applications.map(immediateSuspensionMapper::toListResponse);
    }

    public Page<ImmediateSuspensionApplicationResponse> getMyArchivedApplications(Long userId, Pageable pageable, String search) {
        List<String> archivedStatuses = List.of("CMS HEAD APPROVED", "TERMINATION CANCELED");
        Page<ImmediateSuspensionApplication> applications ;

        if (search == null || search.isBlank()) {
            applications =  immediateSuspensionApplicationRepository.findByApplicantUserIdAndStatusIn(userId, archivedStatuses, pageable);
        } else {
            applications = immediateSuspensionApplicationRepository.findByApplicantUserIdAndSearch(userId, archivedStatuses, search.trim(), pageable);
        }

        return applications.map(immediateSuspensionMapper::toListResponse);
    }

    public SuccessResponse<List<ImmediateSuspensionApplicationResponse>> getAllApplicationAdmin(Pageable pageable, String search) {
        Page<ImmediateSuspensionApplication> page;

        if (search == null || search.isBlank()) {
            page = immediateSuspensionApplicationRepository.findAll(pageable);
        } else {
            page = immediateSuspensionApplicationRepository.findAllBySearch(search.trim(), pageable);
        }

        return SuccessResponse.fromPage("Applications fetched successfully", page.map(immediateSuspensionMapper::toResponse));
    }

    public ImmediateSuspensionApplicationResponse getApplicationByNumber(String applicationNo) {
        ImmediateSuspensionApplication application = immediateSuspensionApplicationRepository.findByApplicationNumber(applicationNo)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationNo));

        return immediateSuspensionMapper.toResponse(application);
    }

    public ImmediateSuspensionApplicationResponse assignApplicationMI(@Valid ReAssignedTaskMI request, Long userId) {
        ImmediateSuspensionApplication immediateSuspensionApplication;

        immediateSuspensionApplication = findApplicationById(request.getId());
        ApplicationMaster applicationMaster ;
        if(immediateSuspensionApplication != null) {
            applicationMaster = immediateSuspensionApplication.getApplicationMaster();
        }else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
        }
        if (request.getMIFocalId() != null ) {
            immediateSuspensionApplication.setCurrentStatus("MI ASSIGNED");
            immediateSuspensionApplication.setMiRemarks(request.getRemarksMI());
            applicationMaster.setCurrentStatus("MI ASSIGNED");
        }
        applicationMasterRepository.save(applicationMaster);
        immediateSuspensionApplicationRepository.save(immediateSuspensionApplication);

        List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(immediateSuspensionApplication.getApplicationNumber(),"MI ASSIGNED","MI", SERVICE_CODE);

        TaskManagement taskManagement = new TaskManagement();

        if (task != null) {
            taskManagement = task.getFirst();
        }

        taskManagement.setAssignedToUserId(request.getMIFocalId());
        taskManagement.setAssignedByUserId(userId);
        taskManagement.setAssignedAt(LocalDateTime.now());
        taskManagement.setReassignmentCount(taskManagement.getReassignmentCount() + 1);
        taskManagement.setActionRemarks(request.getRemarksMI());

        taskManagementRepository.save(taskManagement);

        UserWorkloadProjection userMI = immediateSuspensionApplicationRepository.findUserDetailsMI(request.getMIFocalId());

            if (immediateSuspensionApplication.getApplicantEmail() != null) {
                notificationClient.sendStatusUpdateNotification(
                        immediateSuspensionApplication.getApplicantEmail(),
                        immediateSuspensionApplication.getApplicantName(),
                        immediateSuspensionApplication.getApplicationNumber(),
                        immediateSuspensionApplication.getCurrentStatus(),
                        "ME ASSIGNED");
            }

            if (userMI != null) {
                notificationClient.sendAssignmentNotification(
                        userMI.getEmail(),
                        userMI.getUsername(),
                        immediateSuspensionApplication.getApplicationNumber(),
                        "ME ASSIGNED");

                String title = "A new immediate suspension application has been assigned.";
                String message = "A new immediate suspension application has been assigned.";
                String serviceId = "116";
                notificationClient.sendUserNotification(title, message, userMI.getUserId(), serviceId);
            }
        return immediateSuspensionMapper.toResponse(immediateSuspensionApplication);

    }
}
