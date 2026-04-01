package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.ReassignTaskRequest;
import com.mas.gov.bt.mas.primary.dto.request.ReviewTerminationApplicationCMSHead;
import com.mas.gov.bt.mas.primary.dto.request.TerminationApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.response.TerminationApplicationResponse;
import com.mas.gov.bt.mas.primary.entity.*;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.exception.ResourceNotFoundException;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.mapper.TerminationMapper;
import com.mas.gov.bt.mas.primary.repository.ApplicationMasterRepository;
import com.mas.gov.bt.mas.primary.repository.MiningLeaseApplicationRepository;
import com.mas.gov.bt.mas.primary.repository.TaskManagementRepository;
import com.mas.gov.bt.mas.primary.repository.TerminationApplicationRepository;
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
import java.time.Year;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TerminationService {

    private static final String SERVICE_CODE = "TERMINATION_SERVICE";
    private static final int DEFAULT_TAT_DAYS = 2;

    private final TerminationApplicationRepository terminationApplicationRepository;

    private final ApplicationMasterRepository applicationMasterRepository;

    private final TaskManagementRepository taskManagementRepository;

    private final NotificationClient notificationClient;

    private final TerminationMapper terminationMapper;

    private final MiningLeaseApplicationRepository miningLeaseApplicationRepository;

    @Transactional
    public List<TerminationApplicationResponse> submitTerminationApplication(
            @Valid TerminationApplicationRequest request, Long userId) {

        String terminationId = generateTerminationId();

        List<TerminationApplicationResponse> responseList = new ArrayList<>();

        // ✅ Assign once (better design)
        UserWorkloadProjection assignedCMSHead = assignCMSHead();

        for (String appNo : request.getApplicationNumber()) {

            Optional<MiningLeaseApplication> miningLeaseApplication =
                    miningLeaseApplicationRepository.findByApplicationNumber(appNo);

            if (miningLeaseApplication.isEmpty()) {
                throw new RuntimeException("Invalid application number: " + appNo);
            }

            MiningLeaseApplication miningLeaseApplication1 = miningLeaseApplication.get();

            TerminationApplicationEntity entity = new TerminationApplicationEntity();

            entity.setTerminationId(terminationId);
            entity.setApplicationNumber(appNo);
            entity.setPromoterUserId(request.getPromoterUserId());
            entity.setFileId(request.getFileId());
            entity.setCreatedBy(userId);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setRemarksChief(request.getRemarksChief());
            entity.setCurrentStatus("SUBMITTED");

            entity.setApplicantName(miningLeaseApplication1.getApplicantName());
            entity.setApplicantEmail(miningLeaseApplication1.getApplicantEmail());

            // Application master
            ApplicationMaster master = miningLeaseApplication1.getApplicationMaster();
            master.setSubmittedAt(LocalDateTime.now());
            master.setCurrentStatus("SUBMITTED");
            master.setApplicantUserId(userId);
            master.setServiceCode(SERVICE_CODE);
            applicationMasterRepository.save(master);

            entity.setApplicationMaster(master);

            terminationApplicationRepository.save(entity);

            // Task creation
            createTask(master, entity, "CMS HEAD", userId, assignedCMSHead.getUserId());

            // Notifications
            if (assignedCMSHead.getEmail() != null) {
                notificationClient.sendTerminationMailToCMSHeadAssigned(
                        assignedCMSHead.getEmail(),
                        assignedCMSHead.getUsername(),
                        entity.getApplicationNumber());
            }

            if (assignedCMSHead.getUserId() != null) {
                String title = "Termination application has been assigned.";
                String message = "Application No. " + entity.getApplicationNumber() + " assigned for review.";
                String serviceId = "112";
                notificationClient.sendUserNotification(title, message, assignedCMSHead.getUserId(), serviceId);
            }

            // ✅ Add to response list
            responseList.add(terminationMapper.toResponse(entity));
        }

        return responseList;
    }

    @Transactional(readOnly = true)
    private UserWorkloadProjection assignCMSHead() {
        UserWorkloadProjection cmsHead =
                terminationApplicationRepository.findCMSTermination();

        if (cmsHead == null) {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
        }
        return cmsHead;
    }


    @Transactional
    private void createTask(ApplicationMaster master, TerminationApplicationEntity application, String role, Long userId, Long directorId) {
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

    public SuccessResponse<List<TerminationApplicationResponse>> getAssignedToCMSHead(Long userId, Pageable pageable, String search) {
        Page<TerminationApplicationEntity> page;

        if (search == null || search.isBlank()) {

            page = terminationApplicationRepository
                    .findAssignedToUserMI(userId, pageable);

        } else {

            page = terminationApplicationRepository
                    .findAssignedToUserAndSearchMI(
                            userId,
                            search.trim(),
                            pageable
                    );
        }

        Page<TerminationApplicationResponse> responsePage =
                page.map(terminationMapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Transactional
    public void reassignTaskCMS(@Valid ReassignTaskRequest request, Long userId) {
        List<String> assignedRoles = new ArrayList<>();
        assignedRoles.add("CMS HEAD");
        List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleInAndServiceCode(request.getApplicationNumber(),"SUBMITTED",assignedRoles, SERVICE_CODE);

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
        notificationClient.sendTaskReassignmentNotificationTermination(
                userDetails.getEmail(),
                userDetails.getUsername(),
                taskManagement.getApplicationNumber(),
                taskManagement.getAssignedToRole());

        if(userDetails.getUserId()!= null) {
            String title = "An new application has been reassigned.";
            String message = "An application for termination has been assigned for review. Application No. "+request.getApplicationNumber()+" Please login to review the application";
            String serviceId = "112";
            notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId);
        }else {
            throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }


        log.info("CMS Head task {} reassigned to user {}", taskManagement.getId(), request.getNewAssigneeUserId());
    }

    @Transactional
    public TerminationApplicationResponse reviewApplicationCMSHead(@Valid ReviewTerminationApplicationCMSHead request, Long userId) {
        log.info("Reviewing Termination application by CMS Head user: {}", userId);

        TerminationApplicationEntity app = findApplicationById(request.getId());
        ApplicationMaster master = app.getApplicationMaster();

        if (request.getStatus() != null) {
            switch (request.getStatus()) {

                case "Approved" -> {
                    LocalDateTime now = LocalDateTime.now();
                    app.setCurrentStatus("CMS HEAD APPROVED");
                    app.setRemarksCMSHead(request.getRemarks());
                    app.setCmsHeadReviewedAt(now);
                    app.setCmsHeadFileId(request.getFileId());
                    app.setApprovedAt(now);

                    if (master != null) {
                        master.setCurrentStatus("CMS HEAD APPROVED");
                        master.setApprovedAt(now);
                        master.setCompletedAt(now);
                        applicationMasterRepository.save(master);
                    }

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendTerminationNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber());
                    }
                    assert master != null;
                    createTask( master, app, "DIRECTOR CMS APPROVED", userId, app.getCreatedBy());

                    Optional<MiningLeaseApplication> miningLeaseApplication = miningLeaseApplicationRepository.findByApplicationNumber(app.getApplicationNumber());
                    MiningLeaseApplication miningLeaseApplicationEntity = null;
                    if (miningLeaseApplication.isPresent()) {
                        miningLeaseApplicationEntity = miningLeaseApplication.get();
                    }
                    assert miningLeaseApplicationEntity != null;
                    miningLeaseApplicationEntity.setCurrentStatus("TERMINATION APPROVED");
                    miningLeaseApplicationRepository.save(miningLeaseApplicationEntity);
                }
                case "Rectification" -> {
                    LocalDateTime now = LocalDateTime.now();
                    app.setCurrentStatus("RECTIFICATION BY CMS");
                    app.setRemarksCMSHead(request.getRemarks());
                    app.setCmsHeadReviewedAt(now);
                    app.setCmsHeadFileId(request.getFileId());

                    if (master != null) {
                        master.setCurrentStatus("RECTIFICATION BY CMS");
                        master.setRemarks(request.getRemarks());
                        applicationMasterRepository.save(master);
                    }

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendTerminationRevisionRequestNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber(),
                                app.getCurrentStatus(),
                                app.getRemarksCMSHead());
                    }

                    assert master != null;
                    createTask(master, app, "APPLICANT", userId, app.getPromoterUserId());
                }
                case "Lift suspension" -> {
                    LocalDateTime now = LocalDateTime.now();
                    app.setCurrentStatus("TERMINATION CANCELED");
                    app.setRemarksCMSHead(request.getRemarks());
                    app.setCmsHeadReviewedAt(now);
                    if (request.getFileId() != null) {
                        app.setCmsHeadFileId(request.getFileId());
                    }
                    app.setApprovedAt(now);

                    if (master != null) {
                        master.setCurrentStatus("TERMINATION CANCELED");
                        master.setApprovedAt(now);
                        master.setCompletedAt(now);
                        applicationMasterRepository.save(master);
                    }

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendTerminationCancellationNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber());
                    }
                    assert master != null;
                    createTask( master, app, "TERMINATION CANCELED", userId, app.getCreatedBy());
                }
                default -> throw new IllegalArgumentException("Application status not recognized");
            }
            terminationApplicationRepository.save(app);
        }
        return terminationMapper.toResponse(app);
    }

    private TerminationApplicationEntity findApplicationById(Long id) {
        return terminationApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));
    }

    public SuccessResponse<List<TerminationApplicationResponse>> getAssignedToPromoter(Long userId, Pageable pageable, String search) {
        Page<TerminationApplicationEntity> page;

        if (search == null || search.isBlank()) {

            page = terminationApplicationRepository
                    .findAssignedToUserPromoter(userId, pageable);

        } else {

            page = terminationApplicationRepository
                    .findAssignedToUserAndSearchPromoter(
                            userId,
                            search.trim(),
                            pageable
                    );
        }

        Page<TerminationApplicationResponse> responsePage =
                page.map(terminationMapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Transactional
    public TerminationApplicationResponse reviewApplicationPromoter(@Valid ReviewTerminationApplicationCMSHead request, Long userId) {
        log.info("Reviewing Termination application by Promoter: {}", userId);

        TerminationApplicationEntity app = findApplicationById(request.getId());
        ApplicationMaster master = app.getApplicationMaster();

        TaskManagement taskManagement = taskManagementRepository.findByApplicationNumberAndAssignedToRoleAndTaskStatusAndServiceCode(app.getApplicationNumber(),"CMS HEAD", "SUBMITTED", SERVICE_CODE );
        if (request.getStatus() != null) {

            if (request.getStatus().equals("Rectification")) {
                app.setCurrentStatus("RECTIFICATION BY PROMOTER");
                app.setPromoterFileId(request.getFileId());

                if (master != null) {
                    master.setCurrentStatus("RECTIFICATION BY PROMOTER");
                    master.setRemarks(request.getRemarks());
                    applicationMasterRepository.save(master);
                }

                if (app.getApplicantEmail() != null) {
                    notificationClient.sendTerminationRevisionRequestNotification(
                            app.getApplicantEmail(),
                            app.getApplicantName(),
                            app.getApplicationNumber(),
                            app.getCurrentStatus(),
                            app.getRemarksCMSHead());
                }

                assert master != null;
                createTask(master, app, "CMS HEAD", userId, taskManagement.getAssignedToUserId());
            } else {
                throw new IllegalArgumentException("Application status not recognized");
            }
            terminationApplicationRepository.save(app);
}
        return terminationMapper.toResponse(app);

    }

//    public SuccessResponse<List<TerminationApplicationResponse>> getAllApplications(
//            Long userId, Pageable pageable, String search) {
//
//        // Normalize search
//        if (search != null && search.isBlank()) {
//            search = null;
//        }
//
//        Page<TerminationGroupedProjection> page =
//                terminationApplicationRepository.findGroupedApplications(userId, search, pageable);
//
//        Page<TerminationApplicationResponse> responsePage = page.map(p -> {
//
//            TerminationApplicationResponse res = new TerminationApplicationResponse();
//
//            res.setTerminationId(p.getTerminationId());
//
//            // Convert array → List
//            if (p.getApplicationNumbers() != null) {
//                res.setApplicationNumbers(Arrays.asList(p.getApplicationNumbers()));
//            }
//
//            res.setCurrentStatus(p.getCurrentStatus());
//            res.setCreatedAt(p.getCreatedAt());
//
//            // ✅ NEW FIELDS
//            res.setApplicantName(p.getApplicantName());
//            res.setPromoterUserId(p.getPromoterUserId());
//
//            return res;
//        });
//
//        return SuccessResponse.fromPage(
//                "Assigned applications fetched successfully",
//                responsePage
//        );
//    }

    public Page<TerminationApplicationResponse> getArchivedApplications(Pageable pageable, String search, Long userId) {
        List<String> archivedStatuses = List.of("CMS HEAD APPROVED", "TERMINATION CANCELED");
        Page<TerminationApplicationEntity> applications;
        List<Long> role_id = terminationApplicationRepository.findUserDetails(userId);
        if(role_id !=null && role_id.contains(35)){

            if (search == null || search.isBlank()) {
                applications = terminationApplicationRepository.findArchivedAssignedToUserCMSHead(
                        userId,
                        archivedStatuses,
                        pageable);
            }
            else {

                applications = terminationApplicationRepository.findArchivedAssignedToUserAndSearchCMSHead(
                        userId,
                        search.trim(),
                        archivedStatuses,
                        pageable
                );
            }

        }else {
        if (search == null || search.isBlank()) {
            applications = terminationApplicationRepository.findArchivedAssignedToUser(
                    userId,
                    archivedStatuses,
                    pageable);
        }
        else {

            applications = terminationApplicationRepository.findArchivedAssignedToUserAndSearch(
                    userId,
                    search.trim(),
                    archivedStatuses,
                    pageable
            );
        }
        }
        return applications.map(terminationMapper::toListResponse);
    }

    @Transactional(readOnly = true)
    public Page<TerminationApplicationResponse> getMyArchivedApplications(Long userId, Pageable pageable, String search) {
        List<String> archivedStatuses = List.of("CMS HEAD APPROVED", "TERMINATION CANCELED");
        Page<TerminationApplicationEntity> applications ;

        if (search == null || search.isBlank()) {
            applications =  terminationApplicationRepository.findByApplicantUserIdAndStatusIn(userId, archivedStatuses, pageable);
        } else {
            applications = terminationApplicationRepository.findByApplicantUserIdAndSearch(userId, archivedStatuses, search.trim(), pageable);
        }

        return applications.map(terminationMapper::toListResponse);
    }

    @Transactional
    private synchronized String generateTerminationId() {

        int year = Year.now().getValue();
        String prefix = String.format("TERMINATION-%d-", year);

        Integer maxSequence = terminationApplicationRepository.findMaxSequenceByPrefix(prefix);

        long nextSequence = (maxSequence == null ? 0 : maxSequence) + 1;

        return String.format("TERMINATION-%d-%06d", year, nextSequence);
    }

    public SuccessResponse<List<TerminationApplicationResponse>> getAllApplicationAdmin(Pageable pageable, String search) {
        Page<TerminationApplicationEntity> page;

        if (search == null || search.isBlank()) {
            page = terminationApplicationRepository.findAll(pageable);
        } else {
            page = terminationApplicationRepository.findAllBySearch(search.trim(), pageable);
        }

        return SuccessResponse.fromPage("Applications fetched successfully", page.map(terminationMapper::toResponse));
    }

    public TerminationApplicationResponse getApplicationByNumber(String applicationNo) {
        TerminationApplicationEntity application = terminationApplicationRepository.findByApplicationNumber(applicationNo)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationNo));

        return terminationMapper.toResponse(application);
    }

    public SuccessResponse<List<TerminationApplicationResponse>> getAllApplications(Long currentUserId, Pageable pageable, String search) {
        Page<TerminationApplicationEntity> page;

        if (search == null || search.isBlank()) {

            page = terminationApplicationRepository
                    .findAssignedToUserChiefMD(currentUserId, pageable);

        } else {

            page = terminationApplicationRepository
                    .findAssignedToUserAndSearchChiefMD(
                            currentUserId,
                            search.trim(),
                            pageable
                    );
        }

        Page<TerminationApplicationResponse> responsePage =
                page.map(terminationMapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }
}
