package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.ApplicationListResponse;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.entity.*;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.exception.ResourceNotFoundException;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.mapper.MiningLeaseMapper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MiningLeaseRenewalService {

    private static final String SERVICE_CODE = "MINING LEASE RENEWAL";

    private static final int DEFAULT_TAT_DAYS = 2;

    private final MiningLeaseMapper mapper;

    private final MiningLeaseApplicationRepository miningLeaseApplicationRepository;

    private final DzongkhagLookupRepository dzongkhagLookupRepository;

    private final GewogLookupRepository gewogLookupRepository;

    private final VillageLookupRepository villageLookupRepository;

    private final ApplicationMasterRepository applicationMasterRepository;

    private final TaskManagementRepository taskManagementRepository;

    private final NotificationClient notificationClient;

    private final ApplicationRevisionHistoryRepository revisionHistoryRepository;

    private final MiningLeaseRenewalApplicationRepository  miningLeaseRenewalApplicationRepository;

    public Page<ApplicationListResponse> getApplicationByApplicantId(Long userId, Pageable pageable) {
        List<String> statusIns = new ArrayList<>();
        statusIns.add("MINING LEASE APPROVED");
        Page<MiningLeaseApplication> applications;
        applications = miningLeaseApplicationRepository.findByApplicantUserIdAndStatusIn(userId, statusIns, pageable);
        return applications.map(mapper::toListResponse);
    }

    @Transactional
    public MiningLeaseResponse createRenewalApplication(@Valid RenewalMiningLeaseRequest request, Long userId) {
        Optional<MiningLeaseApplication> miningLeaseApplication = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNumber());
        MiningLeaseRenewalApplication miningLeaseRenewalApplication = new MiningLeaseRenewalApplication();
        if (miningLeaseApplication.isPresent()) {
            // 1
            // Map all data to renewal application table
            // IF THE DATA IS PRESENT
            miningLeaseRenewalApplication.setApplicationNumber(request.getApplicationNumber());
            miningLeaseRenewalApplication.setApplicantCid(request.getApplicantCid());
            miningLeaseRenewalApplication.setApplicantContact(request.getApplicantContact());
            miningLeaseRenewalApplication.setApplicantEmail(request.getApplicantEmail());
            miningLeaseRenewalApplication.setApplicantType(request.getApplicantType());
            miningLeaseRenewalApplication.setPostalAddress(request.getPostalAddress());
            miningLeaseRenewalApplication.setTelephoneNo(request.getTelephoneNo());
            miningLeaseRenewalApplication.setLeaseEndDate(request.getLeaseEndDate());
            miningLeaseRenewalApplication.setLeasePeriodYears(request.getLeasePeriodYears());
            miningLeaseRenewalApplication.setProposedLeaseRenewalPeriod(request.getProposedLeaseRenewalPeriod());
            // Application master
            ApplicationMaster applicationMaster = miningLeaseRenewalApplication.getApplicationMaster();
            miningLeaseRenewalApplication.setApplicationMaster(applicationMaster);
            miningLeaseRenewalApplication.setPlaceOfMiningActivity(request.getPlaceOfMiningActivity());
            miningLeaseRenewalApplication.setDungkhag(request.getDungkhag());

            if (request.getDzongkhag() != null && !request.getDzongkhag().isEmpty()) {
                DzongkhagLookup dzongkhag = dzongkhagLookupRepository
                        .findById(request.getDzongkhag())
                        .orElseThrow(() -> new RuntimeException("Invalid Dzongkhag ID"));

                miningLeaseRenewalApplication.setDzongkhag(dzongkhag);
            }

            if (request.getGewog() != null && !request.getGewog().isEmpty()) {
                GewogLookup gewog = (GewogLookup) gewogLookupRepository
                        .findByGewogId(request.getGewog())
                        .orElseThrow(() -> new RuntimeException("Invalid gewog ID"));

                miningLeaseRenewalApplication.setGewog(gewog);
            }

            if (request.getNearestVillage() != null && !request.getNearestVillage().isEmpty()) {
                VillageLookup villageLookup = (VillageLookup) villageLookupRepository
                        .findByVillageSerialNo(Integer.parseInt(request.getNearestVillage()))
                        .orElseThrow(() -> new RuntimeException("Invalid village ID"));

                miningLeaseRenewalApplication.setNearestVillage(villageLookup);
            }

            miningLeaseRenewalApplication.setDepositAssessmentReportId(request.getDepositAssessmentReportId());
            miningLeaseRenewalApplication.setDeclarationStatus(request.isDeclarationStatus());
            miningLeaseRenewalApplication.setCurrentStatus("RENEWAL APPLICATION");

            // =====================================================
            // 2. ASSIGN DIRECTOR
            // =====================================================
            UserWorkloadProjection assignedDirector = assignDirector();

            // =====================================================
            // 3. APPLICATION MASTER CREATION
            // =====================================================
            applicationMaster.setCurrentStatus("RENEWAL APPLICATION");
            applicationMaster.setSubmittedAt(LocalDateTime.now());
            applicationMaster.setServiceCode(SERVICE_CODE);

            // =====================================================
            // 4. TASK CREATION FOR DIRECTOR
            // =====================================================
            createTask(applicationMaster, miningLeaseRenewalApplication, "DIRECTOR", userId, assignedDirector.getUserId());
            miningLeaseRenewalApplicationRepository.save(miningLeaseRenewalApplication);
            applicationMasterRepository.save(applicationMaster);

        } else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
        }

        return mapper.toRenewalResponse(miningLeaseRenewalApplication);
    }

    @Transactional
    private void createTask(ApplicationMaster master, MiningLeaseRenewalApplication application, String role, Long userId, Long assignedUserId) {
        LocalDateTime now = LocalDateTime.now();

        TaskManagement task = new TaskManagement();
        task.setApplicationNumber(application.getApplicationNumber());
        task.setServiceCode(SERVICE_CODE);
        task.setAssignedToRole(role);
        task.setAssignedByUserId(userId);
        task.setAssignedToUserId(assignedUserId);
        task.setAssignedAt(now);
        task.setTaskStatus(master.getCurrentStatus());

        task.setDeadlineDate(now.plusDays(DEFAULT_TAT_DAYS));
        task.setCreatedBy(userId);

        taskManagementRepository.save(task);
        log.info("Created task for role {}", role);
    }

    @Transactional
    public UserWorkloadProjection assignDirector() {

        UserWorkloadProjection director =
                miningLeaseApplicationRepository.findDirectorQuarrying();

        if (director == null) {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
        }
        return director;
    }

    public SuccessResponse<List<MiningLeaseResponse>> getAssignedToDirector(Long userId, Pageable pageable, String search) {
        Page<MiningLeaseRenewalApplication> page;

        if (search == null || search.isBlank()) {

            page = miningLeaseRenewalApplicationRepository
                    .findAssignedToUserDirector(userId, pageable);

        } else {

            page = miningLeaseRenewalApplicationRepository
                    .findAssignedToUserAndSearchDirector(
                            userId,
                            search.trim(),
                            pageable
                    );
        }

        Page<MiningLeaseResponse> responsePage =
                page.map(mapper::toRenewalResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Transactional
    public MiningLeaseResponse assignApplicationDirector(@Valid RenewalTaskAssignedDirector request, Long userId) {
        MiningLeaseRenewalApplication miningLeaseRenewalApplication ;

        miningLeaseRenewalApplication = findApplicationById(request.getApplicationId());
        ApplicationMaster applicationMaster ;
        if(miningLeaseRenewalApplication != null) {
            applicationMaster = miningLeaseRenewalApplication.getApplicationMaster();
        }else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
        }
        if (request.getMiningEngineerId() != null && request.getGeologistId() != null) {
            miningLeaseRenewalApplication.setCurrentStatus("ASSIGNED");
            applicationMaster.setCurrentStatus("ASSIGNED");
        }else {
            miningLeaseRenewalApplication.setCurrentStatus("GEOLOGIST ASSIGNED");
            applicationMaster.setCurrentStatus("GEOLOGIST ASSIGNED");
        }
        applicationMasterRepository.save(applicationMaster);
        miningLeaseRenewalApplicationRepository.save(miningLeaseRenewalApplication);

        if (request.getGeologistId() != null) {
            createTask(
                    applicationMaster,
                    miningLeaseRenewalApplication,
                    "GEOLOGIST",
                    userId,
                    request.getGeologistId());

            UserWorkloadProjection userGeologist = miningLeaseApplicationRepository.findUserDetailsME(request.getGeologistId());

            if (miningLeaseRenewalApplication.getApplicantEmail() != null) {
                notificationClient.sendStatusUpdateNotification(
                        miningLeaseRenewalApplication.getApplicantEmail(),
                        miningLeaseRenewalApplication.getApplicantName(),
                        miningLeaseRenewalApplication.getApplicationNumber(),
                        miningLeaseRenewalApplication.getCurrentStatus(),
                        "ASSIGNED");
            }

            if (userGeologist != null) {
                notificationClient.sendAssignmentNotification(
                        userGeologist.getEmail(),
                        userGeologist.getUsername(),
                        miningLeaseRenewalApplication.getApplicationNumber(),
                        "ASSIGNED");

                String title = "A new mining application has been assigned.";
                String message = "A new mining application has been assigned.";
                String serviceId = "85";
                notificationClient.sendUserNotification(title, message, userGeologist.getUserId(), serviceId);
            }
        }

        if(request.getMiningEngineerId() != null){

            createTask(
                    applicationMaster,
                    miningLeaseRenewalApplication,
                    "MPCD_FOCAL",
                    userId,
                    request.getMiningEngineerId());

            UserWorkloadProjection userMineEngineer = miningLeaseApplicationRepository.findUserDetailsME(request.getMiningEngineerId());

            if (miningLeaseRenewalApplication.getApplicantEmail() != null) {
                notificationClient.sendStatusUpdateNotification(
                        miningLeaseRenewalApplication.getApplicantEmail(),
                        miningLeaseRenewalApplication.getApplicantName(),
                        miningLeaseRenewalApplication.getApplicationNumber(),
                        miningLeaseRenewalApplication.getCurrentStatus(),
                        "ASSIGNED");
            }

            if (userMineEngineer != null) {
                notificationClient.sendAssignmentNotification(
                        userMineEngineer.getEmail(),
                        userMineEngineer.getUsername(),
                        miningLeaseRenewalApplication.getApplicationNumber(),
                        "ASSIGNED");

                String title = "A new mining application has been assigned.";
                String message = "A new mining application has been assigned.";
                String serviceId = "85";
                notificationClient.sendUserNotification(title, message, userMineEngineer.getUserId(), serviceId);
            }
        }
        return mapper.toRenewalResponse(miningLeaseRenewalApplication);
    }

    private MiningLeaseRenewalApplication findApplicationById(Long id) {
        return miningLeaseRenewalApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));
    }

    @Transactional
    public MiningLeaseResponse reviewApplicationDirector(@Valid ReviewMiningLeaseApplicationDirector request, Long userId) {
        log.info("Reviewing renewal mining lease application by Director user: {}", userId);

        MiningLeaseRenewalApplication app = findApplicationById(request.getId());
        ApplicationMaster master = app.getApplicationMaster();

        if (request.getStatus() != null) {
            switch (request.getStatus()) {

                case "Accepted" -> {
                    LocalDateTime now = LocalDateTime.now();
                    app.setCurrentStatus("DIRECTOR APPROVED FMFS");
                    app.setRemarksDirector(request.getRemarks());
                    app.setDirectorReviewedAt(now);
                    app.setApprovedAt(now);

                    List<TaskManagement> taskManagement = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRole(app.getApplicationNumber(),"FMFS SUBMITTED","MINE ENGINEER");
                    Long mineEngineerId = null;
                    if (taskManagement != null) {
                        TaskManagement taskManagement1 = taskManagement.getFirst();
                        mineEngineerId = taskManagement1.getAssignedToUserId();
                    }

                    if (master != null) {
                        master.setCurrentStatus("DIRECTOR APPROVED FMFS");
                        master.setApprovedAt(now);
                        master.setCompletedAt(now);
                        applicationMasterRepository.save(master);
                    }

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendApprovalNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber());
                    }
                    assert master != null;
                    createTask( master, app, "DIRECTOR APPROVED FMFS", userId, mineEngineerId);
                }
                case "Approved" -> {
                    LocalDateTime now = LocalDateTime.now();
                    app.setCurrentStatus("APPROVED BY DIRECTOR");
                    app.setRemarksDirector(request.getRemarks());
                    app.setMlaSignedAt(now);
                    app.setMlaStatus("SIGNED");
                    app.setDirectorReviewedAt(now);
                    app.setApprovedAt(now);

                    if (master != null) {
                        master.setCurrentStatus("APPROVED BY DIRECTOR");
                        master.setApprovedAt(now);
                        master.setCompletedAt(now);
                        applicationMasterRepository.save(master);
                    }

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendApprovalNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber());
                    }

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendMLASigningNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber());
                    }
                    assert master != null;
                    createTask(master, app, "DIRECTOR", userId, userId);
                }
                case "Rejected" -> {
                    app.setCurrentStatus("REJECTED");
                    app.setRemarksDirector(request.getRemarks());
                    app.setDirectorReviewedAt(LocalDateTime.now());
                    app.setRejectedAt(LocalDateTime.now());
                    app.setRejectionReason(request.getRemarks());

                    if (master != null) {
                        master.setCurrentStatus("REJECTED");
                        master.setRejectedAt(LocalDateTime.now());
                        master.setRejectionRemarks(request.getRemarks());
                        master.setCompletedAt(LocalDateTime.now());
                        applicationMasterRepository.save(master);
                    }

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendRejectionNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber(),
                                request.getRemarks());
                    }
                }
                default -> throw new IllegalArgumentException("Application status not recognized");
            }
            miningLeaseRenewalApplicationRepository.save(app);
        }
        return mapper.toRenewalResponse(app);
    }

    @Transactional
    public MiningLeaseResponse reviewApplicationME(@Valid ReviewMiningLeaseApplicationME request, Long userId) {
        log.info("Reviewing renewal mining lease application by Mining Engineer user: {}", userId);

        MiningLeaseRenewalApplication app = findApplicationById(request.getId());
        ApplicationMaster master = app.getApplicationMaster();

        if (request.getStatus() != null) {
            switch (request.getStatus()) {
                case "Approved" -> {
                    app.setCurrentStatus("APPROVED");
                    app.setRemarksME(request.getRemarks());
                    app.setFmfsStatus(request.getFmfsStatus());
                    app.setMeReviewedAt(LocalDateTime.now());

                    if (master != null) {
                        master.setCurrentStatus("APPROVED");
                        applicationMasterRepository.save(master);
                    }

                    // =====================================================
                    // 8. ASSIGN DIRECTOR + CREATE TASK
                    // =====================================================
                    UserWorkloadProjection assignedMiningChief =
                            assignMiningChief();

                    assert master != null;
                    createTask(master, app, "MINING_CHIEF", userId, assignedMiningChief.getUserId());

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber(),
                                "Mining Chief Review",
                                "Your application has been forwarded to the Mining Chief for review.");
                    }

                    if (assignedMiningChief.getEmail() != null) {
                        notificationClient.sendAssignmentNotification(
                                assignedMiningChief.getEmail(),
                                assignedMiningChief.getUsername(),
                                app.getApplicationNumber(),
                                "Mining Chief Review");

                    }
                }
                case "ACCEPTED APP" -> {
                    List<TaskManagement> taskManagement = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRole(app.getApplicationNumber(),"ASSIGNED","GEOLOGIST");
                    Long geologistId = null;
                    if (taskManagement != null) {
                        TaskManagement taskManagement1 = taskManagement.getFirst();
                        geologistId = taskManagement1.getAssignedToUserId();
                    }
                    app.setCurrentStatus("GEOLOGIST_REVIEW");
                    app.setRemarksME(request.getRemarks());
                    app.setMeReviewedAt(LocalDateTime.now());

                    if (master != null) {
                        master.setCurrentStatus("GEOLOGIST_REVIEW");
                        applicationMasterRepository.save(master);
                    }

                    assert master != null;
                    createTask(master, app, "GEOLOGIST", userId, geologistId);

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber(),
                                "Geologist Review",
                                "Your application has been forwarded to the Geologist for review.");
                    }

                    String title = "Application status updated.";
                    String message = "Your application has been forwarded to geologist to review.";
                    String serviceId = "85";
                    notificationClient.sendUserNotification(title, message, app.getCreatedBy(), serviceId);
                }
                case "Forwarded FMFS" -> {
                    app.setCurrentStatus("MINING_CHIEF_REVIEW");
                    app.setRemarksME(request.getRemarks());
                    app.setFmfsStatus(request.getFmfsStatus());
                    app.setMeReviewedAt(LocalDateTime.now());

                    if (master != null) {
                        master.setCurrentStatus("MINING_CHIEF_REVIEW");
                        applicationMasterRepository.save(master);
                    }

                    // =====================================================
                    // 8. ASSIGN DIRECTOR + CREATE TASK
                    // =====================================================
                    UserWorkloadProjection assignedMiningChief =
                            assignMiningChief();

                    assert master != null;
                    createTask(master, app, "MINING_CHIEF_REVIEW", userId, assignedMiningChief.getUserId());

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber(),
                                "Mining Chief Review",
                                "Your application has been forwarded to the Mining Chief for review.");
                    }

                    if (assignedMiningChief.getEmail() != null) {
                        notificationClient.sendAssignmentNotification(
                                assignedMiningChief.getEmail(),
                                assignedMiningChief.getUsername(),
                                app.getApplicationNumber(),
                                "Mining Chief Review");

                        if(assignedMiningChief.getUserId()!= null) {
                            String title = "An new application has been assigned.";
                            String message = "An application for mining lease has been assigned for review. Application No. "+ app.getApplicationNumber() +" Please login in review the application";
                            String serviceId = "78";
                            notificationClient.sendUserNotification(title, message, assignedMiningChief.getUserId(), serviceId);
                        }else {
                            throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
                        }
                    }
                }
                case "Rejected" -> {
                    app.setCurrentStatus("REJECTED");
                    app.setRemarksME(request.getRemarks());
                    app.setMeReviewedAt(LocalDateTime.now());
                    app.setRejectedAt(LocalDateTime.now());
                    app.setRejectionReason(request.getRemarks());

                    if (master != null) {
                        master.setCurrentStatus("REJECTED");
                        applicationMasterRepository.save(master);
                    }

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendRejectionNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber(),
                                request.getRemarks());
                    }
                }
                case "Resubmit FMFS" -> {
                    app.setCurrentStatus("RESUBMIT FMFS");
                    app.setRemarksME(request.getRemarks());
                    app.setMeReviewedAt(LocalDateTime.now());

                    createRevisionRecord(app, "ME_REVIEW_FMFS", request.getRemarks(), userId);
                    createTask(master, app, "APPLICANT", userId, app.getCreatedBy());

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendRevisionRequestNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber(),
                                "Mining Engineer Review",
                                request.getRemarks());
                    }
                }
                default -> throw new IllegalArgumentException("Application status not recognized");
            }
            miningLeaseRenewalApplicationRepository.save(app);
        }
        return mapper.toRenewalResponse(app);
    }

    @Transactional
    public UserWorkloadProjection assignMiningChief() {

        UserWorkloadProjection director =
                miningLeaseApplicationRepository.findChiefQuarrying();

        if (director == null) {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
        }
        return director;
    }

    @Transactional
    private void createRevisionRecord(MiningLeaseRenewalApplication miningLeaseApplication, String reviewStage, String geologistRemarks, Long userId) {
        long revisionCount = revisionHistoryRepository.countByApplicationIdAndRevisionStage(miningLeaseApplication.getId(), reviewStage);

        ApplicationRevisionHistory revision = ApplicationRevisionHistory.builder()
                .applicationId(miningLeaseApplication.getId())
                .applicationNumber(miningLeaseApplication.getApplicationNumber())
                .revisionStage(reviewStage)
                .revisionNumber((int) revisionCount + 1)
                .remarks(geologistRemarks)
                .requestedBy(userId)
                .requestedAt(LocalDateTime.now())
                .status("PENDING")
                .createdBy(userId)
                .build();

        revisionHistoryRepository.save(revision);
        log.info("Created revision record #{} for application {} at stage {}", revision.getRevisionNumber(), miningLeaseApplication.getApplicationNumber(), reviewStage);
    }
}
