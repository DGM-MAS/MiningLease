package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.ReassignTaskRequest;
import com.mas.gov.bt.mas.primary.dto.request.ReviewTerminationApplicationCMSHead;
import com.mas.gov.bt.mas.primary.dto.request.TerminationApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.response.TerminationApplicationResponse;
import com.mas.gov.bt.mas.primary.entity.ApplicationMaster;
import com.mas.gov.bt.mas.primary.entity.MiningLeaseApplication;
import com.mas.gov.bt.mas.primary.entity.TaskManagement;
import com.mas.gov.bt.mas.primary.entity.TerminationApplicationEntity;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    public TerminationApplicationResponse submitTerminationApplication(@Valid TerminationApplicationRequest request, Long userId) {

        Optional<MiningLeaseApplication> miningLeaseApplication = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNumber().getFirst());
        MiningLeaseApplication miningLeaseApplication1 = null;
        if (miningLeaseApplication.isPresent()) {
            miningLeaseApplication1 = miningLeaseApplication.get();
        }
        // 1.
        // ======== SAVE TERMINATION SUBMITTED BY APPLICANT ====== //

        TerminationApplicationEntity terminationApplicationEntity =  new TerminationApplicationEntity();
        terminationApplicationEntity.setPromoterUserId(request.getPromoterUserId());
        terminationApplicationEntity.setFileId(request.getFileId());
        terminationApplicationEntity.setCreatedBy(userId);
        terminationApplicationEntity.setCreatedAt(LocalDateTime.now());
        terminationApplicationEntity.setApplicationNumber(request.getApplicationNumber().toString());
        terminationApplicationEntity.setRemarksChief(request.getRemarksChief());

        terminationApplicationRepository.save(terminationApplicationEntity);

        // =====================================================
        // 2. ASSIGN DIRECTOR
        // =====================================================
        UserWorkloadProjection assignedCMSHead = assignCMSHead();

        // =====================================================
        // 3. Application master and create task for director
        // =====================================================
        assert miningLeaseApplication1 != null;
        ApplicationMaster master = miningLeaseApplication1.getApplicationMaster();
        master.setSubmittedAt(LocalDateTime.now());
        master.setCurrentStatus("SUBMITTED");
        master.setApplicantUserId(userId);
        master.setServiceCode(SERVICE_CODE);
        applicationMasterRepository.save(master);
        terminationApplicationEntity.setApplicationMaster(master);
        createTask( master, terminationApplicationEntity, "CMS HEAD", userId, assignedCMSHead.getUserId());

        if (assignedCMSHead.getEmail() != null) {
            notificationClient.sendMailToDirectorAssigned(
                    assignedCMSHead.getEmail(),
                    assignedCMSHead.getUsername(),
                    terminationApplicationEntity.getApplicationNumber());
        }

        if(assignedCMSHead.getUserId()!= null) {
            String title = "Termination application has been assigned.";
            String message = "An application for termination has been assigned for review. Application No. "+ terminationApplicationEntity.getApplicationNumber()+" Please login to review the report.";
            String serviceId = "108";
            notificationClient.sendUserNotification(title, message, assignedCMSHead.getUserId(), serviceId);
        }else {
            throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }

        return terminationMapper.toResponse(terminationApplicationEntity);
    }

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
        List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleInAndServiceCode(request.getApplicationNumber(),"ASSIGNED",assignedRoles, SERVICE_CODE);

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
            String message = "An application for termination has been assigned for review. Application No. "+request.getApplicationNumber()+" Please login to review the application";
            String serviceId = "108";
            notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId);
        }else {
            throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }


        log.info("CMS Head task {} reassigned to user {}", taskManagement.getId(), request.getNewAssigneeUserId());
    }

