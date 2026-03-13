package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.ApplicationListResponse;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.dto.response.TaskManagementAssignedUser;
import com.mas.gov.bt.mas.primary.entity.*;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.exception.ResourceNotFoundException;
import com.mas.gov.bt.mas.primary.exception.UnauthorizedOperationException;
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
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Core service for mining Lease Application management.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MiningLeaseService {

    private static final String SERVICE_CODE = "MINING_LEASE";
    private static final int DEFAULT_TAT_DAYS = 2;
    //    private static final String SERVICE_NAME = "Mining Lease Application";

    private final MiningLeaseMapper mapper;

    private final MiningLeaseApplicationRepository miningLeaseApplicationRepository;

    private final DzongkhagLookupRepository dzongkhagLookupRepository;

    private final GewogLookupRepository  gewogLookupRepository;

    private final VillageLookupRepository villageLookupRepository;

    private final PaymentMasterRepository paymentMasterRepository;

    private final ApplicationMasterRepository applicationMasterRepository;

    private final TaskManagementRepository taskManagementRepository;

    private final NotificationClient notificationClient;

    private final ApplicationRevisionHistoryRepository revisionHistoryRepository;
    /**
     * Create a new application.
     * If applicationType is "Draft", save as draft. Otherwise, submit immediately.
     */
    @Transactional
    public MiningLeaseResponse createApplication(
            MiningLeaseApplicationRequest request,
            Long userId) {

        log.info("Creating/updating mining lease application for user: {}", userId);

        boolean isDraft = "Draft".equalsIgnoreCase(request.getApplicationType());
        LocalDateTime now = LocalDateTime.now();

        // =====================================================
        // 1. FETCH EXISTING APPLICATION OR CREATE NEW
        // =====================================================
        MiningLeaseApplication application =
                miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNo())
                        .orElse(new MiningLeaseApplication());

        boolean isNew = application.getId() == null;

        // =====================================================
        // 2. GENERATE APPLICATION NUMBER (ONLY IF NEW)
        // =====================================================
        String applicationNumber;

        if (request.getApplicationNo() == null) {
            applicationNumber = isDraft
                    ? generateDraftApplicationNumber()
                    : generateApplicationNumber();
        } else {
            if(Objects.equals(request.getApplicationType(), "Submitted") && Objects.equals(application.getCurrentStatus(), "DRAFT")) {
                applicationNumber = generateApplicationNumber();
            }else {
                applicationNumber = request.getApplicationNo();
            }
        }

        // =====================================================
        // 3. CREATE MASTER ONLY FOR NEW APPLICATION
        // =====================================================
        if (isNew) {
            ApplicationMaster master =
                    createApplicationMaster(applicationNumber, userId);

            application.setApplicationNumber(applicationNumber);
            application.setApplicationMaster(master);
            application.setApplicantUserId(userId);
            application.setCreatedBy(userId);
            application.setCurrentStatus("DRAFT");
            application.setIsActive(true);
        }

        // =====================================================
        // 4. UPDATE ENTITY FROM REQUEST (SAFE UPDATE)
        // =====================================================
        mapper.updateEntityFromRequest(request, application);

        if (request.getDzongkhag() != null && !request.getDzongkhag().isEmpty()) {
            DzongkhagLookup dzongkhag = dzongkhagLookupRepository
                    .findById(request.getDzongkhag())
                    .orElseThrow(() -> new RuntimeException("Invalid Dzongkhag ID"));

            application.setDzongkhag(dzongkhag);
        }

        if (request.getGewog() != null && !request.getGewog().isEmpty()) {
            GewogLookup gewog = (GewogLookup) gewogLookupRepository
                    .findByGewogId(request.getGewog())
                    .orElseThrow(() -> new RuntimeException("Invalid gewog ID"));

            application.setGewog(gewog);
        }

        if (request.getNearestVillage() != null && !request.getNearestVillage().isEmpty()) {
            VillageLookup villageLookup = (VillageLookup) villageLookupRepository
                    .findByVillageSerialNo(Integer.parseInt(request.getNearestVillage()))
                    .orElseThrow(() -> new RuntimeException("Invalid village ID"));

            application.setNearestVillage(villageLookup);
        }

        application.setUpdatedBy(userId);

        PaymentMaster paymentMaster =
                paymentMasterRepository.findByServiceName(SERVICE_CODE);

        // =====================================================
        // 5. DRAFT FLOW
        // =====================================================
        if (isDraft) {

            application.setCurrentStatus("DRAFT");

            // Draft does not trigger workflow or notifications
            miningLeaseApplicationRepository.save(application);

            log.info("Application saved as DRAFT: {}", application.getApplicationNumber());

            return mapper.toResponse(application);
        }

        // =====================================================
        // 6. SUBMIT FLOW
        // =====================================================

        // Safety checks
        if (!userId.equals(application.getApplicantUserId())) {
            throw new UnauthorizedOperationException(
                    "You are not authorized to submit this application");
        }

        boolean feeRequired =
                paymentMaster != null &&
                        Boolean.TRUE.equals(paymentMaster.getIsApplicationFeeEnabled());

        Long directorId;
        if (feeRequired) {
            application.setCurrentStatus("PAYMENT PENDING");
            application.setApplicationFeesAmount(paymentMaster.getApplicationFee());
            application.setApplicationFeesRequired(true);
        } else {
            application.setCurrentStatus("SUBMITTED");
        }

        application.setSubmittedAt(now);
        application.setApplicationNumber(applicationNumber);
        // =====================================================
        // 6. UPDATE APPLICATION MASTER
        // =====================================================
        ApplicationMaster master = application.getApplicationMaster();

        master.setApplicationNumber(applicationNumber);
        master.setCurrentStatus(application.getCurrentStatus());
        master.setSubmittedAt(now);
        applicationMasterRepository.save(master);

        // Resubmission of application to director after application submission from client
        List<TaskManagement> getAssignedDirector =
                taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRole
                        (application.getApplicationNumber(),"GR SUBMITTED", "DIRECTOR");
        TaskManagement taskManagement = getAssignedDirector.getFirst();
        directorId = taskManagement.getAssignedToUserId();

        createTask(master,application,"DIRECTOR",userId,directorId);

        // =====================================================
        // 7. NOTIFICATIONS
        // =====================================================

        if (application.getApplicantEmail() != null) {
            if (feeRequired) {
                notificationClient.sendApplicationFeeRequiredNotification(
                        application.getApplicantEmail(),
                        application.getApplicantName(),
                        application.getApplicationNumber());
            } else {
                notificationClient.sendApplicationSubmittedNotification(
                        application.getApplicantEmail(),
                        application.getApplicantName(),
                        application.getApplicationNumber());
            }
        }

        if (application.getApplicantUserId() != null) {
            String title = feeRequired
                    ? "Application pending for payment"
                    : "Application submitted successfully";

            String message = feeRequired
                    ? "Your application will be processed after payment."
                    : "Your quarry lease application has been submitted.";

            notificationClient.sendUserNotification(
                    title,
                    message,
                    application.getApplicantUserId(),
                    "78"
            );
        }

        // =====================================================
        // 9. FINAL SAVE (ONLY ONCE)
        // =====================================================
        miningLeaseApplicationRepository.save(application);

        log.info("Application submitted successfully: {}",
                application.getApplicationNumber());

        return mapper.toResponse(application);
    }

    @Transactional
    private void createTask(ApplicationMaster master, MiningLeaseApplication application, String role, Long userId, Long directorId) {
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

    @Transactional
    private ApplicationMaster createApplicationMaster(String applicationNumber, Long userId) {
        ApplicationMaster master = new ApplicationMaster();
        master.setApplicationNumber(applicationNumber);
        master.setServiceCode(SERVICE_CODE);
        master.setApplicantUserId(userId);
        master.setCurrentStatus("GR SUBMITTED");
        return applicationMasterRepository.save(master);
    }

    @Transactional
    private synchronized String generateApplicationNumber() {
        int year = Year.now().getValue();
        String prefix = String.format("ML-%d-", year);

        // Get max sequence from database for current year
        Integer maxSequence = miningLeaseApplicationRepository.findMaxSequenceByPrefix(prefix);
        long nextSequence = (maxSequence == null ? 0 : maxSequence) + 1;

        return String.format("ML-%d-%06d", year, nextSequence);
    }

    @Transactional
    private synchronized String generateDraftApplicationNumber() {
        int year = Year.now().getValue();
        String prefix = String.format("DRAFT-%d-", year);

        Integer maxSequence =
                miningLeaseApplicationRepository.findMaxDraftSequenceByPrefix(prefix);

        long nextSequence = (maxSequence == null ? 0 : maxSequence) + 1;

        return String.format("DRAFT-%d-%06d", year, nextSequence);
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

    @Transactional
    public MiningLeaseResponse submitGR(@Valid MiningLeaseGRRequest request, Long userId) {
        // 1.
        // ======== SAVE MINING LEASE GR SUBMITTED BY APPLICANT ====== //
        MiningLeaseApplication miningLeaseApplication = new MiningLeaseApplication();

        miningLeaseApplication.setExpPermitNo(request.getExpPermitNo());
        miningLeaseApplication.setFileUploadIdGr(request.getGRDocId());
        miningLeaseApplication.setFileUploadIdKmz(request.getKmzDocId());
        miningLeaseApplication.setApplicationType(request.getApplicationType());
        miningLeaseApplication.setApplicationNumber(generateApplicationNumber());
        miningLeaseApplication.setApplicantCid(request.getApplicantCid());
        miningLeaseApplication.setApplicantUserId(userId);
        miningLeaseApplication.setApplicantType(request.getApplicantType());
        miningLeaseApplication.setApplicantEmail(request.getApplicantEmail());
        miningLeaseApplication.setApplicantName(request.getApplicantName());
        miningLeaseApplication.setLicenseNo(request.getLicenseNo());
        miningLeaseApplication.setCompanyName(request.getCompanyName());
        miningLeaseApplication.setCurrentStatus("GR SUBMITTED");
        miningLeaseApplication.setCreatedBy(userId);

        miningLeaseApplicationRepository.save(miningLeaseApplication);

        // =====================================================
        // 2. ASSIGN DIRECTOR
        // =====================================================
        UserWorkloadProjection assignedDirector = assignDirector();

        // =====================================================
        // 3. Application master and create task for director
        // =====================================================
        ApplicationMaster master = createApplicationMaster(miningLeaseApplication.getApplicationNumber(), userId);
        miningLeaseApplication.setApplicationMaster(master);
        createTask(master,miningLeaseApplication,"DIRECTOR",userId,assignedDirector.getUserId());

        if (assignedDirector.getEmail() != null) {
            notificationClient.sendMailToDirectorAssigned(
                    assignedDirector.getEmail(),
                    assignedDirector.getUsername(),
                    miningLeaseApplication.getApplicationNumber());
        }

        if(assignedDirector.getUserId()!= null) {
            String title = "Mining lease application has been assigned.";
            String message = "An application for mining lease has been assigned for review. Application No. "+ miningLeaseApplication.getApplicationNumber()+" Please login in review the Geological report.";
            String serviceId = "78";
            notificationClient.sendUserNotification(title, message, assignedDirector.getUserId(), serviceId);
        }else {
            throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }

        return mapper.toResponse(miningLeaseApplication);
    }

    /**
     * Get all applications (for Agency users).
     */
    @Transactional(readOnly = true)
    public Page<ApplicationListResponse> getAllApplications(
            Long currentUserId,
            Pageable pageable) {

        Page<MiningLeaseApplication> applications =
                miningLeaseApplicationRepository.findApplicationsAssignedToUser(currentUserId, pageable);

        return applications.map(mapper::toListResponse);
    }

    /**
     * Get applications for an applicant.
     */
    @Transactional(readOnly = true)
    public Page<ApplicationListResponse> getMyApplications(Long userId, Pageable pageable) {
        List<String> ApplicationStatus = List.of(
                "GR SUBMITTED",
                "LLC UPLOADED",
                "SUBMITTED",
                "ASSIGNED",
                "DRAFT",
                "PAYMENT PENDING",
                "GEOLOGIST_REVIEW",
                "ACCEPTED PFS",
                "ADDITIONAL DATA NEEDED",
                "MA SUBMITTED",
                "PA/FC SUBMITTED",
                "APPROVED GR",
                "NOTE SHEET UPLOADED",
                "GR SUBMITTED",
                "FMFS SUBMITTED",
                "MLA SUBMITTED",
                "APPROVED BY DIRECTOR",
                "RESUBMIT PFS",
                "RESUBMITTED PFS",
                "RESUBMIT GR",
                "RESUBMITTED GR",
                "RESUBMIT FMFS",
                "MPCD ASSIGNED",
                "RESUBMITTED FMFS");
        Page<MiningLeaseApplication> applications = miningLeaseApplicationRepository.findByApplicantUserIdAndStatusIn(userId,ApplicationStatus, pageable);
        return applications.map(mapper::toListResponse);
    }

    /**
     * Get application by application number.
     * Agency users can view any application, regular users can only view their own.
     */
    @Transactional(readOnly = true)
    public MiningLeaseResponse getApplicationByNumber(String applicationNumber, Long userId, boolean isAgencyUser) {
        MiningLeaseApplication application = miningLeaseApplicationRepository.findByApplicationNumber(applicationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationNumber));


        // Agency users can view any application, others can only view their own
        if (!isAgencyUser && !application.getApplicantUserId().equals(userId)) {
            throw new UnauthorizedOperationException("You are not authorized to view this application");
        }

        return mapper.toResponse(application);
    }

    /**
     * Get archived applications (APPROVED or REJECTED) for all users (Agency users).
     */
    @Transactional(readOnly = true)
    public Page<ApplicationListResponse> getArchivedApplications(Pageable pageable) {
        List<String> archivedStatuses = List.of("MINING LEASE APPROVED", "REJECTED");
        Page<MiningLeaseApplication> applications = miningLeaseApplicationRepository.findByStatusIn(archivedStatuses, pageable);
        return applications.map(mapper::toListResponse);
    }

    /**
     * Get archived applications (APPROVED or REJECTED) for a specific user.
     */
    @Transactional(readOnly = true)
    public Page<ApplicationListResponse> getMyArchivedApplications(Long userId, Pageable pageable) {
        List<String> archivedStatuses = List.of("MINING LEASE APPROVED", "REJECTED", "PAID", "BG SUBMITTED");
        Page<MiningLeaseApplication> applications = miningLeaseApplicationRepository.findByApplicantUserIdAndStatusIn(userId, archivedStatuses, pageable);
        return applications.map(mapper::toListResponse);
    }

    public SuccessResponse<List<MiningLeaseResponse>> getArchivedApplicationToMPCD(Long userId, Pageable pageable, String search) {

        Page<MiningLeaseApplication> page;

        if (search == null || search.isBlank()) {

            page = miningLeaseApplicationRepository
                    .findArchivedAssignedToUserMPCD(userId, pageable);

        } else {

            page = miningLeaseApplicationRepository
                    .findArchivedAssignedToUserAndSearchMPCD(
                            userId,
                            search.trim(),
                            pageable
                    );
        }

        Page<MiningLeaseResponse> responsePage =
                page.map(mapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Transactional
    public void reassignTaskGeologist(ReassignTaskRequest request, Long userId) {

        List<String> assignedRoles = new ArrayList<>();
        assignedRoles.add("GEOLOGIST");
        List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleIn(request.getApplicationNumber(),"ASSIGNED",assignedRoles);

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
            String message = "An application for quarry lease has been assigned for review. Application No. "+request.getApplicationNumber()+" Please login in review the application";
            String serviceId = "78";
            notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId);
        }else {
            throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }


        log.info("Geologist task {} reassigned to user {}", taskManagement.getId(), request.getNewAssigneeUserId());
    }

    @Transactional
    public void reassignTaskMPCD(@Valid ReassignTaskRequest request, Long userId) {
        List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRole(request.getApplicationNumber(),"MPCD ASSIGNED","MPCD_FOCAL");

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
            String message = "An application for quarry lease has been assigned for review. Application No. "+ request.getApplicationNumber()+" Please login in review the application";
            String serviceId = "78";
            notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId);
        }else {
            throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }

        log.info("MPCD task {} reassigned to user {}", taskManagement.getId(), request.getNewAssigneeUserId());
    }

    @Transactional
    public void reassignTaskME(@Valid ReassignTaskRequest request, Long userId) {
        log.info("Reassigning task by mining engineer: {} by user: {}", request.getApplicationNumber(), userId);
        List<TaskManagement> tasks = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRole(request.getApplicationNumber(),"FMFS SUBMITTED","MINE ENGINEER");

        if (tasks.isEmpty()) {
            throw new RuntimeException("No tasks found");
        }

        for (TaskManagement taskManagement : tasks) {
            taskManagement.setAssignedToUserId(request.getNewAssigneeUserId());
            taskManagement.setAssignedByUserId(userId);
            taskManagement.setAssignedAt(LocalDateTime.now());
            taskManagement.setReassignmentCount(
                    taskManagement.getReassignmentCount() + 1);
            taskManagement.setActionRemarks(request.getRemarks());

        }

        taskManagementRepository.saveAll(tasks);

        TaskManagement firstTask = tasks.getFirst();

        UserWorkloadProjection userDetails = miningLeaseApplicationRepository.findUserDetails(request.getNewAssigneeUserId());
        notificationClient.sendTaskReassignmentNotification(
                userDetails.getEmail(), userDetails.getUsername(),
                firstTask.getApplicationNumber(),
                firstTask.getAssignedToRole());

        if(userDetails.getUserId()!= null) {
            String title = "An new application has been reassigned.";
            String message = "An application for quarry lease has been assigned for review. Application No. "+request.getApplicationNumber()+" Please login in review the application";
            String serviceId = "78";
            notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId);
        }else {
            throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }

        log.info("Mining engineer task {} reassigned to user {}", request.getApplicationNumber(), request.getNewAssigneeUserId());
    }

    @Transactional
    public void reassignTaskMineChief(@Valid ReassignTaskRequest request, Long userId) {
        log.info("Mine chief reassigning task: {} by user: {}", request.getApplicationNumber(), userId);

        List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRole(request.getApplicationNumber(),"MINING_CHIEF_REVIEW","MINING_CHIEF_REVIEW");

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
            String message = "An application for quarry lease has been assigned for review. Application No. "+request.getApplicationNumber()+" Please login in review the application";
            String serviceId = "78";
            notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId);
        }else {
            throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }

        log.info("Task {} reassigned to user {}", taskManagement.getId(), request.getNewAssigneeUserId());
    }

    @Transactional
    public MiningLeaseResponse submitPAFC(MiningLeasePAFCRequest request, Long userId) {
        MiningLeaseApplication miningLeaseApplication = null;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseApplication> miningLeaseApplication1 = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            if (miningLeaseApplication1.isPresent()) {
                miningLeaseApplication = miningLeaseApplication1.get();
                ApplicationMaster applicationMaster = miningLeaseApplication.getApplicationMaster();
                miningLeaseApplication.setFileUploadIdPA(request.getPaDocId());
                miningLeaseApplication.setFileUploadIdFC(request.getFcDocId());
                miningLeaseApplication.setFileUploadIdPublicClearance(request.getPublicClearanceDocId());
                miningLeaseApplication.setCurrentStatus("PA/FC SUBMITTED");
                applicationMaster.setCurrentStatus("PA/FC SUBMITTED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseApplicationRepository.save(miningLeaseApplication);

                createTask(applicationMaster,miningLeaseApplication,"APPLICANT", userId, userId);

                if(userId != null) {
                    String title = "Submit Geological Report to proceed further.";
                    String message = "You have upload PA / FC for you application now you have to upload FMFS to proceed further.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, userId, serviceId);
                }

            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        }
        return mapper.toResponse(miningLeaseApplication);
    }

    @Transactional
    public MiningLeaseResponse submitFMFS(@Valid MiningLeaseFMFSRequest request, Long userId) {
        MiningLeaseApplication miningLeaseApplication = null;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseApplication> miningLeaseApplication1 = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            if (miningLeaseApplication1.isPresent()) {
                miningLeaseApplication = miningLeaseApplication1.get();
                ApplicationMaster applicationMaster = miningLeaseApplication.getApplicationMaster();
                miningLeaseApplication.setFmfsDocId(request.getFmfsDocId());
                miningLeaseApplication.setCurrentStatus("FMFS SUBMITTED");
                applicationMaster.setCurrentStatus("FMFS SUBMITTED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseApplicationRepository.save(miningLeaseApplication);

                UserWorkloadProjection assignedMineEngineer = miningLeaseApplicationRepository.findLeastBusyMineEngineer(20L);

                createTask(applicationMaster,miningLeaseApplication,"MINE ENGINEER", userId, assignedMineEngineer.getUserId());

                if (assignedMineEngineer.getEmail() != null) {
                    notificationClient.sendStatusUpdateNotification(
                            miningLeaseApplication.getApplicantEmail(),
                            miningLeaseApplication.getApplicantCid(),
                            miningLeaseApplication.getApplicationNumber(),
                            "FMFS SUBMITTED",
                            "FMFS has been submitted by the client.");
                }

                if(assignedMineEngineer.getUserId() != null) {
                    String title = "Mining lease application has been assigned for FMFS review.";
                    String message = "Mining lease application has been  assigned for FMFS review.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, assignedMineEngineer.getUserId(), serviceId);
                }

            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        }
        return mapper.toResponse(miningLeaseApplication);
    }

    @Transactional
    public MiningLeaseResponse submitBankDetails(@Valid MiningLeaseBankDetailsRequest request) {
        MiningLeaseApplication miningLeaseApplication = null;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseApplication> miningLeaseApplication1 = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            if (miningLeaseApplication1.isPresent()) {
                miningLeaseApplication = miningLeaseApplication1.get();
                ApplicationMaster applicationMaster = miningLeaseApplication.getApplicationMaster();
                miningLeaseApplication.setBankGuarantorDocId(request.getBankGuarantorDocId());
                miningLeaseApplication.setUpfrontPaymentAmount(request.getUpfrontPaymentAmount());
                miningLeaseApplication.setCurrentStatus("BG SUBMITTED");
                applicationMaster.setCurrentStatus("BG SUBMITTED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseApplicationRepository.save(miningLeaseApplication);
            }
        }
        return mapper.toResponse(miningLeaseApplication);
    }

    @Transactional
    public MiningLeaseResponse deleteFileUpload(DeleteFileRequest request) {
        MiningLeaseApplication miningLeaseApplication = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNumber()).orElseThrow();

        if (Objects.equals(request.getFileType(), "PFS")) {
            miningLeaseApplication.setPfsDocId(null);
        } else if (Objects.equals(request.getFileType(), "GR")) {
            miningLeaseApplication.setFileUploadIdGr(null);
        } else {
            miningLeaseApplication.setFmfsDocId(null);
        }
        miningLeaseApplicationRepository.save(miningLeaseApplication);
        return mapper.toResponse(miningLeaseApplication);
    }

    public SuccessResponse<List<MiningLeaseResponse>> getAssignedToDirector(Long userId, Pageable pageable, String search) {
        Page<MiningLeaseApplication> page;

        if (search == null || search.isBlank()) {

            page = miningLeaseApplicationRepository
                    .findAssignedToUserDirector(userId, pageable);

        } else {

            page = miningLeaseApplicationRepository
                    .findAssignedToUserAndSearchDirector(
                            userId,
                            search.trim(),
                            pageable
                    );
        }

        Page<MiningLeaseResponse> responsePage =
                page.map(mapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Transactional
    public MiningLeaseResponse reviewApplicationDirector(@Valid ReviewMiningLeaseApplicationDirector request, Long userId) {
        log.info("Reviewing mining lease application by Director user: {}", userId);

        MiningLeaseApplication app = findApplicationById(request.getId());
        ApplicationMaster master = app.getApplicationMaster();

        completeCurrentTask(master,request.getStatus(), request.getRemarks());

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
            miningLeaseApplicationRepository.save(app);
        }
        return mapper.toResponse(app);
    }

    private MiningLeaseApplication findApplicationById(Long id) {
        return miningLeaseApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));
    }

    @Transactional
    private void completeCurrentTask(ApplicationMaster master, String action, String remarks) {
        if (master == null) return;

        List<TaskManagement> pendingTasks = taskManagementRepository.findByApplicationNumber(master.getApplicationNumber());
        pendingTasks.stream()
                .filter(t -> "PENDING".equals(t.getTaskStatus()))
                .findFirst()
                .ifPresent(task -> {
                    task.setTaskStatus("COMPLETED");
                    task.setActionTaken(action);
                    task.setActionRemarks(remarks);
                    taskManagementRepository.save(task);
                });
    }

    @Transactional
    public MiningLeaseResponse assignApplicationDirector(@Valid AssignTaskDirector request, Long userId) {
        MiningLeaseApplication miningLeaseApplication ;

        miningLeaseApplication = findApplicationById(request.getApplicationId());
        ApplicationMaster applicationMaster ;
        if(miningLeaseApplication != null) {
            applicationMaster = miningLeaseApplication.getApplicationMaster();
        }else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
        }
        if (request.getMpcdFocalId() != null && request.getGeologistId() != null) {
            miningLeaseApplication.setCurrentStatus("MPCD ASSIGNED");
            applicationMaster.setCurrentStatus("MPCD ASSIGNED");
        }else {
            miningLeaseApplication.setCurrentStatus("ASSIGNED");
            applicationMaster.setCurrentStatus("ASSIGNED");
        }
        applicationMasterRepository.save(applicationMaster);
        miningLeaseApplicationRepository.save(miningLeaseApplication);

        if (request.getGeologistId() != null) {
            createTask(
                    applicationMaster,
                    miningLeaseApplication,
                    "GEOLOGIST",
                    userId,
                    request.getGeologistId());

            UserWorkloadProjection userGeologist = miningLeaseApplicationRepository.findUserDetailsME(request.getGeologistId());

            if (miningLeaseApplication.getApplicantEmail() != null) {
                notificationClient.sendStatusUpdateNotification(
                        miningLeaseApplication.getApplicantEmail(),
                        miningLeaseApplication.getApplicantName(),
                        miningLeaseApplication.getApplicationNumber(),
                        miningLeaseApplication.getCurrentStatus(),
                        "ASSIGNED");
            }

            if (userGeologist != null) {
                notificationClient.sendAssignmentNotification(
                        userGeologist.getEmail(),
                        userGeologist.getUsername(),
                        miningLeaseApplication.getApplicationNumber(),
                        "ASSIGNED");

                String title = "A new mining application has been assigned.";
                String message = "A new mining application has been assigned.";
                String serviceId = "78";
                notificationClient.sendUserNotification(title, message, userGeologist.getUserId(), serviceId);
            }
        }

        if(request.getMpcdFocalId() != null){

            createTask(
                    applicationMaster,
                    miningLeaseApplication,
                    "MPCD_FOCAL",
                    userId,
                    request.getMpcdFocalId());

            UserWorkloadProjection userGeologist = miningLeaseApplicationRepository.findUserDetailsME(request.getMpcdFocalId());

            if (miningLeaseApplication.getApplicantEmail() != null) {
                notificationClient.sendStatusUpdateNotification(
                        miningLeaseApplication.getApplicantEmail(),
                        miningLeaseApplication.getApplicantName(),
                        miningLeaseApplication.getApplicationNumber(),
                        miningLeaseApplication.getCurrentStatus(),
                        "ASSIGNED");
            }

            if (userGeologist != null) {
                notificationClient.sendAssignmentNotification(
                        userGeologist.getEmail(),
                        userGeologist.getUsername(),
                        miningLeaseApplication.getApplicationNumber(),
                        "ASSIGNED");

                String title = "A new mining application has been assigned.";
                String message = "A new mining application has been assigned.";
                String serviceId = "78";
                notificationClient.sendUserNotification(title, message, userGeologist.getUserId(), serviceId);
            }
        }
        return mapper.toResponse(miningLeaseApplication);
    }

    @Transactional
    public MiningLeaseResponse reviewApplicationGeologist(@Valid ReviewMiningLeaseApplicationGeologist reviewQuarryLeaseApplicationGeologist, Long userId) {
        log.info("Reviewing mining lease application by Geologist user: {}", userId);

        MiningLeaseApplication miningLeaseApplication = findApplicationById(reviewQuarryLeaseApplicationGeologist.getId());
        ApplicationMaster applicationMaster = miningLeaseApplication.getApplicationMaster();

        // Complete current task
        completeCurrentTask(applicationMaster, reviewQuarryLeaseApplicationGeologist.getStatus(), reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());

        if(reviewQuarryLeaseApplicationGeologist.getStatus() != null) {
            switch (reviewQuarryLeaseApplicationGeologist.getStatus()) {
                case "ACCEPTED" -> {
                    if (Objects.equals(miningLeaseApplication.getCurrentStatus(), "ACCEPTED PFS MPCD")) {
                        miningLeaseApplication.setCurrentStatus("APPROVED");
                    }else {
                        miningLeaseApplication.setCurrentStatus("ACCEPTED PFS");
                    }

                    miningLeaseApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseApplication.setGeologistReviewedAt(LocalDateTime.now());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("ACCEPTED PFS");
                        applicationMasterRepository.save(applicationMaster);
                    }

                    if (miningLeaseApplication.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                miningLeaseApplication.getApplicantEmail(),
                                miningLeaseApplication.getApplicantName(),
                                miningLeaseApplication.getApplicationNumber(),
                                "Mining Engineer Review",
                                "Your application has been forwarded to the Mining Engineer for review.");
                    }

                    String title = "Application status updated.";
                    String message = "Your application has been forwarded to geologist to review.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, miningLeaseApplication.getApplicantUserId(), serviceId);
                }
                case "ACCEPTED GR" -> {
                    miningLeaseApplication.setCurrentStatus("APPROVED GR");
                    miningLeaseApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseApplication.setGeologistReviewedAt(LocalDateTime.now());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("APPROVED GR");
                        applicationMasterRepository.save(applicationMaster);
                    }

                    assert applicationMaster != null;
                    createTask(applicationMaster, miningLeaseApplication, "APPLICANT", userId, miningLeaseApplication.getApplicantUserId());

                    if (miningLeaseApplication.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                miningLeaseApplication.getApplicantEmail(),
                                miningLeaseApplication.getApplicantName(),
                                miningLeaseApplication.getApplicationNumber(),
                                "GR APPROVED",
                                "Geological Report has been accepted. Please upload mining lease application and PFS to proceed further.");
                    }

                    if(miningLeaseApplication.getApplicantUserId() != null) {
                        String title = "Geological report has been approved successfully.";
                        String message = "Geological Report has been accepted. Please upload mining lease application and PFS to proceed further.";
                        String serviceId = "78";
                        notificationClient.sendUserNotification(title, message, miningLeaseApplication.getApplicantUserId(), serviceId);
                    }else {
                        throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
                    }
                }
                case "ACCEPTED FMFS" -> {
                    miningLeaseApplication.setCurrentStatus("ACCEPTED FMFS");
                    miningLeaseApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseApplication.setGeologistReviewedAt(LocalDateTime.now());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("ACCEPTED FMFS");
                        applicationMasterRepository.save(applicationMaster);
                    }

                    assert applicationMaster != null;
                    createTask(applicationMaster, miningLeaseApplication, "MINING_ENGINEER", userId, userId);

                    if (miningLeaseApplication.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                miningLeaseApplication.getApplicantEmail(),
                                miningLeaseApplication.getApplicantName(),
                                miningLeaseApplication.getApplicationNumber(),
                                "Mining Engineer Review",
                                "Your application has been forwarded to the Mining Engineer for review.");
                    }
                }
                case "Rejected" -> {
                    miningLeaseApplication.setCurrentStatus("REJECTED");
                    miningLeaseApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseApplication.setGeologistReviewedAt(LocalDateTime.now());
                    miningLeaseApplication.setRejectedAt(LocalDateTime.now());
                    miningLeaseApplication.setRejectionReason(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("REJECTED");
                        applicationMasterRepository.save(applicationMaster);
                    }

                    if (miningLeaseApplication.getApplicantEmail() != null) {
                        notificationClient.sendRejectionNotification(
                                miningLeaseApplication.getApplicantEmail(),
                                miningLeaseApplication.getApplicantName(),
                                miningLeaseApplication.getApplicationNumber(),
                                reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    }
                }
                case "Resubmit PFS" -> {
                    miningLeaseApplication.setCurrentStatus("RESUBMIT PFS");
                    miningLeaseApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseApplication.setGeologistReviewedAt(LocalDateTime.now());

                    createRevisionRecord(miningLeaseApplication, "GEOLOGIST_REVIEW", reviewQuarryLeaseApplicationGeologist.getGeologistRemarks(), userId);
                    createTask(applicationMaster, miningLeaseApplication, "APPLICANT", userId, miningLeaseApplication.getApplicantUserId());

                    if (miningLeaseApplication.getApplicantEmail() != null) {
                        notificationClient.sendRevisionRequestNotification(
                                miningLeaseApplication.getApplicantEmail(),
                                miningLeaseApplication.getApplicantName(),
                                miningLeaseApplication.getApplicationNumber(),
                                "Geologist Review",
                                reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    }
                }
                case "Resubmit GR" -> {
                    miningLeaseApplication.setCurrentStatus("RESUBMIT GR");
                    miningLeaseApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseApplication.setGeologistReviewedAt(LocalDateTime.now());

                    createRevisionRecord(miningLeaseApplication, "GEOLOGIST_REVIEW", reviewQuarryLeaseApplicationGeologist.getGeologistRemarks(), userId);
                    createTask(applicationMaster, miningLeaseApplication, "APPLICANT", userId, miningLeaseApplication.getApplicantUserId());

                    if (miningLeaseApplication.getApplicantEmail() != null) {
                        notificationClient.sendRevisionRequestNotification(
                                miningLeaseApplication.getApplicantEmail(),
                                miningLeaseApplication.getApplicantName(),
                                miningLeaseApplication.getApplicationNumber(),
                                "Geologist Review",
                                reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    }
                }
                case "FMFS Review" -> {
                    miningLeaseApplication.setCurrentStatus("ADDITIONAL DATA NEEDED FMFS");
                    miningLeaseApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseApplication.setGeologistReviewedAt(LocalDateTime.now());

                    createRevisionRecord(miningLeaseApplication, "GEOLOGIST_REVIEW", reviewQuarryLeaseApplicationGeologist.getGeologistRemarks(), userId);
                    createTask(applicationMaster, miningLeaseApplication, "APPLICANT", userId, userId);

                    if (miningLeaseApplication.getApplicantEmail() != null) {
                        notificationClient.sendRevisionRequestNotification(
                                miningLeaseApplication.getApplicantEmail(),
                                miningLeaseApplication.getApplicantName(),
                                miningLeaseApplication.getApplicationNumber(),
                                "Geologist Review",
                                reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    }
                }
                default -> throw new IllegalArgumentException("Application status not recognized");
            }
            miningLeaseApplicationRepository.save(miningLeaseApplication);
        }
        return mapper.toResponse(miningLeaseApplication);
    }

    @Transactional
    private void createRevisionRecord(MiningLeaseApplication miningLeaseApplication, String geologistReview, String geologistRemarks, Long userId) {
        long revisionCount = revisionHistoryRepository.countByApplicationIdAndRevisionStage(miningLeaseApplication.getId(), geologistReview);

        ApplicationRevisionHistory revision = ApplicationRevisionHistory.builder()
                .applicationId(miningLeaseApplication.getId())
                .applicationNumber(miningLeaseApplication.getApplicationNumber())
                .revisionStage(geologistReview)
                .revisionNumber((int) revisionCount + 1)
                .remarks(geologistRemarks)
                .requestedBy(userId)
                .requestedAt(LocalDateTime.now())
                .status("PENDING")
                .createdBy(userId)
                .build();

        revisionHistoryRepository.save(revision);
        log.info("Created revision record #{} for application {} at stage {}", revision.getRevisionNumber(), miningLeaseApplication.getApplicationNumber(), geologistReview);
    }

    @Transactional(readOnly = true)
    public SuccessResponse<List<MiningLeaseResponse>> getAssignedToGeologist(
            Long userId, Pageable pageable, String search) {

        Page<MiningLeaseApplication> page;

        if (search == null || search.isBlank()) {

            page = miningLeaseApplicationRepository
                    .findAssignedToUserGeologist(userId, pageable);

        } else {

            page = miningLeaseApplicationRepository
                    .findAssignedToUserAndSearchGeologist(
                            userId,
                            search.trim(),
                            pageable
                    );
        }

        Page<MiningLeaseResponse> responsePage =
                page.map(mapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Transactional
    public MiningLeaseResponse reviewApplicationMPCD(@Valid ReviewMiningLeaseApplicationMPCD reviewQuarryLeaseApplication, Long userId) {
        log.info("Reviewing mining lease application by MPCD user: {}", userId);

        MiningLeaseApplication miningLeaseApplication = findApplicationById(reviewQuarryLeaseApplication.getId());
        ApplicationMaster applicationMaster = miningLeaseApplication.getApplicationMaster();

//        // Complete current task
        completeCurrentTask(applicationMaster, reviewQuarryLeaseApplication.getStatus(), reviewQuarryLeaseApplication.getMpcdRemarks());

        if(reviewQuarryLeaseApplication.getStatus() != null) {
            switch (reviewQuarryLeaseApplication.getStatus()) {
                case "ACCEPTED APP" -> {
                    List<TaskManagement> taskManagement = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRole(miningLeaseApplication.getApplicationNumber(),"ASSIGNED","GEOLOGIST");
                    Long geologistId = null;
                    if (taskManagement != null) {
                        TaskManagement taskManagement1 = taskManagement.getFirst();
                        geologistId = taskManagement1.getAssignedToUserId();
                    }
                    miningLeaseApplication.setCurrentStatus("GEOLOGIST_REVIEW");
                    miningLeaseApplication.setRemarksMPCD(reviewQuarryLeaseApplication.getMpcdRemarks());
                    miningLeaseApplication.setMpcdReviewedAt(LocalDateTime.now());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("GEOLOGIST_REVIEW");
                        applicationMasterRepository.save(applicationMaster);
                    }

                    assert applicationMaster != null;
                    createTask(applicationMaster, miningLeaseApplication, "GEOLOGIST", userId, geologistId);

                    if (miningLeaseApplication.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                miningLeaseApplication.getApplicantEmail(),
                                miningLeaseApplication.getApplicantName(),
                                miningLeaseApplication.getApplicationNumber(),
                                "Geologist Review",
                                "Your application has been forwarded to the Geologist for review.");
                    }

                    String title = "Application status updated.";
                    String message = "Your application has been forwarded to geologist to review.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, miningLeaseApplication.getApplicantUserId(), serviceId);
                }
                case "ACCEPTED" -> {
                    if (Objects.equals(miningLeaseApplication.getCurrentStatus(), "ACCEPTED PFS")) {
                        miningLeaseApplication.setCurrentStatus("APPROVED");
                    }else {
                        miningLeaseApplication.setCurrentStatus("ACCEPTED PFS MPCD");
                    }
                    miningLeaseApplication.setRemarksMPCD(reviewQuarryLeaseApplication.getMpcdRemarks());
                    miningLeaseApplication.setMpcdReviewedAt(LocalDateTime.now());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus(miningLeaseApplication.getCurrentStatus());
                        applicationMasterRepository.save(applicationMaster);
                    }

                    assert applicationMaster != null;
                    createTask(applicationMaster, miningLeaseApplication, "MPCD_FOCAL", userId, userId);

                    if (miningLeaseApplication.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                miningLeaseApplication.getApplicantEmail(),
                                miningLeaseApplication.getApplicantName(),
                                miningLeaseApplication.getApplicationNumber(),
                                "MPCD MA UPLOAD",
                                "Your application has been forwarded to the MPCD for review.");
                    }

                    String title = "Application status updated.";
                    String message = "Your application has been forwarded to MPCD for review.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, miningLeaseApplication.getApplicantUserId(), serviceId);

                }
                case "Rejected" -> {
                    miningLeaseApplication.setCurrentStatus("REJECTED");
                    miningLeaseApplication.setRemarksMPCD(reviewQuarryLeaseApplication.getMpcdRemarks());
                    miningLeaseApplication.setMpcdReviewedAt(LocalDateTime.now());
                    miningLeaseApplication.setRejectedAt(LocalDateTime.now());
                    miningLeaseApplication.setRejectionReason(reviewQuarryLeaseApplication.getMpcdRemarks());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("REJECTED");
                        applicationMasterRepository.save(applicationMaster);
                    }

                    if (miningLeaseApplication.getApplicantEmail() != null) {
                        notificationClient.sendRejectionNotification(
                                miningLeaseApplication.getApplicantEmail(),
                                miningLeaseApplication.getApplicantName(),
                                miningLeaseApplication.getApplicationNumber(),
                                reviewQuarryLeaseApplication.getMpcdRemarks());
                    }
                }
                case "Resubmit PFS" -> {
                    miningLeaseApplication.setCurrentStatus("RESUBMIT PFS");
                    miningLeaseApplication.setRemarksMPCD(reviewQuarryLeaseApplication.getMpcdRemarks());
                    miningLeaseApplication.setMpcdReviewedAt(LocalDateTime.now());

                    createRevisionRecord(miningLeaseApplication, "MPCD_REVIEW", reviewQuarryLeaseApplication.getMpcdRemarks(), userId);
                    createTask(applicationMaster, miningLeaseApplication, "APPLICANT", userId, miningLeaseApplication.getApplicantUserId());

                    if (miningLeaseApplication.getApplicantEmail() != null) {
                        notificationClient.sendRevisionRequestNotification(
                                miningLeaseApplication.getApplicantEmail(),
                                miningLeaseApplication.getApplicantName(),
                                miningLeaseApplication.getApplicationNumber(),
                                "MPCD Review",
                                reviewQuarryLeaseApplication.getMpcdRemarks());
                    }
                }
                case "Resubmit Application" -> {
                    miningLeaseApplication.setCurrentStatus("RESUBMIT APPLICATION");
                    miningLeaseApplication.setRemarksMPCD(reviewQuarryLeaseApplication.getMpcdRemarks());
                    miningLeaseApplication.setMpcdReviewedAt(LocalDateTime.now());

                    createRevisionRecord(miningLeaseApplication, "MPCD_REVIEW", reviewQuarryLeaseApplication.getMpcdRemarks(), userId);
                    createTask(applicationMaster, miningLeaseApplication, "APPLICANT", userId, miningLeaseApplication.getApplicantUserId());

                    if (miningLeaseApplication.getApplicantEmail() != null) {
                        notificationClient.sendRevisionRequestNotification(
                                miningLeaseApplication.getApplicantEmail(),
                                miningLeaseApplication.getApplicantName(),
                                miningLeaseApplication.getApplicationNumber(),
                                "MPCD Review",
                                reviewQuarryLeaseApplication.getMpcdRemarks());
                    }
                }
                default -> throw new IllegalArgumentException("Application status not recognized");
            }

            miningLeaseApplicationRepository.save(miningLeaseApplication);
        }
        return mapper.toResponse(miningLeaseApplication);
    }

    public SuccessResponse<List<MiningLeaseResponse>> getAssignedToMPCD(Long userId, Pageable pageable, String search) {
        Page<MiningLeaseApplication> page;

        if (search == null || search.isBlank()) {

            page = miningLeaseApplicationRepository
                    .findAssignedToUserMPCD(userId, pageable);

        } else {

            page = miningLeaseApplicationRepository
                    .findAssignedToUserAndSearchMPCD(
                            userId,
                            search.trim(),
                            pageable
                    );
        }

        Page<MiningLeaseResponse> responsePage =
                page.map(mapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Transactional
    public MiningLeaseResponse submitMA(@Valid MiningLeaseMARequest request, Long userId) {
        MiningLeaseApplication miningleaseapplication = null;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseApplication> miningLeaseApplication = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            if (miningLeaseApplication.isPresent()) {
                miningleaseapplication = miningLeaseApplication.get();
                ApplicationMaster applicationMaster = miningleaseapplication.getApplicationMaster();
                applicationMaster.setCurrentStatus("MA SUBMITTED");
                miningleaseapplication.setMpcdFileUploadIdMa(request.getMaDocId());
                miningleaseapplication.setCurrentStatus("MA SUBMITTED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseApplicationRepository.save(miningleaseapplication);

                createTask(applicationMaster,miningleaseapplication,"APPLICANT", userId, miningleaseapplication.getApplicantUserId());

                if(miningleaseapplication.getApplicantUserId() != null) {
                    String title = "Mining lease application has been send to you with MA document.";
                    String message = "Mining lease application has been  send to you with MA document. Please log in the system to upload PA/FC document.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, miningleaseapplication.getApplicantUserId(), serviceId);
                }

            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        }
        return mapper.toResponse(miningleaseapplication);
    }

    public TaskManagementAssignedUser getAssignedGeologist(String applicationNo, boolean agencyUser) {
        TaskManagementAssignedUser assignedUser = new TaskManagementAssignedUser();
        if(agencyUser){
            List<TaskManagement> taskManagements =
                    taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRole
                            (applicationNo,"ASSIGNED", "GEOLOGIST");
            TaskManagement taskManagement = taskManagements.getFirst();
            assignedUser.setAssignedToRole(taskManagement.getAssignedToRole());
            assignedUser.setAssignedToUserId(taskManagement.getAssignedToUserId());
            assignedUser.setApplicationNumber(applicationNo);
        }else {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED_REQUEST);
        }
        return assignedUser;
    }

    @Transactional
    public MiningLeaseResponse reviewApplicationChief(@Valid ReviewMiningLeaseApplicationChief request, Long userId) {
        log.info("Reviewing quarry lease application by Mining Chief user: {}", userId);

        MiningLeaseApplication app = findApplicationById(request.getId());
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
            miningLeaseApplicationRepository.save(app);
        }
        return mapper.toResponse(app);
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

    public SuccessResponse<List<MiningLeaseResponse>> getAssignedToMiningChief(Long userId, Pageable pageable, String search) {
        Page<MiningLeaseApplication> page;

        if (search == null || search.isBlank()) {

            page = miningLeaseApplicationRepository
                    .findAssignedToUserMPCD(userId, pageable);

        } else {

            page = miningLeaseApplicationRepository
                    .findAssignedToUserAndSearchMPCD(
                            userId,
                            search.trim(),
                            pageable
                    );
        }

        Page<MiningLeaseResponse> responsePage =
                page.map(mapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Transactional
    public MiningLeaseResponse reviewApplicationME(@Valid ReviewMiningLeaseApplicationME request, Long userId) {
        log.info("Reviewing mining lease application by Mining Engineer user: {}", userId);

        MiningLeaseApplication app = findApplicationById(request.getId());
        ApplicationMaster master = app.getApplicationMaster();

        completeCurrentTask(master,  request.getStatus(), request.getRemarks());

        if (request.getStatus() != null) {
            switch (request.getStatus()) {
                case "Approved" -> {
                    app.setCurrentStatus("APPROVED");
                    app.setRemarksME(request.getRemarks());
                    app.setFmfsStatus(request.getFmfsStatus());
                    app.setApprovedArea(request.getApprovedArea());
                    app.setApprovedErb(request.getApprovedErb());
                    app.setApprovedLeasePeriod(request.getApprovedLeasePeriod());
                    app.setApprovedMineral(request.getApprovedMineral());
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
                case "Forwarded FMFS" -> {
                    app.setCurrentStatus("MINING_CHIEF_REVIEW");
                    app.setRemarksME(request.getRemarks());
                    app.setFmfsStatus(request.getFmfsStatus());
                    app.setApprovedArea(request.getApprovedArea());
                    app.setApprovedErb(request.getApprovedErb());
                    app.setApprovedLeasePeriod(request.getApprovedLeasePeriod());
                    app.setApprovedMineral(request.getApprovedMineral());
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

                    createRevisionRecord(app, "ME_REVIEW", request.getRemarks(), userId);
                    createTask(master, app, "APPLICANT", userId, app.getApplicantUserId());

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
            miningLeaseApplicationRepository.save(app);
        }
        return mapper.toResponse(app);
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

    public SuccessResponse<List<MiningLeaseResponse>> getAssignedToMineEngineer(Long userId, Pageable pageable, String search) {
        Page<MiningLeaseApplication> page;

        if (search == null || search.isBlank()) {

            page = miningLeaseApplicationRepository
                    .findAssignedToUserMineEngineer(userId, pageable);

        } else {

            page = miningLeaseApplicationRepository
                    .findAssignedToUserIdAndSearchMineEngineer(
                            userId,
                            search.trim(),
                            pageable
                    );
        }

        Page<MiningLeaseResponse> responsePage =
                page.map(mapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Transactional
    public MiningLeaseResponse submitLLC(@Valid MiningLeaseLLCRequest request) {

        MiningLeaseApplication quarryLeaseApplication1 = null;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseApplication> quarryLeaseApplication = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            if (quarryLeaseApplication.isPresent()) {
                quarryLeaseApplication1 = quarryLeaseApplication.get();
                ApplicationMaster applicationMaster = quarryLeaseApplication1.getApplicationMaster();
                applicationMaster.setCurrentStatus("LLC UPLOADED");
                quarryLeaseApplication1.setLlcDocId(request.getLLCDocId());
                quarryLeaseApplication1.setCurrentStatus("LLC UPLOADED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseApplicationRepository.save(quarryLeaseApplication1);

                if(quarryLeaseApplication1.getApplicantUserId() != null) {
                    String title = "LLC has been uploaded by mine engineer.";
                    String message = "LLC for you application has been uploaded by mine engineer.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, quarryLeaseApplication1.getApplicantUserId(), serviceId);
                }

            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        }
        return mapper.toResponse(quarryLeaseApplication1);
    }

    @Transactional
    public MiningLeaseResponse submitNoteSheetAndAdditionalDetails(@Valid MiningLeaseNoteSheetRequest request) {
        MiningLeaseApplication quarryLeaseApplication1 = null;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseApplication> quarryLeaseApplication = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            if (quarryLeaseApplication.isPresent()) {
                quarryLeaseApplication1 = quarryLeaseApplication.get();
                ApplicationMaster applicationMaster = quarryLeaseApplication1.getApplicationMaster();
                applicationMaster.setCurrentStatus("NOTE SHEET UPLOADED");
                quarryLeaseApplication1.setNotesheetDocId(request.getNoteSheetDocId());
                quarryLeaseApplication1.setCurrentStatus("NOTE SHEET UPLOADED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseApplicationRepository.save(quarryLeaseApplication1);

                if(quarryLeaseApplication1.getApplicantUserId() != null) {
                    String title = "Note sheet has been uploaded by mine engineer.";
                    String message = "Note sheet for you application has been uploaded by mine engineer.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, quarryLeaseApplication1.getApplicantUserId(), serviceId);
                }

            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        }
        return mapper.toResponse(quarryLeaseApplication1);
    }

    @Transactional
    public MiningLeaseResponse submitWorkOrder(@Valid MiningLeaseWorkOrderRequest request) {
        MiningLeaseApplication quarryLeaseApplication1 = null;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseApplication> quarryLeaseApplication = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            if (quarryLeaseApplication.isPresent()) {
                quarryLeaseApplication1 = quarryLeaseApplication.get();
                ApplicationMaster applicationMaster = quarryLeaseApplication1.getApplicationMaster();
                applicationMaster.setCurrentStatus("MINING LEASE APPROVED");
                quarryLeaseApplication1.setWorkOrderDocId(request.getWorkOrderDocId());
                quarryLeaseApplication1.setWorkOrderRemarks(request.getRemarks());
                quarryLeaseApplication1.setCurrentStatus("MINING LEASE APPROVED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseApplicationRepository.save(quarryLeaseApplication1);

                if(quarryLeaseApplication1.getApplicantUserId() != null) {
                    String title = "Work order has been uploaded by mine engineer.";
                    String message = "Work order for your application has been uploaded by mine engineer. Your application for mining lease has been approved.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, quarryLeaseApplication1.getApplicantUserId(), serviceId);
                }

            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        }
        return mapper.toResponse(quarryLeaseApplication1);
    }

    @Transactional
    public MiningLeaseResponse submitMLA(@Valid MiningLeaseMLARequest request, Long userId) {

        MiningLeaseApplication quarryLeaseApplication1 = null;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseApplication> quarryLeaseApplication = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            if (quarryLeaseApplication.isPresent()) {
                quarryLeaseApplication1 = quarryLeaseApplication.get();
                ApplicationMaster applicationMaster = quarryLeaseApplication1.getApplicationMaster();
                quarryLeaseApplication1.setMlaDocId(request.getMlaDocId());
                quarryLeaseApplication1.setMlaStatus("SUBMITTED");
                quarryLeaseApplication1.setCurrentStatus("MLA SUBMITTED");
                applicationMaster.setCurrentStatus("MLA SUBMITTED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseApplicationRepository.save(quarryLeaseApplication1);


                List<String> status = new ArrayList<>();
                status.add("SUBMITTED");
                status.add("PAYMENT PENDING");
                List<TaskManagement> taskManagement = taskManagementRepository.findByApplicationNumberAndTaskStatusInAndAssignedToRole(quarryLeaseApplication1.getApplicationNumber(),status,"DIRECTOR");
                Long directorId = null;
                if (taskManagement != null) {
                    TaskManagement taskManagement1 = taskManagement.getFirst();
                    directorId = taskManagement1.getAssignedToUserId();
                }

                createTask(applicationMaster,quarryLeaseApplication1,"DIRECTOR", userId, directorId);

                UserWorkloadProjection assignedDirectorDetails = miningLeaseApplicationRepository.findUserDetails(directorId);
                if(assignedDirectorDetails.getUserId() != null) {
                    String title = "Mining lease application has been assigned for MLA review.";
                    String message = "Mining lease application has been  assigned for MLA review.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, assignedDirectorDetails.getUserId(), serviceId);
                }

            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        }
        return mapper.toResponse(quarryLeaseApplication1);
    }

    @Transactional
    public MiningLeaseResponse resubmitApplication(FileUploadRequest request, Long userId) {
        MiningLeaseApplication quarryLeaseApplication = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNumber())
                .orElseThrow();

        Long geologistId = null;

        Long mpcdId = null;

        ApplicationMaster applicationMaster = quarryLeaseApplication.getApplicationMaster();

        List<TaskManagement> taskManagement = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRole(request.getApplicationNumber(), "RESUBMIT PFS", "APPLICANT");
        if(!taskManagement.isEmpty()) {
            TaskManagement taskManagement1 = taskManagement.getFirst();
            geologistId = taskManagement1.getAssignedToUserId();
        }

        List<TaskManagement> taskManagement2 = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRole(request.getApplicationNumber(), "RESUBMIT PFS", "APPLICANT");
        if(!taskManagement2.isEmpty()) {
            TaskManagement taskManagement3 = taskManagement.getFirst();
            mpcdId = taskManagement3.getAssignedToUserId();
        }

        if(request.getFileType().equals("PFS")) {
            quarryLeaseApplication.setPfsDocId(request.getFileId());
            applicationMaster.setCurrentStatus("RESUBMITTED PFS");
            quarryLeaseApplication.setCurrentStatus("RESUBMITTED PFS");

            createTask(applicationMaster,quarryLeaseApplication,"GEOLOGIST",userId,geologistId);
            createTask(applicationMaster,quarryLeaseApplication,"MPCD FOCAL",userId,mpcdId);
        } else if (request.getFileType().equals("GR")) {
            quarryLeaseApplication.setFileUploadIdGr(Long.valueOf(request.getFileId()));
            applicationMaster.setCurrentStatus("RESUBMITTED GR");
            quarryLeaseApplication.setCurrentStatus("RESUBMITTED GR");

            createTask(applicationMaster,quarryLeaseApplication,"GEOLOGIST",userId,geologistId);
            createTask(applicationMaster,quarryLeaseApplication,"MPCD FOCAL",userId,mpcdId);

        }else {
            quarryLeaseApplication.setFmfsDocId(request.getFileId());
            applicationMaster.setCurrentStatus("RESUBMITTED FMFS");
            quarryLeaseApplication.setCurrentStatus("RESUBMITTED FMFS");

            createTask(applicationMaster,quarryLeaseApplication,"GEOLOGIST",userId,geologistId);
            createTask(applicationMaster,quarryLeaseApplication,"MPCD FOCAL",userId,mpcdId);
        }

        miningLeaseApplicationRepository.save(quarryLeaseApplication);
        return mapper.toResponse(quarryLeaseApplication);
    }

    public SuccessResponse<List<MiningLeaseResponse>> getAllApplicationAdmin(Pageable pageable, String search) {
        Page<MiningLeaseApplication> page;

        if (search == null || search.isBlank()) {
            page = miningLeaseApplicationRepository.findAll(pageable);
        } else {
            page = miningLeaseApplicationRepository.findAllBySearch(search.trim(), pageable);
        }

        return SuccessResponse.fromPage("Applications fetched successfully", page.map(mapper::toResponse));
    }
}
