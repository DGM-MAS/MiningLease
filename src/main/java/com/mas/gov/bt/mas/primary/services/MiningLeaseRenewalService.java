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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

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

        // Check if a renewal application (e.g. DRAFT) already exists for this application number
        Optional<MiningLeaseRenewalApplication> existingRenewal =
                miningLeaseRenewalApplicationRepository.findByApplicationNumber(request.getApplicationNumber());
        if (existingRenewal.isPresent() && !"DRAFT".equals(existingRenewal.get().getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.DUPLICATE_ENTRY);
        }

        MiningLeaseRenewalApplication miningLeaseRenewalApplication = existingRenewal.orElse(new MiningLeaseRenewalApplication());
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
            ApplicationMaster applicationMaster = miningLeaseApplication.get().getApplicationMaster();
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
                    "MINE ENGINEER",
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

    public MiningLeaseResponse getApplicationById(Long id) {
        return mapper.toRenewalResponse(findApplicationById(id));
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
                    Optional<MiningLeaseApplication> miningLeaseApplication =
                            miningLeaseApplicationRepository.findByApplicationNumber(app.getApplicationNumber());

                    if (miningLeaseApplication.isPresent()) {

                        MiningLeaseApplication application = miningLeaseApplication.get();
                        String ecStatus = application.getECStatus();
                        Date ecExpiryDate = application.getECExpiryDate();

                        if (ecStatus != null && ecStatus.equalsIgnoreCase("VALID") && ecExpiryDate != null) {

                            // Convert Date → LocalDate
                            LocalDate expiryDate = ecExpiryDate.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();

                            if (!expiryDate.isBefore(LocalDate.now())) {
                                // ✅ EC is valid and not expired
                                // continue process

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
                            } else {
                                throw new RuntimeException("EC has expired.");
                            }
                        } else {
                            throw new RuntimeException("EC Status is not valid.");
                        }
                    }
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
                case "Resubmit Application" -> {
                    app.setCurrentStatus("RESUBMIT APP");
                    app.setRemarksME(request.getRemarks());
                    app.setMeReviewedAt(LocalDateTime.now());

                    createRevisionRecord(app, "ME_REVIEW_APP", request.getRemarks(), userId);
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

    public SuccessResponse<List<MiningLeaseResponse>> getAssignedToMineEngineer(Long userId, Pageable pageable, String search) {
        Page<MiningLeaseRenewalApplication> page;

        if (search == null || search.isBlank()) {

            page = miningLeaseRenewalApplicationRepository
                    .findAssignedToUserMineEngineer(userId, pageable);

        } else {

            page = miningLeaseRenewalApplicationRepository
                    .findAssignedToUserIdAndSearchMineEngineer(
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

    public SuccessResponse<List<MiningLeaseResponse>> getAssignedToGeologist(Long userId, Pageable pageable, String search) {
        Page<MiningLeaseRenewalApplication> page;

        if (search == null || search.isBlank()) {

            page = miningLeaseRenewalApplicationRepository
                    .findAssignedToUserGeologist(userId, pageable);

        } else {

            page = miningLeaseRenewalApplicationRepository
                    .findAssignedToUserAndSearchGeologist(
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

    public SuccessResponse<List<MiningLeaseResponse>> getAssignedToMiningChief(Long userId, Pageable pageable, String search) {
        Page<MiningLeaseRenewalApplication> page;

        if (search == null || search.isBlank()) {
            page = miningLeaseRenewalApplicationRepository.findAssignedToUserMiningChief(userId, pageable);
        } else {
            page = miningLeaseRenewalApplicationRepository.findAssignedToUserAndSearchMiningChief(userId, search.trim(), pageable);
        }

        return SuccessResponse.fromPage("Assigned applications fetched successfully", page.map(mapper::toRenewalResponse));
    }

    @Transactional
    public MiningLeaseResponse reviewApplicationGeologist(@Valid ReviewMiningLeaseApplicationGeologist reviewQuarryLeaseApplicationGeologist, Long userId) {
        log.info("Reviewing renewal mining lease application by Geologist user: {}", userId);

        MiningLeaseRenewalApplication miningLeaseRenewalApplication = findApplicationById(reviewQuarryLeaseApplicationGeologist.getId());
        ApplicationMaster applicationMaster = miningLeaseRenewalApplication.getApplicationMaster();

        if(reviewQuarryLeaseApplicationGeologist.getStatus() != null) {
            switch (reviewQuarryLeaseApplicationGeologist.getStatus()) {
                case "ACCEPTED" -> {
                    if (Objects.equals(miningLeaseRenewalApplication.getCurrentStatus(), "ACCEPTED DAR MPCD")) {
                        miningLeaseRenewalApplication.setCurrentStatus("APPROVED");
                    }else {
                        miningLeaseRenewalApplication.setCurrentStatus("ACCEPTED DAR");
                    }

                    miningLeaseRenewalApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseRenewalApplication.setGeologistReviewedAt(LocalDateTime.now());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("ACCEPTED PFS");
                        applicationMasterRepository.save(applicationMaster);
                    }

                    if (miningLeaseRenewalApplication.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                miningLeaseRenewalApplication.getApplicantEmail(),
                                miningLeaseRenewalApplication.getApplicantName(),
                                miningLeaseRenewalApplication.getApplicationNumber(),
                                "Mining Engineer Review",
                                "Your application has been forwarded to the Mining Engineer for review.");
                    }

                    String title = "Application status updated.";
                    String message = "Your application has been forwarded to geologist to review.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, miningLeaseRenewalApplication.getCreatedBy(), serviceId);
                }
                case "ACCEPTED GR" -> {
                    miningLeaseRenewalApplication.setCurrentStatus("APPROVED GR");
                    miningLeaseRenewalApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseRenewalApplication.setGeologistReviewedAt(LocalDateTime.now());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("APPROVED GR");
                        applicationMasterRepository.save(applicationMaster);
                    }

                    assert applicationMaster != null;
                    createTask(applicationMaster, miningLeaseRenewalApplication, "APPLICANT", userId, miningLeaseRenewalApplication.getCreatedBy());

                    if (miningLeaseRenewalApplication.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                miningLeaseRenewalApplication.getApplicantEmail(),
                                miningLeaseRenewalApplication.getApplicantName(),
                                miningLeaseRenewalApplication.getApplicationNumber(),
                                "GR APPROVED",
                                "Geological Report has been accepted. Please upload mining lease application and PFS to proceed further.");
                    }

                    if(miningLeaseRenewalApplication.getCreatedBy() != null) {
                        String title = "Geological report has been approved successfully.";
                        String message = "Geological Report has been accepted. Please upload mining lease application and PFS to proceed further.";
                        String serviceId = "78";
                        notificationClient.sendUserNotification(title, message, miningLeaseRenewalApplication.getCreatedBy(), serviceId);
                    }else {
                        throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
                    }
                }
                case "ACCEPTED FMFS" -> {
                    miningLeaseRenewalApplication.setCurrentStatus("ACCEPTED FMFS");
                    miningLeaseRenewalApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseRenewalApplication.setGeologistReviewedAt(LocalDateTime.now());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("ACCEPTED FMFS");
                        applicationMasterRepository.save(applicationMaster);
                    }

                    assert applicationMaster != null;
                    createTask(applicationMaster, miningLeaseRenewalApplication, "MINING_ENGINEER", userId, userId);

                    if (miningLeaseRenewalApplication.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                miningLeaseRenewalApplication.getApplicantEmail(),
                                miningLeaseRenewalApplication.getApplicantName(),
                                miningLeaseRenewalApplication.getApplicationNumber(),
                                "Mining Engineer Review",
                                "Your application has been forwarded to the Mining Engineer for review.");
                    }
                }
                case "Rejected" -> {
                    miningLeaseRenewalApplication.setCurrentStatus("REJECTED");
                    miningLeaseRenewalApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseRenewalApplication.setGeologistReviewedAt(LocalDateTime.now());
                    miningLeaseRenewalApplication.setRejectedAt(LocalDateTime.now());
                    miningLeaseRenewalApplication.setRejectionReason(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("REJECTED");
                        applicationMasterRepository.save(applicationMaster);
                    }

                    if (miningLeaseRenewalApplication.getApplicantEmail() != null) {
                        notificationClient.sendRejectionNotification(
                                miningLeaseRenewalApplication.getApplicantEmail(),
                                miningLeaseRenewalApplication.getApplicantName(),
                                miningLeaseRenewalApplication.getApplicationNumber(),
                                reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    }
                }
                case "Resubmit PFS" -> {
                    miningLeaseRenewalApplication.setCurrentStatus("RESUBMIT PFS");
                    miningLeaseRenewalApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseRenewalApplication.setGeologistReviewedAt(LocalDateTime.now());

                    createRevisionRecord(miningLeaseRenewalApplication, "GEOLOGIST_REVIEW", reviewQuarryLeaseApplicationGeologist.getGeologistRemarks(), userId);
                    createTask(applicationMaster, miningLeaseRenewalApplication, "APPLICANT", userId, miningLeaseRenewalApplication.getCreatedBy());

                    if (miningLeaseRenewalApplication.getApplicantEmail() != null) {
                        notificationClient.sendRevisionRequestNotification(
                                miningLeaseRenewalApplication.getApplicantEmail(),
                                miningLeaseRenewalApplication.getApplicantName(),
                                miningLeaseRenewalApplication.getApplicationNumber(),
                                "Geologist Review",
                                reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    }
                }
                case "Resubmit GR" -> {
                    miningLeaseRenewalApplication.setCurrentStatus("RESUBMIT GR");
                    miningLeaseRenewalApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseRenewalApplication.setGeologistReviewedAt(LocalDateTime.now());

                    createRevisionRecord(miningLeaseRenewalApplication, "GEOLOGIST_REVIEW", reviewQuarryLeaseApplicationGeologist.getGeologistRemarks(), userId);
                    createTask(applicationMaster, miningLeaseRenewalApplication, "APPLICANT", userId, miningLeaseRenewalApplication.getCreatedBy());

                    if (miningLeaseRenewalApplication.getApplicantEmail() != null) {
                        notificationClient.sendRevisionRequestNotification(
                                miningLeaseRenewalApplication.getApplicantEmail(),
                                miningLeaseRenewalApplication.getApplicantName(),
                                miningLeaseRenewalApplication.getApplicationNumber(),
                                "Geologist Review",
                                reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    }
                }
                case "FMFS Review" -> {
                    miningLeaseRenewalApplication.setCurrentStatus("ADDITIONAL DATA NEEDED FMFS");
                    miningLeaseRenewalApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseRenewalApplication.setGeologistReviewedAt(LocalDateTime.now());

                    createRevisionRecord(miningLeaseRenewalApplication, "GEOLOGIST_REVIEW", reviewQuarryLeaseApplicationGeologist.getGeologistRemarks(), userId);
                    createTask(applicationMaster, miningLeaseRenewalApplication, "APPLICANT", userId, userId);

                    if (miningLeaseRenewalApplication.getApplicantEmail() != null) {
                        notificationClient.sendRevisionRequestNotification(
                                miningLeaseRenewalApplication.getApplicantEmail(),
                                miningLeaseRenewalApplication.getApplicantName(),
                                miningLeaseRenewalApplication.getApplicationNumber(),
                                "Geologist Review",
                                reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    }
                }
                default -> throw new IllegalArgumentException("Application status not recognized");
            }
            miningLeaseRenewalApplicationRepository.save(miningLeaseRenewalApplication);
        }
        return mapper.toRenewalResponse(miningLeaseRenewalApplication);
    }

    @Transactional
    public MiningLeaseResponse submitLLC(@Valid MiningLeaseLLCRequest request) {
        MiningLeaseRenewalApplication miningLeaseRenewalApplication = null;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseRenewalApplication> miningLeaseRenewalApplication1 = miningLeaseRenewalApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            if (miningLeaseRenewalApplication1.isPresent()) {
                miningLeaseRenewalApplication = miningLeaseRenewalApplication1.get();
                ApplicationMaster applicationMaster = miningLeaseRenewalApplication.getApplicationMaster();
                applicationMaster.setCurrentStatus("LLC UPLOADED");
                miningLeaseRenewalApplication.setLlcMineEngineerDocId(request.getLLCDocId());
                miningLeaseRenewalApplication.setCurrentStatus("LLC UPLOADED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseRenewalApplicationRepository.save(miningLeaseRenewalApplication);

                if(miningLeaseRenewalApplication.getCreatedBy() != null) {
                    String title = "LLC has been uploaded by mine engineer.";
                    String message = "LLC for you application has been uploaded by mine engineer.";
                    String serviceId = "85";
                    notificationClient.sendUserNotification(title, message, miningLeaseRenewalApplication.getCreatedBy(), serviceId);
                }

            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        }
        return mapper.toRenewalResponse(miningLeaseRenewalApplication);
    }

    public MiningLeaseResponse submitFMFS(@Valid MiningLeaseFMFSRequest request, Long userId) {
        MiningLeaseRenewalApplication miningLeaseRenewalApplication = null;
        new TaskManagement();
        TaskManagement taskManagement;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseRenewalApplication> miningLeaseRenewalApplication1 = miningLeaseRenewalApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            List<TaskManagement> taskManagements = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRole(request.getApplicationNo(),"ASSIGNED", "MINE ENGINEER");
            if (miningLeaseRenewalApplication1.isPresent()) {
                if (taskManagements != null && !taskManagements.isEmpty()) {
                    taskManagement = taskManagements.getFirst();
                    miningLeaseRenewalApplication = miningLeaseRenewalApplication1.get();
                    ApplicationMaster applicationMaster = miningLeaseRenewalApplication.getApplicationMaster();
                    miningLeaseRenewalApplication.setFmfsDocId(request.getFmfsDocId());
                    miningLeaseRenewalApplication.setCurrentStatus("FMFS SUBMITTED");
                    applicationMaster.setCurrentStatus("FMFS SUBMITTED");
                    applicationMasterRepository.save(applicationMaster);
                    miningLeaseRenewalApplicationRepository.save(miningLeaseRenewalApplication);


                    createTask(applicationMaster, miningLeaseRenewalApplication, "MINE ENGINEER", userId, taskManagement.getAssignedToUserId());
                    UserWorkloadProjection userWorkloadProjection = miningLeaseRenewalApplicationRepository.findUserDetailsME(taskManagement.getAssignedToUserId());
                    if (userWorkloadProjection.getEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                miningLeaseRenewalApplication.getApplicantEmail(),
                                miningLeaseRenewalApplication.getApplicantCid(),
                                miningLeaseRenewalApplication.getApplicationNumber(),
                                "FMFS SUBMITTED",
                                "FMFS has been submitted by the client.");
                    }

                    if (userWorkloadProjection.getUserId() != null) {
                        String title = "Mining lease application has been assigned for FMFS review.";
                        String message = "Mining lease application has been  assigned for FMFS review.";
                        String serviceId = "85";
                        notificationClient.sendUserNotification(title, message, userWorkloadProjection.getUserId(), serviceId);
                    }
                }

            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        }
        return mapper.toRenewalResponse(miningLeaseRenewalApplication);
    }

    @Transactional
    public MiningLeaseResponse reviewApplicationChief(@Valid ReviewMiningLeaseApplicationChief request, Long userId) {
        log.info("Reviewing quarry lease application by Mining Chief user: {}", userId);

        MiningLeaseRenewalApplication app = findApplicationById(request.getId());
        ApplicationMaster master = app.getApplicationMaster();

        Long directorId = request.getDirectorId();

        if (request.getStatus() != null) {
            switch (request.getStatus()) {
                case "Approved" -> {
                    app.setCurrentStatus("FORWARDED TO DIRECTOR");
                    app.setRemarksChief(request.getRemarks());
                    app.setChiefReviewedAt(LocalDateTime.now());
                    app.setFmfsId(generateFMFSId());
                    if (master != null) {
                        master.setCurrentStatus("FORWARDED TO DIRECTOR");
                        applicationMasterRepository.save(master);
                    }

                    assert master != null;
                    createTask(master, app, "DIRECTOR", userId, directorId);

                    UserWorkloadProjection fetchDirectorDetails  = miningLeaseApplicationRepository.findUserDetails(directorId);

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber(),
                                "Director Review",
                                request.getRemarks());
                    }

                    if (fetchDirectorDetails.getEmail() != null) {
                        notificationClient.sendAssignmentNotification(
                                fetchDirectorDetails.getEmail(),
                                fetchDirectorDetails.getUsername(),
                                app.getApplicationNumber(),
                                "Director Review");
                    }
                }
                case "Rejected" -> {
                    app.setCurrentStatus("REJECTED");
                    app.setRemarksChief(request.getRemarks());
                    app.setChiefReviewedAt(LocalDateTime.now());
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
                case "Return" -> {
                    // Return to Mining Engineer
                    app.setCurrentStatus("ME_REVIEW");
                    app.setRemarksChief(request.getRemarks());
                    app.setChiefReviewedAt(LocalDateTime.now());

                    if (master != null) {
                        master.setCurrentStatus("ME_REVIEW");
                        applicationMasterRepository.save(master);
                    }

                    assert master != null;
                    createTask(master, app, "MINING_ENGINEER", userId, userId);
                }
                default -> throw new IllegalArgumentException("Application status not recognized");
            }
            miningLeaseRenewalApplicationRepository.save(app);
        }
        return mapper.toRenewalResponse(app);
    }

    private synchronized String generateFMFSId() {

        String year = String.valueOf(LocalDate.now().getYear());
        String prefix = "FMFS-" + year + "-";

        String lastFmfsId = miningLeaseApplicationRepository.findLastFmfsId(prefix);

        int nextNumber = 1;

        if (lastFmfsId != null && !lastFmfsId.isBlank()) {
            // Example: FMFS-2026-00012
            String lastNumber = lastFmfsId.substring(prefix.length());
            nextNumber = Integer.parseInt(lastNumber) + 1;
        }

        return prefix + String.format("%05d", nextNumber);
    }

    @Transactional
    public MiningLeaseResponse submitWorkOrder(@Valid MiningLeaseWorkOrderRequest request) {
        MiningLeaseRenewalApplication miningLeaseRenewalApplication = null;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseRenewalApplication> miningLeaseRenewalApplication1 = miningLeaseRenewalApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            if (miningLeaseRenewalApplication1.isPresent()) {
                miningLeaseRenewalApplication = miningLeaseRenewalApplication1.get();
                ApplicationMaster applicationMaster = miningLeaseRenewalApplication.getApplicationMaster();
                applicationMaster.setCurrentStatus("MINING LEASE APPROVED");
                miningLeaseRenewalApplication.setWorkOrderDocId(request.getWorkOrderDocId());
                miningLeaseRenewalApplication.setWorkOrderRemarks(request.getRemarks());
                miningLeaseRenewalApplication.setCurrentStatus("MINING LEASE APPROVED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseRenewalApplicationRepository.save(miningLeaseRenewalApplication);

                if(miningLeaseRenewalApplication.getCreatedBy() != null) {
                    String title = "Work order has been uploaded by mine engineer.";
                    String message = "Work order for your application has been uploaded by mine engineer. Your application for mining lease has been approved.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, miningLeaseRenewalApplication.getCreatedBy(), serviceId);
                }

            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        }
        return mapper.toRenewalResponse(miningLeaseRenewalApplication);
    }

    public MiningLeaseResponse submitMLA(@Valid MiningLeaseMLARequest request, Long userId) {
        MiningLeaseRenewalApplication miningLeaseRenewalApplication = null;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseRenewalApplication> miningLeaseRenewalApplication1 = miningLeaseRenewalApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            if (miningLeaseRenewalApplication1.isPresent()) {
                miningLeaseRenewalApplication = miningLeaseRenewalApplication1.get();
                ApplicationMaster applicationMaster = miningLeaseRenewalApplication.getApplicationMaster();
                miningLeaseRenewalApplication.setMlaDocId(request.getMlaDocId());
                miningLeaseRenewalApplication.setMlaStatus("SUBMITTED");
                miningLeaseRenewalApplication.setCurrentStatus("MLA SUBMITTED");
                applicationMaster.setCurrentStatus("MLA SUBMITTED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseRenewalApplicationRepository.save(miningLeaseRenewalApplication);


                List<String> status = new ArrayList<>();
                status.add("SUBMITTED");
                status.add("PAYMENT PENDING");
                List<TaskManagement> taskManagement = taskManagementRepository.findByApplicationNumberAndTaskStatusInAndAssignedToRole(miningLeaseRenewalApplication.getApplicationNumber(),status,"DIRECTOR");
                Long directorId = null;
                if (taskManagement != null) {
                    TaskManagement taskManagement1 = taskManagement.getFirst();
                    directorId = taskManagement1.getAssignedToUserId();
                }

                createTask(applicationMaster,miningLeaseRenewalApplication,"DIRECTOR", userId, directorId);

                UserWorkloadProjection assignedDirectorDetails = miningLeaseApplicationRepository.findUserDetails(directorId);
                if(assignedDirectorDetails.getUserId() != null) {
                    String title = "Mining lease application has been assigned for MLA review.";
                    String message = "Mining lease application has been  assigned for MLA review.";
                    String serviceId = "85";
                    notificationClient.sendUserNotification(title, message, assignedDirectorDetails.getUserId(), serviceId);
                }

            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        }
        return mapper.toRenewalResponse(miningLeaseRenewalApplication);
    }

    @Transactional
    public MiningLeaseResponse submitNoteSheet(@Valid MiningLeaseNoteSheetRequest request) {
        MiningLeaseRenewalApplication app = miningLeaseRenewalApplicationRepository
                .findByApplicationNumber(request.getApplicationNo())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        ApplicationMaster master = app.getApplicationMaster();
        app.setNoteSheetDocId(request.getNoteSheetDocId());
        app.setCurrentStatus("NOTE SHEET UPLOADED");

        if (master != null) {
            master.setCurrentStatus("NOTE SHEET UPLOADED");
            applicationMasterRepository.save(master);
        }

        if (app.getCreatedBy() != null) {
            notificationClient.sendUserNotification(
                    "Note sheet has been uploaded.",
                    "The note sheet for your application " + app.getApplicationNumber() + " has been uploaded by the Mine Engineer.",
                    app.getCreatedBy(), "78");
        }

        miningLeaseRenewalApplicationRepository.save(app);
        return mapper.toRenewalResponse(app);
    }

    @Transactional
    public MiningLeaseResponse saveDraft(@Valid RenewalMiningLeaseRequest request, Long userId) {
        Optional<MiningLeaseRenewalApplication> existing =
                miningLeaseRenewalApplicationRepository.findByApplicationNumber(request.getApplicationNumber());

        MiningLeaseRenewalApplication app;
        if (existing.isPresent()) {
            app = existing.get();
            if (!"DRAFT".equals(app.getCurrentStatus())) {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        } else {
            MiningLeaseApplication miningLeaseApplication = miningLeaseApplicationRepository
                    .findByApplicationNumber(request.getApplicationNumber())
                    .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

            app = new MiningLeaseRenewalApplication();
            app.setApplicationMaster(miningLeaseApplication.getApplicationMaster());
            app.setCreatedBy(userId);
        }

        mapRequestToApplication(request, app);
        app.setCurrentStatus("DRAFT");
        miningLeaseRenewalApplicationRepository.save(app);
        return mapper.toRenewalResponse(app);
    }

    @Transactional
    public MiningLeaseResponse resubmitApplication(@Valid RenewalApplicationResubmitRequest request, Long userId) {
        MiningLeaseRenewalApplication app = miningLeaseRenewalApplicationRepository
                .findByApplicationNumber(request.getApplicationNumber())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        if (!"RESUBMIT APP".equals(app.getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
        }

        app.setApplicantCid(request.getApplicantCid());
        app.setApplicantContact(request.getApplicantContact());
        app.setApplicantEmail(request.getApplicantEmail());
        app.setApplicantType(request.getApplicantType());
        app.setPostalAddress(request.getPostalAddress());
        app.setTelephoneNo(request.getTelephoneNo());
        app.setLeaseEndDate(request.getLeaseEndDate());
        app.setLeasePeriodYears(request.getLeasePeriodYears());
        app.setProposedLeaseRenewalPeriod(request.getProposedLeaseRenewalPeriod());
        app.setPlaceOfMiningActivity(request.getPlaceOfMiningActivity());
        app.setDungkhag(request.getDungkhag());
        app.setDepositAssessmentReportId(request.getDepositAssessmentReportId());
        app.setDeclarationStatus(request.isDeclarationStatus());

        if (request.getDzongkhag() != null && !request.getDzongkhag().isEmpty()) {
            DzongkhagLookup dzongkhag = dzongkhagLookupRepository
                    .findById(request.getDzongkhag())
                    .orElseThrow(() -> new RuntimeException("Invalid Dzongkhag ID"));
            app.setDzongkhag(dzongkhag);
        }

        if (request.getGewog() != null && !request.getGewog().isEmpty()) {
            GewogLookup gewog = (GewogLookup) gewogLookupRepository
                    .findByGewogId(request.getGewog())
                    .orElseThrow(() -> new RuntimeException("Invalid Gewog ID"));
            app.setGewog(gewog);
        }

        if (request.getNearestVillage() != null && !request.getNearestVillage().isEmpty()) {
            VillageLookup village = (VillageLookup) villageLookupRepository
                    .findByVillageSerialNo(Integer.parseInt(request.getNearestVillage()))
                    .orElseThrow(() -> new RuntimeException("Invalid Village ID"));
            app.setNearestVillage(village);
        }

        app.setCurrentStatus("RENEWAL APPLICATION");
        ApplicationMaster master = app.getApplicationMaster();
        if (master != null) {
            master.setCurrentStatus("RENEWAL APPLICATION");
            applicationMasterRepository.save(master);
        }

        // Re-assign to previously assigned ME
        List<TaskManagement> tasks = taskManagementRepository
                .findByApplicationNumberAndTaskStatusAndAssignedToRole(
                        app.getApplicationNumber(), "ASSIGNED", "MINE ENGINEER");
        Long meId = (tasks != null && !tasks.isEmpty()) ? tasks.getFirst().getAssignedToUserId() : null;

        if (master != null && meId != null) {
            createTask(master, app, "MINE ENGINEER", userId, meId);
            UserWorkloadProjection me = miningLeaseRenewalApplicationRepository.findUserDetailsME(meId);
            if (me != null && me.getUserId() != null) {
                notificationClient.sendUserNotification(
                        "Renewal application resubmitted.",
                        "The applicant has resubmitted the renewal application " + app.getApplicationNumber() + " for your review.",
                        me.getUserId(), "85");
            }
        }

        miningLeaseRenewalApplicationRepository.save(app);
        return mapper.toRenewalResponse(app);
    }

    @Transactional
    public MiningLeaseResponse resubmitFMFS(@Valid MiningLeaseFMFSRequest request, Long userId) {
        MiningLeaseRenewalApplication app = miningLeaseRenewalApplicationRepository
                .findByApplicationNumber(request.getApplicationNo())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        ApplicationMaster master = app.getApplicationMaster();
        app.setFmfsDocId(request.getFmfsDocId());
        app.setCurrentStatus("FMFS SUBMITTED");

        if (master != null) {
            master.setCurrentStatus("FMFS SUBMITTED");
            applicationMasterRepository.save(master);
        }

        List<TaskManagement> tasks = taskManagementRepository
                .findByApplicationNumberAndTaskStatusAndAssignedToRole(
                        app.getApplicationNumber(), "ASSIGNED", "MINE ENGINEER");
        Long meId = (tasks != null && !tasks.isEmpty()) ? tasks.getFirst().getAssignedToUserId() : null;

        if (master != null && meId != null) {
            createTask(master, app, "MINE ENGINEER", userId, meId);
            UserWorkloadProjection me = miningLeaseRenewalApplicationRepository.findUserDetailsME(meId);
            if (me != null && me.getUserId() != null) {
                notificationClient.sendUserNotification(
                        "FMFS resubmitted.",
                        "The applicant has resubmitted the FMFS for application " + app.getApplicationNumber() + ".",
                        me.getUserId(), "85");
            }
        }

        miningLeaseRenewalApplicationRepository.save(app);
        return mapper.toRenewalResponse(app);
    }

    @Transactional
    public MiningLeaseResponse applicantSignMLA(@Valid MiningLeaseMLARequest request, Long userId) {
        MiningLeaseRenewalApplication app = miningLeaseRenewalApplicationRepository
                .findByApplicationNumber(request.getApplicationNo())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        ApplicationMaster master = app.getApplicationMaster();
        app.setMlaDocId(request.getMlaDocId());
        app.setMlaStatus("APPLICANT SIGNED");
        app.setCurrentStatus("APPLICANT SIGNED MLA");

        if (master != null) {
            master.setCurrentStatus("APPLICANT SIGNED MLA");
            applicationMasterRepository.save(master);
        }

        // Notify assigned ME to proceed with work order
        List<String> statuses = List.of("MLA SUBMITTED", "SUBMITTED");
        List<TaskManagement> tasks = taskManagementRepository
                .findByApplicationNumberAndTaskStatusInAndAssignedToRole(
                        app.getApplicationNumber(), statuses, "MINE ENGINEER");
        Long meId = (tasks != null && !tasks.isEmpty()) ? tasks.getFirst().getAssignedToUserId() : null;

        if (master != null && meId != null) {
            createTask(master, app, "MINE ENGINEER", userId, meId);
            notificationClient.sendUserNotification(
                    "Applicant has signed the MLA.",
                    "The applicant has signed the MLA for application " + app.getApplicationNumber() + ". Please proceed with work order issuance.",
                    meId, "85");
        }

        miningLeaseRenewalApplicationRepository.save(app);
        return mapper.toRenewalResponse(app);
    }

    public SuccessResponse<List<MiningLeaseResponse>> getMyApplications(Long userId, Pageable pageable, String search) {
        Page<MiningLeaseRenewalApplication> page;

        if (search == null || search.isBlank()) {
            page = miningLeaseRenewalApplicationRepository.findByCreatedBy(userId, pageable);
        } else {
            page = miningLeaseRenewalApplicationRepository.findByCreatedByAndSearch(userId, search.trim(), pageable);
        }

        return SuccessResponse.fromPage("Applications fetched successfully", page.map(mapper::toRenewalResponse));
    }

    public SuccessResponse<List<MiningLeaseResponse>> getAllApplications(Pageable pageable, String search) {
        Page<MiningLeaseRenewalApplication> page;

        if (search == null || search.isBlank()) {
            page = miningLeaseRenewalApplicationRepository.findAll(pageable);
        } else {
            page = miningLeaseRenewalApplicationRepository.findAllBySearch(search.trim(), pageable);
        }

        return SuccessResponse.fromPage("Applications fetched successfully", page.map(mapper::toRenewalResponse));
    }

    private void mapRequestToApplication(RenewalMiningLeaseRequest request, MiningLeaseRenewalApplication app) {
        app.setApplicationNumber(request.getApplicationNumber());
        app.setApplicantCid(request.getApplicantCid());
        app.setApplicantContact(request.getApplicantContact());
        app.setApplicantEmail(request.getApplicantEmail());
        app.setApplicantType(request.getApplicantType());
        app.setPostalAddress(request.getPostalAddress());
        app.setTelephoneNo(request.getTelephoneNo());
        app.setLeaseEndDate(request.getLeaseEndDate());
        app.setLeasePeriodYears(request.getLeasePeriodYears());
        app.setProposedLeaseRenewalPeriod(request.getProposedLeaseRenewalPeriod());
        app.setPlaceOfMiningActivity(request.getPlaceOfMiningActivity());
        app.setDungkhag(request.getDungkhag());
        app.setDepositAssessmentReportId(request.getDepositAssessmentReportId());
        app.setDeclarationStatus(request.isDeclarationStatus());

        if (request.getDzongkhag() != null && !request.getDzongkhag().isEmpty()) {
            DzongkhagLookup dzongkhag = dzongkhagLookupRepository
                    .findById(request.getDzongkhag())
                    .orElseThrow(() -> new RuntimeException("Invalid Dzongkhag ID"));
            app.setDzongkhag(dzongkhag);
        }

        if (request.getGewog() != null && !request.getGewog().isEmpty()) {
            GewogLookup gewog = (GewogLookup) gewogLookupRepository
                    .findByGewogId(request.getGewog())
                    .orElseThrow(() -> new RuntimeException("Invalid Gewog ID"));
            app.setGewog(gewog);
        }

        if (request.getNearestVillage() != null && !request.getNearestVillage().isEmpty()) {
            VillageLookup village = (VillageLookup) villageLookupRepository
                    .findByVillageSerialNo(Integer.parseInt(request.getNearestVillage()))
                    .orElseThrow(() -> new RuntimeException("Invalid Village ID"));
            app.setNearestVillage(village);
        }
    }
}