//    public TerminationApplicationResponse reviewApplicationCMSHead(@Valid ReviewTerminationApplicationCMSHead request, Long userId) {
//        log.info("Reviewing Termination application by CMS Head user: {}", userId);
//
//        MiningLeaseApplication app = findApplicationById(request.getId());
//        ApplicationMaster master = app.getApplicationMaster();
//
//        completeCurrentTask(master,request.getStatus(), request.getRemarks());
//
//        if (request.getStatus() != null) {
//            switch (request.getStatus()) {
//
//                case "Accepted" -> {
//                    LocalDateTime now = LocalDateTime.now();
//                    app.setCurrentStatus("DIRECTOR APPROVED FMFS");
//                    app.setRemarksDirector(request.getRemarks());
//                    app.setDirectorReviewedAt(now);
//                    app.setApprovedAt(now);
//
//                    List<TaskManagement> taskManagement = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(app.getApplicationNumber(),"FMFS SUBMITTED","MINE ENGINEER", SERVICE_CODE);
//                    Long mineEngineerId = null;
//                    if (taskManagement != null) {
//                        TaskManagement taskManagement1 = taskManagement.getFirst();
//                        mineEngineerId = taskManagement1.getAssignedToUserId();
//                    }
//
//                    if (master != null) {
//                        master.setCurrentStatus("DIRECTOR APPROVED FMFS");
//                        master.setApprovedAt(now);
//                        master.setCompletedAt(now);
//                        applicationMasterRepository.save(master);
//                    }
//
//                    if (app.getApplicantEmail() != null) {
//                        notificationClient.sendApprovalNotification(
//                                app.getApplicantEmail(),
//                                app.getApplicantName(),
//                                app.getApplicationNumber());
//                    }
//                    assert master != null;
//                    createTask( master, app, "DIRECTOR APPROVED FMFS", userId, mineEngineerId);
//                }
//                case "Approved" -> {
//                    LocalDateTime now = LocalDateTime.now();
//                    app.setCurrentStatus("APPROVED BY DIRECTOR");
//                    app.setRemarksDirector(request.getRemarks());
//                    app.setMlaSignedAt(now);
//                    app.setMlaStatus("SIGNED");
//                    app.setDirectorReviewedAt(now);
//                    app.setApprovedAt(now);
//
//                    if (master != null) {
//                        master.setCurrentStatus("APPROVED BY DIRECTOR");
//                        master.setApprovedAt(now);
//                        master.setCompletedAt(now);
//                        applicationMasterRepository.save(master);
//                    }
//
//                    if (app.getApplicantEmail() != null) {
//                        notificationClient.sendApprovalNotification(
//                                app.getApplicantEmail(),
//                                app.getApplicantName(),
//                                app.getApplicationNumber());
//                    }
//
//                    if (app.getApplicantEmail() != null) {
//                        notificationClient.sendMLASigningNotification(
//                                app.getApplicantEmail(),
//                                app.getApplicantName(),
//                                app.getApplicationNumber());
//                    }
//                    assert master != null;
//                    createTask(master, app, "DIRECTOR", userId, userId);
//                }
//                case "Rejected" -> {
//                    app.setCurrentStatus("REJECTED");
//                    app.setRemarksDirector(request.getRemarks());
//                    app.setDirectorReviewedAt(LocalDateTime.now());
//                    app.setRejectedAt(LocalDateTime.now());
//                    app.setRejectionReason(request.getRemarks());
//
//                    if (master != null) {
//                        master.setCurrentStatus("REJECTED");
//                        master.setRejectedAt(LocalDateTime.now());
//                        master.setRejectionRemarks(request.getRemarks());
//                        master.setCompletedAt(LocalDateTime.now());
//                        applicationMasterRepository.save(master);
//                    }
//
//                    if (app.getApplicantEmail() != null) {
//                        notificationClient.sendRejectionNotification(
//                                app.getApplicantEmail(),
//                                app.getApplicantName(),
//                                app.getApplicationNumber(),
//                                request.getRemarks());
//                    }
//                }
//                default -> throw new IllegalArgumentException("Application status not recognized");
//            }
//            miningLeaseApplicationRepository.save(app);
//        }
//        return mapper.toResponse(app);
//    }
}
