package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.AssignedTaskRC;
import com.mas.gov.bt.mas.primary.dto.request.ImmediateSuspensionApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.request.PromoterImmediateSuspensionRequest;
import com.mas.gov.bt.mas.primary.dto.request.RcMeImmediateSuspensionRequest;
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


    public ImmediateSuspensionApplicationResponse submitImmediateSuspensionApplication(@Valid ImmediateSuspensionApplicationRequest request, Long userId) {
        ImmediateSuspensionApplication immediateSuspensionApplication = new ImmediateSuspensionApplication();
        if(request.getApplicationFrom().equalsIgnoreCase("M")){
            Optional<MiningLeaseApplication> miningLeaseApplication =
                    miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNumber());

            if (miningLeaseApplication.isEmpty()) {
                throw new RuntimeException("Invalid application number: " + request.getApplicationNumber());
            }

            MiningLeaseApplication miningLeaseApplication1 = miningLeaseApplication.get();

            immediateSuspensionApplication.setApplicationNumber(miningLeaseApplication1.getApplicationNumber());
            immediateSuspensionApplication.setApplicationFrom(request.getApplicationFrom());
            immediateSuspensionApplication.setApplicantName(miningLeaseApplication1.getApplicantName());
            immediateSuspensionApplication.setApplicantEmail(miningLeaseApplication1.getApplicantEmail());
            immediateSuspensionApplication.setApplicantName(miningLeaseApplication1.getApplicantName());
            immediateSuspensionApplication.setRemarksRcMi(request.getRcMiRemark());
            immediateSuspensionApplication.setSuspensionReasonId(request.getSuspensionReasonId());


            // Application master
            ApplicationMaster master = miningLeaseApplication1.getApplicationMaster();
            master.setSubmittedAt(LocalDateTime.now());
            master.setCurrentStatus("SUBMITTED");
            master.setApplicantUserId(userId);
            master.setServiceCode(SERVICE_CODE);
            applicationMasterRepository.save(master);

            immediateSuspensionApplicationRepository.save(immediateSuspensionApplication);

            immediateSuspensionApplication.setApplicationMaster(master);

            // Task creation
            createTask(master, immediateSuspensionApplication, "APPLICANT", userId, miningLeaseApplication1.getApplicantUserId());

            if (miningLeaseApplication1.getApplicantEmail() != null) {
                notificationClient.sendMiningLeaseMailToDirectorAssigned(
                        miningLeaseApplication1.getApplicantEmail(),
                        miningLeaseApplication1.getApplicantName(),
                        request.getApplicationNumber());
            }

            if (miningLeaseApplication1.getApplicantUserId() != null) {
                String title = "Immediate Suspension application has been issued related to your lease.";
                String message = "Application No. " + request.getApplicationNumber();
                String serviceId = "116";
                notificationClient.sendUserNotification(title, message, immediateSuspensionApplication.getPromoterUserId(), serviceId);
            }

        }else if(request.getApplicationFrom().equalsIgnoreCase("Q")){
            Optional<QuarryLeaseApplication> application =
                    quarryLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNumber());

            if (application.isEmpty()) {
                throw new RuntimeException("Invalid application number: " + request.getApplicationNumber());
            }else {
                QuarryLeaseApplication quarryLeaseApplication = application.get();
                immediateSuspensionApplication.setApplicationNumber(quarryLeaseApplication.getApplicationNumber());
                immediateSuspensionApplication.setApplicationFrom(request.getApplicationFrom());
                immediateSuspensionApplication.setApplicantName(quarryLeaseApplication.getApplicantName());
                immediateSuspensionApplication.setApplicantEmail(quarryLeaseApplication.getApplicantEmail());
                immediateSuspensionApplication.setRemarksRcMi(request.getRcMiRemark());
                immediateSuspensionApplication.setSuspensionReasonId(request.getSuspensionReasonId());

                // Application master
                ApplicationMaster master = quarryLeaseApplication.getApplicationMaster();
                master.setSubmittedAt(LocalDateTime.now());
                master.setCurrentStatus("SUBMITTED");
                master.setApplicantUserId(userId);
                master.setServiceCode(SERVICE_CODE);
                applicationMasterRepository.save(master);

                immediateSuspensionApplicationRepository.save(immediateSuspensionApplication);

                immediateSuspensionApplication.setApplicationMaster(master);
            }
        }
        return immediateSuspensionMapper.toResponse(immediateSuspensionApplication);

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
            immediateSuspensionApplication.setCurrentStatus("ME ASSIGNED");
            immediateSuspensionApplication.setRemarksRcMi(request.getRemarksRC());
            applicationMaster.setCurrentStatus("ME ASSIGNED");
        }
        applicationMasterRepository.save(applicationMaster);
        immediateSuspensionApplicationRepository.save(immediateSuspensionApplication);

        if (request.getMIFocalId() != null) {
            createTask(
                    applicationMaster,
                    immediateSuspensionApplication,
                    "ME",
                    userId,
                    request.getMIFocalId());

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
        log.info("Reviewing Termination application by Promoter: {}", userId);

        ImmediateSuspensionApplication app = findApplicationById(request.getId());
        ApplicationMaster master = app.getApplicationMaster();

        if (request.getStatus() != null) {

            if (request.getStatus().equals("Rectification")) {
                LocalDateTime now = LocalDateTime.now();
                app.setCurrentStatus("RECTIFICATION NEEDED");
                app.setPromoterReviewedAt(now);
                app.setPromoterFileId(request.getFileId());

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
                app.setCurrentStatus("SUSPENSION LIFTED");
                app.setPromoterReviewedAt(now);
                app.setPromoterFileId(request.getFileId());

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
            } else {
                throw new IllegalArgumentException("Application status not recognized");
            }
            immediateSuspensionApplicationRepository.save(app);
        }
        return immediateSuspensionMapper.toResponse(app);
    }

    public ImmediateSuspensionApplicationResponse reviewApplicationRCME(@Valid RcMeImmediateSuspensionRequest request, Long userId) {
        log.info("Reviewing Termination application by Promoter: {}", userId);

        ImmediateSuspensionApplication app = findApplicationById(request.getId());
        ApplicationMaster master = app.getApplicationMaster();

        if (request.getStatus() != null) {

            if (request.getStatus().equals("Suspended")) {
                LocalDateTime now = LocalDateTime.now();
                app.setCurrentStatus("SUSPENDED");
                app.setPromoterReviewedAt(now);

                if(app.getApplicationFrom().equalsIgnoreCase("M")){
                    Optional<MiningLeaseApplication> miningLeaseApplication =
                            miningLeaseApplicationRepository.findByApplicationNumber(app.getApplicationNumber());

                    if (miningLeaseApplication.isEmpty()) {
                        throw new RuntimeException("Invalid application number: " + app.getApplicationNumber());
                    }

                    MiningLeaseApplication miningLeaseApplication1 = miningLeaseApplication.get();

                    miningLeaseApplication1.setCurrentStatus("SUSPENDED");
                    miningLeaseApplicationRepository.save(miningLeaseApplication1);
                }else if(app.getApplicationFrom().equalsIgnoreCase("Q")){
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
            } else if (request.getStatus().equals("Lifting")) {
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
            } else {
                throw new IllegalArgumentException("Application status not recognized");
            }
            immediateSuspensionApplicationRepository.save(app);
        }
        return immediateSuspensionMapper.toResponse(app);
    }
}
