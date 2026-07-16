package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.utility.CustomRuntimeException;

import com.mas.gov.bt.mas.primary.client.MastersPaymentClient;
import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.payment.PaymentInitiationRequest;
import com.mas.gov.bt.mas.primary.dto.payment.PaymentInitiationResponse;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.ApplicationListResponse;
import com.mas.gov.bt.mas.primary.dto.response.CapCheckResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    private final MastersPaymentClient mastersPaymentClient;

    private final SiteProvisioningService siteProvisioningService;

    private final FMFSDetailsRepository fmfsDetailsRepository;

    private final WorkflowTrackingService workflowTrackingService;

    private final UserRepository userRepository;

    private final HouseholdPermitCapConfigRefRepository capConfigRepository;

    private final com.mas.gov.bt.mas.primary.client.WorkflowAssignmentClient workflowAssignmentClient;

    private final HouseholdPermitThresholdEntryRepository thresholdEntryRepository;

    private static final int DEFAULT_MAX_APPLICATIONS = 2;
    private static final String THRESHOLD_SERVICE_TYPE = "MINING_LEASE";

    private final HouseholdPermitThresholdRepository householdPermitThresholdRepository;

    @Value("${app.self.base-url}")
    private String selfBaseUrl;

    @Value("${payment.enabled:true}")
    private boolean paymentEnabled;

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

        // ============================================================
        // 0. Checking if the user has not more than two mining lease
        // ============================================================
//            String houseHoldNumber = miningLeaseApplicationRepository.findUserHouseHoldNumber(userId);
//            Integer miningLeaseCount = miningLeaseApplicationRepository.findLeaseCountForMining(houseHoldNumber);

//            if(miningLeaseCount <=1){
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

                    application.setRegionId(dzongkhag.getRegion().getId());
                    application.setDzongkhag(dzongkhag);
                }

                if (request.getGewog() != null && !request.getGewog().isEmpty()) {
                    GewogLookup gewog = (GewogLookup) gewogLookupRepository
                            .findByGewogId(request.getGewog())
                            .orElseThrow(() -> new RuntimeException("Invalid gewog ID"));

                    application.setGewog(gewog);
                }

                if (request.getNearestVillage() != null && !request.getNearestVillage().isEmpty()) {
                    VillageLookup villageLookup = villageLookupRepository
                            .findByVillageSerialNo(Integer.parseInt(request.getNearestVillage()))
                            .orElseThrow(() -> new RuntimeException("Invalid village ID"));

                    application.setNearestVillage(villageLookup);
                }

                application.setUpdatedBy(userId);

                PaymentMaster paymentMaster =
                        paymentMasterRepository.resolveApplicable(SERVICE_CODE, "APPLICATION_FEE", "SUBMITTED").orElse(null);

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
                                Boolean.TRUE.equals(paymentMaster.getIsEnabled());

                if (feeRequired) {
                    application.setCurrentStatus("PAYMENT PENDING");
                    application.setApplicationFeesAmount(paymentMaster.getAmount());
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

                // =====================================================
                // 9. FINAL SAVE
                // =====================================================
                miningLeaseApplicationRepository.save(application);

                if (feeRequired) {
                    if (!paymentEnabled) {
                        onPaymentConfirmed(application.getApplicationNumber());
                        log.info("Payment bypassed (payment.enabled=false) for application {}", application.getApplicationNumber());
                        return mapper.toResponse(application);
                    }

                    // Initiate payment — director task is created in onPaymentConfirmed()
                    PaymentInitiationResponse paymentResp = mastersPaymentClient.initiate(
                            buildPaymentRequest(application, paymentMaster.getServiceCode()));

                    if (application.getApplicantEmail() != null) {
                        notificationClient.sendApplicationFeeRequiredNotification(
                                application.getApplicantEmail(),
                                application.getApplicantName(),
                                application.getApplicationNumber());
                    }
                    if (application.getApplicantUserId() != null) {
                        notificationClient.sendUserNotification(
                                "Application pending for payment",
                                "Your application will be processed after payment.",
                                application.getApplicantUserId(),
                                "78", "CITIZEN");
                    }

                    MiningLeaseResponse response = mapper.toResponse(application);
                    response.setRedirectUrl(paymentResp.getRedirectUrl());
                    log.info("Payment initiated for application {}", application.getApplicationNumber());
                    return response;
                }

                // =====================================================
                // No fee: assign director task immediately
                // =====================================================
                List<TaskManagement> getAssignedDirector =
                        taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(
                                application.getApplicationNumber(), "GR SUBMITTED", "DIRECTOR", SERVICE_CODE);
                TaskManagement grTask = getAssignedDirector.getFirst();
                Long directorId = grTask.getAssignedToUserId();
                createTask(master, application, "DIRECTOR", userId, directorId);

                if (application.getApplicantEmail() != null) {
                    notificationClient.sendApplicationSubmittedNotification(
                            application.getApplicantEmail(),
                            application.getApplicantName(),
                            application.getApplicationNumber());
                }
                if (application.getApplicantUserId() != null) {
                    notificationClient.sendUserNotification(
                            "Application submitted successfully",
                            "Your mining lease application has been submitted.",
                            application.getApplicantUserId(),
                            "78", "CITIZEN");
                }

                log.info("Application submitted successfully: {}", application.getApplicationNumber());
                return mapper.toResponse(application);

    }

    /**
     * Called by the payment callback once application fee payment is confirmed as PAID.
     * Transitions status PAYMENT PENDING → SUBMITTED, creates director task, and notifies.
     */
    @Transactional
    public void onPaymentConfirmed(String applicationNo) {
        MiningLeaseApplication application = miningLeaseApplicationRepository.findByApplicationNumber(applicationNo)
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "Application not found: " + applicationNo));

        application.setCurrentStatus("SUBMITTED");
        ApplicationMaster master = application.getApplicationMaster();
        master.setCurrentStatus("SUBMITTED");
        applicationMasterRepository.save(master);
        miningLeaseApplicationRepository.save(application);

        List<TaskManagement> grTasks = taskManagementRepository
                .findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(
                        applicationNo, "GR SUBMITTED", "DIRECTOR", SERVICE_CODE);
        if (!grTasks.isEmpty()) {
            Long directorId = grTasks.getFirst().getAssignedToUserId();
            createTask(master, application, "DIRECTOR", application.getApplicantUserId(), directorId);
        }

        if (application.getApplicantEmail() != null) {
            notificationClient.sendApplicationSubmittedNotification(
                    application.getApplicantEmail(),
                    application.getApplicantName(),
                    applicationNo);
        }
        if (application.getApplicantUserId() != null) {
            notificationClient.sendUserNotification(
                    "Application submitted successfully",
                    "Your payment has been confirmed and your mining lease application has been submitted.",
                    application.getApplicantUserId(),
                    "78", "CITIZEN");
        }
        log.info("Payment confirmed — application {} transitioned to SUBMITTED", applicationNo);
    }

    private PaymentInitiationRequest buildPaymentRequest(MiningLeaseApplication application, String serviceCode) {
        PaymentInitiationRequest.PaymentItemRequest item = new PaymentInitiationRequest.PaymentItemRequest();
        item.setFeeType("APPLICATION_FEE");
        item.setServiceCode(serviceCode);
        item.setDescription("Mining Lease Application Fee");
        item.setQuantity(1);

        String documentNo = resolveDocumentNo(application);

        PaymentInitiationRequest req = new PaymentInitiationRequest();
        req.setApplicationId(application.getApplicationNumber());
        req.setApplicationType("MINING_LEASE");
        req.setTaxPayerName(application.getApplicantName());
        req.setTaxPayerDocumentNo(documentNo);
        req.setTaxPayerNo(documentNo);
        req.setPlatform("MAS");
        req.setOnPaidStatus("SUBMITTED");
        req.setCallbackUrl(selfBaseUrl + "/api/mining-lease/payment-callback");
        req.setPaymentItems(List.of(item));
        return req;
    }

    private String resolveDocumentNo(MiningLeaseApplication application) {
        String type = application.getApplicantType();
        if (type == null) return application.getApplicantCid();
        return switch (type.toUpperCase()) {
            case "REGISTERED_COMPANY" -> application.getCompanyRegistrationNo();
            case "BUSINESS_LICENSE"   -> application.getLicenseNo();
            default                   -> application.getApplicantCid();
        };
    }

    @Transactional
    private void createTask(ApplicationMaster master, MiningLeaseApplication application, String role, Long userId, Long directorId) {
        if (directorId == null) {
            // No eligible candidate — the resolver already recorded this application in the
            // escalation queue (see WorkflowAssignmentClient), nothing to create locally.
            log.info("Skipping task creation for role {} on application {} — escalated, no assignee",
                    role, application.getApplicationNumber());
            return;
        }

        log.debug("Creating task for director ID: {}", directorId);
        try {
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

            TaskManagement savedTask = taskManagementRepository.save(task);

            log.info("Task created successfully for role {}: Task ID={}", role, savedTask.getId());

        } catch (Exception ex) {
            log.error("Error creating task for director", ex);
            throw new BusinessException(
                    ErrorCodes.DATABASE_CONNECTION_FAILED,
                    "Failed to create task assignment",
                    ex
            );
        }
    }

    @Transactional
    private ApplicationMaster createApplicationMaster(String applicationNumber, Long userId) {
        log.debug("Creating ApplicationMaster for application number: {}", applicationNumber);
        try {
            ApplicationMaster master = new ApplicationMaster();
            master.setApplicationNumber(applicationNumber);
            master.setServiceCode(SERVICE_CODE);
            master.setApplicantUserId(userId);
            master.setCurrentStatus("GR SUBMITTED");
            master.setSubmittedOn(LocalDateTime.now());

            ApplicationMaster savedMaster = applicationMasterRepository.save(master);
            log.debug("Application Master created with ID: {}", savedMaster.getId());
            return savedMaster;
        } catch (Exception ex) {
            log.error("Error creating application master", ex);
            throw new BusinessException(
                    ErrorCodes.DATABASE_CONNECTION_FAILED,
                    "Failed to create application master",
                    ex);
        }
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

    /**
     * Resolves the application-cap grouping key for a user based on their
     * t_citizens.registration_type: household_number for INDIVIDUAL, license_no
     * for BUSINESS_LICENSE, company_registration_number for REGISTERED_COMPANY.
     * Falls back to the applicant's own CID (for counting) when the natural
     * key is blank, so applicants with no household/license/company number on
     * file still get capped individually instead of the check silently doing
     * nothing. The applicant's real registration type is always returned as
     * element [2] so the admin-configured cap for that type still applies.
     * Returns {groupingType, groupingKey, registrationType}.
     */
    private String[] resolveGroupingKey(Long userId) {
        return miningLeaseApplicationRepository.findGroupingInfoByUserId(userId)
                .map(info -> {
                    String rt = info.getRegistrationType();
                    if ("INDIVIDUAL".equals(rt) && isNotBlank(info.getHouseholdNumber())) {
                        return new String[]{"INDIVIDUAL", info.getHouseholdNumber(), rt};
                    }
                    if ("BUSINESS_LICENSE".equals(rt) && isNotBlank(info.getLicenseNo())) {
                        return new String[]{"BUSINESS_LICENSE", info.getLicenseNo(), rt};
                    }
                    if ("REGISTERED_COMPANY".equals(rt) && isNotBlank(info.getCompanyRegistrationNumber())) {
                        return new String[]{"REGISTERED_COMPANY", info.getCompanyRegistrationNumber(), rt};
                    }
                    return new String[]{"CID", info.getCid(), rt};
                })
                .orElse(new String[]{"CID", null, "INDIVIDUAL"});
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private int getMaxAllowedForService(String serviceType, String registrationType) {
        String rt = isNotBlank(registrationType) ? registrationType : "INDIVIDUAL";
        return capConfigRepository.findByServiceTypeAndRegistrationType(serviceType, rt)
                .map(c -> c.getMaxAllowed() != null ? c.getMaxAllowed() : DEFAULT_MAX_APPLICATIONS)
                .orElse(DEFAULT_MAX_APPLICATIONS);
    }

    /**
     * Pre-flight cap check — called when the applicant clicks to open the
     * application form, before they fill anything in. Also reused by
     * submitGR as the authoritative server-side guard.
     */
    public CapCheckResponse checkCap(Long userId) {
        String[] grouping = resolveGroupingKey(userId);
        int maxAllowed = getMaxAllowedForService("MINING_LEASE", grouping[2]);
        Integer total = miningLeaseApplicationRepository.countMiningLeasesForGrouping(grouping[0], grouping[1]);
        int current = total != null ? total : 0;
        boolean allowed = current < maxAllowed;
        String message = allowed ? null
                : "Only " + maxAllowed + " mining lease application(s) are permitted per " +
                        ("CID".equals(grouping[0]) ? "applicant" : "household/entity") + ".";
        return new CapCheckResponse(allowed, current, maxAllowed, message);
    }

    /** Records an ACTIVE entry in the shared threshold table when a lease is finally approved. */
    private void recordApprovedForThreshold(MiningLeaseApplication app) {
        if (app.getApplicantCid() == null || app.getApplicantCid().isBlank()) return;
        if (thresholdEntryRepository.existsByServiceTypeAndApplicationNo(THRESHOLD_SERVICE_TYPE, app.getApplicationNumber())) {
            return;
        }
        HouseholdPermitThresholdEntry entry = new HouseholdPermitThresholdEntry();
        entry.setApplicantCid(app.getApplicantCid());
        entry.setServiceType(THRESHOLD_SERVICE_TYPE);
        entry.setApplicationNo(app.getApplicationNumber());
        entry.setStatus("ACTIVE");
        thresholdEntryRepository.save(entry);
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
    public MiningLeaseResponse submitGR(@Valid MiningLeaseGRRequest request, Long userId) {
        // 1.
        // ======== SAVE MINING LEASE GR SUBMITTED BY APPLICANT ====== //
        MiningLeaseApplication miningLeaseApplication = new MiningLeaseApplication();

        // Validate input
        validateApplicationRequest(request);

//         ============================================================
//         0. Checking the applicant hasn't exceeded the configured cap
//         ============================================================
        CapCheckResponse cap = checkCap(userId);
        if (!cap.isAllowed()) {
            throw new BusinessException(ErrorCodes.DATA_INTEGRITY_VIOLATION, cap.getMessage());
        }

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
        miningLeaseApplication.setBusinessLicenseNo(request.getBusinessLicenseNo());
        miningLeaseApplication.setBusinessName(request.getBusinessName());
        miningLeaseApplication.setCompanyRegistrationNo(request.getCompanyRegistrationNo());
        miningLeaseApplication.setCompanyName(request.getCompanyName());
        miningLeaseApplication.setCurrentStatus("GR SUBMITTED");
        miningLeaseApplication.setCreatedBy(userId);

        try {
            miningLeaseApplicationRepository.save(miningLeaseApplication);
        }catch (Exception ex) {
            log.error("Error saving mining lease application", ex);
            throw new BusinessException(
                    ErrorCodes.DATABASE_CONNECTION_FAILED,
                    "Failed to save mining lease application",
                    ex);
        }

        // =====================================================
        // 2. ASSIGN DIRECTOR
        // =====================================================
        UserWorkloadProjection assignedDirector = assignDirector(
                miningLeaseApplication.getRegionId(), miningLeaseApplication.getApplicationNumber());

        // =====================================================
        // 3. Application master and create task for director
        // =====================================================
        ApplicationMaster master = createApplicationMaster(miningLeaseApplication.getApplicationNumber(), userId);

        miningLeaseApplication.setApplicationMaster(master);

        createTask(master,miningLeaseApplication,"DIRECTOR",userId, assignedDirector.getUserId());

        // No eligible director — masters already recorded this in the escalation queue, nothing to notify.
        if (assignedDirector.getUserId() == null) {
            return mapper.toResponse(miningLeaseApplication);
        }

        log.debug("Sending notifications to director: {}", assignedDirector.getUserId());

        try {
            if (assignedDirector.getEmail() != null && !assignedDirector.getEmail().trim().isEmpty()) {
                try {
                    notificationClient.sendMiningLeaseMailToDirectorAssigned(
                            assignedDirector.getEmail(),
                            assignedDirector.getUsername(),
                            miningLeaseApplication.getApplicationNumber());
                    log.info("Email notification sent to director: {}", assignedDirector.getEmail());
                } catch (Exception ex) {
                    log.warn("Failed to send email notification to director", ex);
                    throw new BusinessException(
                            ErrorCodes.RECORD_NOT_FOUND,
                            "Director Email is missing",
                            ex
                    );
                }
            }

        if(assignedDirector.getUserId()!= null) {
            try {
                String title = "Mining lease application has been assigned.";
                String message = "An application for mining lease has been assigned for review. Application No. " + miningLeaseApplication.getApplicationNumber() + " Please login in review the Geological report.";
                String serviceId = "78";
                notificationClient.sendUserNotification(title, message, assignedDirector.getUserId(), serviceId, "STAFF");
            }catch (Exception ex) {
                log.warn("Failed to send in-app notification to director", ex);
                throw new BusinessException(
                        ErrorCodes.RECORD_NOT_FOUND,
                        "Director User ID  is missing",
                        ex
                );
            }
        }
        } catch (Exception ex) {
            log.error("Unexpected error sending notifications", ex);
            throw new BusinessException(
                    ErrorCodes.EXTERNAL_SERVICE_ERROR,
                    "Failed to send notifications to director",
                    ex);
        }

        return mapper.toResponse(miningLeaseApplication);
    }

    /**
     * Resolves the Director to assign a newly-submitted application to, via the
     * admin-configured mas_db.t_workflow_step rule for (MINING_LEASE, GR SUBMITTED).
     * When no eligible candidate is found, the returned projection has a null
     * userId — masters has already recorded it in the escalation queue; callers
     * must check for that (see createTask()'s null-guard and the notification
     * blocks below) instead of assuming a real director was always found.
     */
    @Transactional
    public UserWorkloadProjection assignDirector(Long regionId, String applicationNumber) {
        var resolved = workflowAssignmentClient.resolve(SERVICE_CODE, "GR SUBMITTED", regionId, applicationNumber);
        return toProjection(resolved);
    }

    private UserWorkloadProjection toProjection(com.mas.gov.bt.mas.primary.dto.workflow.ResolvedAssigneeDto dto) {
        return new UserWorkloadProjection() {
            @Override public Long getUserId() { return dto.getUserId(); }
            @Override public String getEmail() { return dto.getEmail(); }
            @Override public Long getWorkload() { return dto.getWorkload(); }
            @Override public String getUsername() { return dto.getUsername(); }
        };
    }

    /**
     * Validate application request data
     */
    private void validateApplicationRequest(MiningLeaseGRRequest request) {
        if (request == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_INPUT_DATA,
                    "Application request cannot be null");
        }

        if (request.getExpPermitNo() == null || request.getExpPermitNo().trim().isEmpty()) {
            throw new BusinessException(
                    ErrorCodes.MISSING_REQUIRED_FIELD,
                    "Exploration permit number is required");
        }

        if (request.getGRDocId() == null) {
            throw new BusinessException(
                    ErrorCodes.MISSING_REQUIRED_FIELD,
                    "GR document ID is required");
        }

        if (request.getApplicantCid() == null || request.getApplicantCid().trim().isEmpty()) {
            throw new BusinessException(
                    ErrorCodes.MISSING_REQUIRED_FIELD,
                    "Applicant CID is required");
        }

        if (request.getApplicantEmail() == null || request.getApplicantEmail().trim().isEmpty()) {
            throw new BusinessException(
                    ErrorCodes.MISSING_REQUIRED_FIELD,
                    "Applicant email is required");
        }

        log.debug("Application request validation passed");
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
    public Page<ApplicationListResponse> getMyApplications(Long userId, Pageable pageable, String search) {
        List<String> ApplicationStatus = List.of(
                "MINING LEASE APPROVED",
                "REJECTED",
                "TERMINATED",
                "RENEWAL APPLICATION",
                "TEMPORARY CLOSURE APPROVED",
                "UNDER-REVIEW-TERMINATION"
        );
        Page<MiningLeaseApplication> applications;

        if (search == null || search.isBlank()) {
            applications = miningLeaseApplicationRepository.findByApplicantUserIdAndStatusInApplication(
                    userId,
                    ApplicationStatus,
                    pageable);
        }
        else {

            applications = miningLeaseApplicationRepository.findByAssignedToUserAndSearch(
                    userId,
                    ApplicationStatus,
                    search.trim(),
                    pageable
            );
        }
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
    public Page<ApplicationListResponse> getArchivedApplications(Pageable pageable, String search, Long userId) {

        List<String> archivedStatuses = List.of(
                "MINING LEASE APPROVED",
                "REJECTED",
                "TERMINATED",
                "RENEWAL APPLICATION",
                "TEMPORARY CLOSURE APPROVED",
                "UNDER-REVIEW-TERMINATION"
        );

        Page<MiningLeaseApplication> applications;

        if (search == null || search.isBlank()) {
            applications = miningLeaseApplicationRepository.findArchivedAssignedToUserMPCD(
                    userId,
                    archivedStatuses,
                    pageable);
        }
        else {

            applications = miningLeaseApplicationRepository.findArchivedAssignedToUserAndSearch(
                    userId,
                    archivedStatuses,
                    search.trim(),
                    pageable
            );
        }
        return applications.map(mapper::toListResponse);
    }

    /**
     * Get archived applications (APPROVED or REJECTED) for a specific user.
     */
    @Transactional(readOnly = true)
    public Page<ApplicationListResponse> getMyArchivedApplications(Long userId, Pageable pageable, String search) {
        List<String> archivedStatuses = List.of(
                "MINING LEASE APPROVED",
                "REJECTED",
                "TERMINATED",
                "RENEWAL APPLICATION",
                "TEMPORARY CLOSURE APPROVED",
                "UNDER-REVIEW-TERMINATION"
        );
        Page<MiningLeaseApplication> applications ;

        if (search == null || search.isBlank()) {
            applications =  miningLeaseApplicationRepository.findByApplicantUserIdAndCurrentStatusIn(userId, archivedStatuses, pageable);
        } else {
            applications = miningLeaseApplicationRepository.findByApplicantUserIdAndSearch(userId, archivedStatuses, search.trim(), pageable);
        }

        return applications.map(mapper::toListResponse);
    }

    public SuccessResponse<List<MiningLeaseResponse>> getArchivedApplicationToMPCD(Long userId, Pageable pageable, String search) {

        Page<MiningLeaseApplication> page;

        List<String> archivedStatuses = List.of(
                "MINING LEASE APPROVED",
                "REJECTED",
                "TERMINATED",
                "RENEWAL APPLICATION",
                "TEMPORARY CLOSURE APPROVED",
                "UNDER-REVIEW-TERMINATION"
        );

        if (search == null || search.isBlank()) {

            page = miningLeaseApplicationRepository
                    .findArchivedAssignedToUserMPCD(userId, archivedStatuses, pageable);

        } else {

            page = miningLeaseApplicationRepository
                    .findArchivedAssignedToUserAndSearchMPCD(
                            userId,
                            archivedStatuses,
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

        UserWorkloadProjection userDetails = miningLeaseApplicationRepository.findUserDetails(request.getNewAssigneeUserId());
        notificationClient.sendTaskReassignmentNotification(
                userDetails.getEmail(), userDetails.getUsername(),
                firstTask.getApplicationNumber(),
                firstTask.getAssignedToRole(),
                request.getRemarks());

        if(userDetails.getUserId()!= null) {
            String title = "An new application has been reassigned.";
            String message = "An application for quarry lease has been assigned for review. Application No. "+request.getApplicationNumber()+" Please login in review the application";
            String serviceId = "78";
            notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId, "STAFF");
        }else {
            throw new CustomRuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }


        log.info("Geologist task {} reassigned to user {}", firstTask.getId(), request.getNewAssigneeUserId());
    }

    @Transactional
    public void reassignTaskMPCD(@Valid ReassignTaskRequest request, Long userId) {
        List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(request.getApplicationNumber(),"MPCD ASSIGNED","MPCD_FOCAL", SERVICE_CODE);
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

        UserWorkloadProjection userDetails = miningLeaseApplicationRepository.findUserDetails(request.getNewAssigneeUserId());
        notificationClient.sendTaskReassignmentNotification(
                userDetails.getEmail(), userDetails.getUsername(),
                firstTask.getApplicationNumber(),
                firstTask.getAssignedToRole(),
                request.getRemarks());

        if(userDetails.getUserId()!= null) {
            String title = "An new application has been reassigned.";
            String message = "An application for quarry lease has been assigned for review. Application No. "+ request.getApplicationNumber()+" Please login in review the application";
            String serviceId = "78";
            notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId, "STAFF");
        }else {
            throw new CustomRuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }

        log.info("MPCD task {} reassigned to user {}", firstTask.getId(), request.getNewAssigneeUserId());
    }

    @Transactional
    public void reassignTaskME(@Valid ReassignTaskRequest request, Long userId) {
        log.info("Reassigning task by mining engineer: {} by user: {}", request.getApplicationNumber(), userId);
        List<TaskManagement> tasks = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(request.getApplicationNumber(),"FMFS SUBMITTED","MINE ENGINEER", SERVICE_CODE);

        if (tasks.isEmpty()) {
            throw new CustomRuntimeException("No tasks found");
        }

        for (TaskManagement taskManagement : tasks) {
            taskManagement.setAssignedToUserId(request.getNewAssigneeUserId());
            taskManagement.setAssignedByUserId(userId);
            taskManagement.setAssignedAt(LocalDateTime.now());
            taskManagement.setReassignmentCount
                    (taskManagement.getReassignmentCount() + 1);
            taskManagement.setActionRemarks(request.getRemarks());

        }

        taskManagementRepository.saveAll(tasks);

        TaskManagement firstTask = tasks.getFirst();

        UserWorkloadProjection userDetails = miningLeaseApplicationRepository.findUserDetails(request.getNewAssigneeUserId());
        notificationClient.sendTaskReassignmentNotification(
                userDetails.getEmail(), userDetails.getUsername(),
                firstTask.getApplicationNumber(),
                firstTask.getAssignedToRole(),
                request.getRemarks());

        if(userDetails.getUserId()!= null) {
            String title = "An new application has been reassigned.";
            String message = "An application for quarry lease has been assigned for review. Application No. "+request.getApplicationNumber()+" Please login in review the application";
            String serviceId = "78";
            notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId, "STAFF");
        }else {
            throw new CustomRuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }

        log.info("Mining engineer task {} reassigned to user {}", request.getApplicationNumber(), request.getNewAssigneeUserId());
    }

    @Transactional
    public void reassignTaskMineChief(@Valid ReassignTaskRequest request, Long userId) {
        log.info("Mine chief reassigning task: {} by user: {}", request.getApplicationNumber(), userId);

        List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(request.getApplicationNumber(),"MINING_CHIEF_REVIEW","MINING_CHIEF_REVIEW", SERVICE_CODE);


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

        UserWorkloadProjection userDetails = miningLeaseApplicationRepository.findUserDetails(request.getNewAssigneeUserId());
        notificationClient.sendTaskReassignmentNotification(
                userDetails.getEmail(), userDetails.getUsername(),
                firstTask.getApplicationNumber(),
                firstTask.getAssignedToRole(),
                request.getRemarks());

        if(userDetails.getUserId()!= null) {
            String title = "An new application has been reassigned.";
            String message = "An application for quarry lease has been assigned for review. Application No. "+request.getApplicationNumber()+" Please login in review the application";
            String serviceId = "78";
            notificationClient.sendUserNotification(title, message, userDetails.getUserId(), serviceId, "STAFF");
        }else {
            throw new CustomRuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }

        log.info("Task {} reassigned to user {}", firstTask.getId(), request.getNewAssigneeUserId());
    }

    @Transactional
    public MiningLeaseResponse submitPAFC(MiningLeasePAFCRequest request, Long userId) {
        MiningLeaseApplication miningLeaseApplication = null;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseApplication> miningLeaseApplication1 = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            if (miningLeaseApplication1.isPresent()) {
                List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(request.getApplicationNo(),"MPCD ASSIGNED","MPCD_FOCAL", SERVICE_CODE);

                TaskManagement taskManagement = new TaskManagement();

                if (task != null) {
                    taskManagement = task.getFirst();
                }
                Long assignedMPCDFocalId = taskManagement.getAssignedToUserId();

                miningLeaseApplication = miningLeaseApplication1.get();
                ApplicationMaster applicationMaster = miningLeaseApplication.getApplicationMaster();
                miningLeaseApplication.setFileUploadIdPA(request.getPaDocId());
                miningLeaseApplication.setFileUploadIdFC(request.getFcDocId());
                miningLeaseApplication.setFileUploadIdPublicClearance(request.getPublicClearanceDocId());
                miningLeaseApplication.setCurrentStatus("PA/FC SUBMITTED");
                applicationMaster.setCurrentStatus("PA/FC SUBMITTED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseApplicationRepository.save(miningLeaseApplication);

                createTask(applicationMaster,miningLeaseApplication,"MPCD_FOCAL", userId, assignedMPCDFocalId);

                if(assignedMPCDFocalId != null) {
                    String title = "PA/FC submitted. ";
                    String message = "PA/FC has been submitted by applicant. Please login an process the application."+ "Application No. : "+ miningLeaseApplication.getApplicationNumber();
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, assignedMPCDFocalId, serviceId, "STAFF");
                }

                if(miningLeaseApplication.getApplicantEmail() != null) {
                    notificationClient.sendStatusUpdateNotification(
                            miningLeaseApplication.getApplicantEmail(),
                            miningLeaseApplication.getApplicantCid(),
                            miningLeaseApplication.getApplicationNumber(),
                            "PA/FC SUBMITTED",
                            "PA/FC has been submitted successfully.");
                }

            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        }
        return mapper.toResponse(miningLeaseApplication);
    }

    @Transactional
    public MiningLeaseResponse resubmitPAFC(MiningLeasePAFCRequest request, Long userId) {
        MiningLeaseApplication miningLeaseApplication = null;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseApplication> miningLeaseApplication1 = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            if (miningLeaseApplication1.isPresent()) {
                List<TaskManagement> task = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(request.getApplicationNo(),"MPCD ASSIGNED","MPCD_FOCAL", SERVICE_CODE);

                TaskManagement taskManagement = new TaskManagement();

                if (task != null) {
                    taskManagement = task.getFirst();
                }
                Long assignedMPCDFocalId = taskManagement.getAssignedToUserId();

                miningLeaseApplication = miningLeaseApplication1.get();
                ApplicationMaster applicationMaster = miningLeaseApplication.getApplicationMaster();
                miningLeaseApplication.setFileUploadIdPA(request.getPaDocId());
                miningLeaseApplication.setFileUploadIdFC(request.getFcDocId());
                miningLeaseApplication.setFileUploadIdPublicClearance(request.getPublicClearanceDocId());
                miningLeaseApplication.setCurrentStatus("PA/FC RESUBMITTED");
                applicationMaster.setCurrentStatus("PA/FC RESUBMITTED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseApplicationRepository.save(miningLeaseApplication);

                createTask(applicationMaster,miningLeaseApplication,"MPCD_FOCAL", userId, assignedMPCDFocalId);

                if(assignedMPCDFocalId != null) {
                    String title = "PA/FC resubmitted. ";
                    String message = "PA/FC has been resubmitted by applicant. Please login to process the application."+ "Application No. : "+ miningLeaseApplication.getApplicationNumber();
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, assignedMPCDFocalId, serviceId, "STAFF");
                }

                if(miningLeaseApplication.getApplicantEmail() != null) {
                    notificationClient.sendStatusUpdateNotification(
                            miningLeaseApplication.getApplicantEmail(),
                            miningLeaseApplication.getApplicantCid(),
                            miningLeaseApplication.getApplicationNumber(),
                            "PA/FC RESUBMITTED",
                            "PA/FC has been resubmitted successfully.");
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

                // New Requirement from client side
                if(request.getEcFileId() != null && request.getEcNumber() != null && request.getEcExpiryDate() != null) {
                    miningLeaseApplication.setEcFileId(request.getEcFileId());
                    miningLeaseApplication.setEcNumber(request.getEcNumber());
                    miningLeaseApplication.setEcExpiryDate(request.getEcExpiryDate());
                    miningLeaseApplication.setECStatus("ACTIVE");
                }else{
                    log.info("EC file is not present");
                }

                miningLeaseApplication.setCurrentStatus("FMFS SUBMITTED");

                applicationMaster.setCurrentStatus("FMFS SUBMITTED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseApplicationRepository.save(miningLeaseApplication);

                // Region -> HQ region -> escalate is now handled natively by the resolver.
                var resolvedMineEngineer = workflowAssignmentClient.resolve(
                        SERVICE_CODE, "FMFS SUBMITTED", miningLeaseApplication.getRegionId(),
                        miningLeaseApplication.getApplicationNumber());
                UserWorkloadProjection assignedMineEngineer = toProjection(resolvedMineEngineer);

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
                    notificationClient.sendUserNotification(title, message, assignedMineEngineer.getUserId(), serviceId, "STAFF");
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

                PaymentMaster bgFeeRow = paymentMasterRepository
                        .resolveApplicable(SERVICE_CODE, "BG_UPFRONT_FEE", "BG SUBMITTED").orElse(null);
                boolean bgFeeEnabled = bgFeeRow != null && Boolean.TRUE.equals(bgFeeRow.getIsEnabled());

                if (bgFeeEnabled && paymentEnabled) {
                    if (request.getUpfrontPaymentAmount() == null) {
                        throw new BusinessException(ErrorCodes.MISSING_REQUIRED_FIELD, "Upfront payment amount is required");
                    }
                    miningLeaseApplication.setCurrentStatus("BG PAYMENT PENDING");
                    applicationMaster.setCurrentStatus("BG PAYMENT PENDING");
                    applicationMasterRepository.save(applicationMaster);
                    miningLeaseApplicationRepository.save(miningLeaseApplication);

                    PaymentInitiationResponse paymentResp = mastersPaymentClient.initiate(
                            buildBgPaymentRequest(miningLeaseApplication, bgFeeRow.getServiceCode(), request.getUpfrontPaymentAmount()));

                    MiningLeaseResponse response = mapper.toResponse(miningLeaseApplication);
                    response.setRedirectUrl(paymentResp.getRedirectUrl());
                    log.info("BG upfront payment initiated for application {}", request.getApplicationNo());
                    return response;
                }

                miningLeaseApplication.setCurrentStatus("BG SUBMITTED");
                applicationMaster.setCurrentStatus("BG SUBMITTED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseApplicationRepository.save(miningLeaseApplication);

                if (miningLeaseApplication.getApplicantUserId() != null) {
                    notificationClient.sendUserNotification(
                            "Bank guarantee details submitted.",
                            "Your bank guarantee details for application " + miningLeaseApplication.getApplicationNumber() + " have been submitted.",
                            miningLeaseApplication.getApplicantUserId(), "78", "CITIZEN");
                }
            }
        }
        return mapper.toResponse(miningLeaseApplication);
    }

    /**
     * Called by the payment callback once the BG upfront payment is confirmed as PAID.
     * Transitions status BG PAYMENT PENDING → BG SUBMITTED (today's existing next step).
     */
    @Transactional
    public void onBgPaymentConfirmed(String applicationNo) {
        MiningLeaseApplication application = miningLeaseApplicationRepository.findByApplicationNumber(applicationNo)
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "Application not found: " + applicationNo));
        application.setCurrentStatus("BG SUBMITTED");
        ApplicationMaster master = application.getApplicationMaster();
        master.setCurrentStatus("BG SUBMITTED");
        applicationMasterRepository.save(master);
        miningLeaseApplicationRepository.save(application);
        log.info("BG upfront payment confirmed — application {} transitioned to BG SUBMITTED", applicationNo);

        if (application.getApplicantUserId() != null) {
            notificationClient.sendUserNotification(
                    "Bank guarantee payment confirmed.",
                    "Your bank guarantee upfront payment for application " + applicationNo + " has been confirmed.",
                    application.getApplicantUserId(), "78", "CITIZEN");
        }
    }

    private PaymentInitiationRequest buildBgPaymentRequest(MiningLeaseApplication application, String serviceCode, BigDecimal upfrontAmount) {
        PaymentInitiationRequest.PaymentItemRequest item = new PaymentInitiationRequest.PaymentItemRequest();
        item.setFeeType("BG_UPFRONT_FEE");
        item.setServiceCode(serviceCode);
        item.setDescription("Mining Lease Bank Guarantee Upfront Payment");
        item.setQuantity(1);
        item.setAmount(upfrontAmount); // caller-supplied — this amount is inherently case-specific, never centrally configured

        String documentNo = resolveDocumentNo(application);

        PaymentInitiationRequest req = new PaymentInitiationRequest();
        req.setApplicationId(application.getApplicationNumber());
        req.setApplicationType("MINING_LEASE");
        req.setTaxPayerName(application.getApplicantName());
        req.setTaxPayerDocumentNo(documentNo);
        req.setTaxPayerNo(documentNo);
        req.setPlatform("MAS");
        req.setOnPaidStatus("BG SUBMITTED");
        req.setCallbackUrl(selfBaseUrl + "/api/mining-lease/bg-payment-callback");
        req.setPaymentItems(List.of(item));
        return req;
    }

    @Transactional
    public MiningLeaseResponse deleteFileUpload(DeleteFileRequest request) {
        MiningLeaseApplication miningLeaseApplication = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNumber()).orElseThrow();

        switch (request.getFileType()) {
            case "PFS" -> miningLeaseApplication.setPfsDocId(null);
            case "GR" -> miningLeaseApplication.setFileUploadIdGr(null);
            case "PA" -> miningLeaseApplication.setFileUploadIdPA(null);
            case "FC" -> miningLeaseApplication.setFileUploadIdFC(null);
            case null, default -> miningLeaseApplication.setFmfsDocId(null);
        }
        miningLeaseApplicationRepository.save(miningLeaseApplication);
        return mapper.toResponse(miningLeaseApplication);
    }

    public SuccessResponse<List<MiningLeaseResponse>> getAssignedToDirector(Long userId, Pageable pageable, String search) {
        Page<MiningLeaseApplication> page;

        List<String> archivedStatuses = List.of(
                "MINING LEASE APPROVED",
                "REJECTED",
                "TERMINATED",
                "RENEWAL APPLICATION",
                "TEMPORARY CLOSURE APPROVED",
                "UNDER-REVIEW-TERMINATION"
        );

        if (search == null || search.isBlank()) {

            page = miningLeaseApplicationRepository
                    .findAssignedToUserDirector(userId, archivedStatuses, pageable);

        } else {

            page = miningLeaseApplicationRepository
                    .findAssignedToUserAndSearchDirector(
                            userId,
                            archivedStatuses,
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

                    List<TaskManagement> taskManagement = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(app.getApplicationNumber(),"FMFS SUBMITTED","MINE ENGINEER", SERVICE_CODE);
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

                    if (mineEngineerId != null) {
                        String title = "Mining lease application forwarded for review.";
                        String message = "Director has approved FMFS. Application No. " + app.getApplicationNumber() + " has been forwarded to you for review.";
                        String serviceId = "78";
                        notificationClient.sendUserNotification(title, message, mineEngineerId, serviceId, "STAFF");
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
        MiningLeaseApplication miningLeaseApplication = findApplicationById(request.getApplicationId());

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
                notificationClient.sendUserNotification(title, message, userGeologist.getUserId(), serviceId, "STAFF");
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
                notificationClient.sendUserNotification(title, message, userGeologist.getUserId(), serviceId, "STAFF");
            }
        }
        return mapper.toResponse(miningLeaseApplication);
    }

    @Transactional
    public MiningLeaseResponse reviewApplicationGeologist(@Valid ReviewMiningLeaseApplicationGeologist reviewQuarryLeaseApplicationGeologist, Long userId) {
        log.info("Reviewing mining lease application by Geologist user: {}", userId);

        Long geologistRegionId = 0L;
                //
        Optional<User> user = userRepository.findById(userId);

        if(user.isPresent()) {
            User user1 = user.get();
            geologistRegionId = user1.getRegion().getId();
        }

        MiningLeaseApplication miningLeaseApplication = findApplicationById(reviewQuarryLeaseApplicationGeologist.getId());
        ApplicationMaster applicationMaster = miningLeaseApplication.getApplicationMaster();

        miningLeaseApplication.setRegionId(geologistRegionId);

        // Complete current task
        completeCurrentTask(applicationMaster, reviewQuarryLeaseApplicationGeologist.getStatus(), reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());

        if(reviewQuarryLeaseApplicationGeologist.getStatus() != null) {
            switch (reviewQuarryLeaseApplicationGeologist.getStatus()) {
                case "ACCEPTED" -> {
                    boolean fullyApprovedPfs = Objects.equals(miningLeaseApplication.getCurrentStatus(), "ACCEPTED PFS MPCD");
                    if (fullyApprovedPfs) {
                        miningLeaseApplication.setCurrentStatus("APPROVED");
                    }else {
                        miningLeaseApplication.setCurrentStatus("ACCEPTED PFS");
                    }

                    miningLeaseApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseApplication.setGeologistReviewedAt(LocalDateTime.now());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus(miningLeaseApplication.getCurrentStatus());
                        applicationMasterRepository.save(applicationMaster);
                    }

                    // Only forward to the Mining Engineer once PFS is fully approved by both
                    // Geologist and MPCD (see the symmetric check in the MPCD review branch) —
                    // otherwise this is still waiting on the other reviewer.
                    if (fullyApprovedPfs && applicationMaster != null) {
                        // Region -> HQ region -> escalate is now handled natively by the resolver.
                        var resolvedMineEngineer = workflowAssignmentClient.resolve(
                                SERVICE_CODE, "FMFS SUBMITTED", geologistRegionId, miningLeaseApplication.getApplicationNumber());
                        UserWorkloadProjection assignedMineEngineer = toProjection(resolvedMineEngineer);

                        {
                            createTask(applicationMaster, miningLeaseApplication, "MINE ENGINEER", userId, assignedMineEngineer.getUserId());

                            if (assignedMineEngineer.getUserId() != null) {
                                String meTitle = "Mining lease application has been assigned for review.";
                                String meMessage = "A mining lease application has been forwarded to you for review.";
                                String meServiceId = "78";
                                notificationClient.sendUserNotification(meTitle, meMessage, assignedMineEngineer.getUserId(), meServiceId, "STAFF");
                            }
                        }
                    }

                    if (miningLeaseApplication.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                miningLeaseApplication.getApplicantEmail(),
                                miningLeaseApplication.getApplicantName(),
                                miningLeaseApplication.getApplicationNumber(),
                                "Mining Engineer Review",
                                "Your application has been forwarded to the Mining Engineer for review.");
                    }else {
                        throw new BusinessException(ErrorCodes.DATA_INTEGRITY_VIOLATION, "Applicant email ID is not present.");
                    }

                    String title = "Application status updated.";
                    String message = "Your application has been forwarded to geologist to review.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, miningLeaseApplication.getApplicantUserId(), serviceId, "CITIZEN");
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
                    }else {
                        throw new BusinessException(ErrorCodes.DATA_INTEGRITY_VIOLATION, "Applicant email ID is not present.");
                    }

                    if(miningLeaseApplication.getApplicantUserId() != null) {
                        String title = "Geological report has been approved successfully.";
                        String message = "Geological Report has been accepted. Please upload mining lease application and PFS to proceed further.";
                        String serviceId = "78";
                        notificationClient.sendUserNotification(title, message, miningLeaseApplication.getApplicantUserId(), serviceId, "CITIZEN");
                    }else {
                        throw new BusinessException(ErrorCodes.DATA_INTEGRITY_VIOLATION, "Applicant user ID is not present.");
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
                    miningLeaseApplication.setCurrentStatus("RESUBMIT PFS GEOLOGIST");
                    miningLeaseApplication.setRemarksGeologist(reviewQuarryLeaseApplicationGeologist.getGeologistRemarks());
                    miningLeaseApplication.setGeologistReviewedAt(LocalDateTime.now());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("RESUBMIT PFS GEOLOGIST");
                        applicationMasterRepository.save(applicationMaster);
                    }
                    createRevisionRecord(miningLeaseApplication, "GEOLOGIST_REVIEW", reviewQuarryLeaseApplicationGeologist.getGeologistRemarks(), userId);
                    assert applicationMaster != null;
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

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("RESUBMIT GR");
                        applicationMasterRepository.save(applicationMaster);
                    }

                    createRevisionRecord(miningLeaseApplication, "GEOLOGIST_REVIEW", reviewQuarryLeaseApplicationGeologist.getGeologistRemarks(), userId);
                    assert applicationMaster != null;
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

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("ADDITIONAL DATA NEEDED FMFS");
                        applicationMasterRepository.save(applicationMaster);
                    }

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
                    List<TaskManagement> taskManagement = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(miningLeaseApplication.getApplicationNumber(),"ASSIGNED","GEOLOGIST", SERVICE_CODE);
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
                    notificationClient.sendUserNotification(title, message, miningLeaseApplication.getApplicantUserId(), serviceId, "CITIZEN");

                    if (geologistId != null) {
                        String geoTitle = "Mining lease application forwarded for review.";
                        String geoMessage = "Application No. " + miningLeaseApplication.getApplicationNumber() + " has been forwarded to you for review.";
                        notificationClient.sendUserNotification(geoTitle, geoMessage, geologistId, serviceId, "STAFF");
                    }
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
                    notificationClient.sendUserNotification(title, message, miningLeaseApplication.getApplicantUserId(), serviceId, "CITIZEN");

                }
                case "Approved PA/FC" -> {
                    miningLeaseApplication.setCurrentStatus("APPROVED PA/FC");
                    miningLeaseApplication.setRemarksMPCD(reviewQuarryLeaseApplication.getMpcdRemarks());
                    miningLeaseApplication.setMpcdReviewedAt(LocalDateTime.now());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus(miningLeaseApplication.getCurrentStatus());
                        applicationMasterRepository.save(applicationMaster);
                    }

                    assert applicationMaster != null;
                    createTask(applicationMaster, miningLeaseApplication, "APPLICANT", userId, miningLeaseApplication.getApplicantUserId());

                    if (miningLeaseApplication.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                miningLeaseApplication.getApplicantEmail(),
                                miningLeaseApplication.getApplicantName(),
                                miningLeaseApplication.getApplicationNumber(),
                                "PA/FC accepted by MPCD",
                                "PA/FC has been approved. Please proceed by uploading FMFS before the set deadline.");
                    }

                    String title = "Application status updated.";
                    String message = "PA/FC has been approved by MPCD. Please proceed by uploading FMFS before the set deadline.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, miningLeaseApplication.getApplicantUserId(), serviceId, "CITIZEN");

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
                    miningLeaseApplication.setCurrentStatus("RESUBMIT PFS MPCD");
                    miningLeaseApplication.setRemarksMPCD(reviewQuarryLeaseApplication.getMpcdRemarks());
                    miningLeaseApplication.setMpcdReviewedAt(LocalDateTime.now());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("RESUBMIT PFS MPCD");
                        applicationMasterRepository.save(applicationMaster);
                    }

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
                case "Resubmit PA/FC" -> {
                    miningLeaseApplication.setCurrentStatus("RESUBMIT PA/FC");
                    miningLeaseApplication.setRemarksMPCD(reviewQuarryLeaseApplication.getMpcdRemarks());
                    miningLeaseApplication.setMpcdReviewedAt(LocalDateTime.now());

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("RESUBMIT PA/FC");
                        applicationMasterRepository.save(applicationMaster);
                    }

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

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("RESUBMIT APPLICATION");
                        applicationMasterRepository.save(applicationMaster);
                    }

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
                miningleaseapplication.setSignedPFSId(request.getSignedPFSId());
                miningleaseapplication.setCurrentStatus("MA SUBMITTED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseApplicationRepository.save(miningleaseapplication);

                createTask(applicationMaster,miningleaseapplication,"APPLICANT", userId, miningleaseapplication.getApplicantUserId());

                if(miningleaseapplication.getApplicantUserId() != null) {
                    String title = "Mining lease application has been send to you with MA document.";
                    String message = "Mining lease application has been  send to you with MA document. Please log in the system to upload PA/FC document.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, miningleaseapplication.getApplicantUserId(), serviceId, "CITIZEN");
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
                    taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode
                            (applicationNo,"ASSIGNED", "GEOLOGIST", SERVICE_CODE);
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

        completeCurrentTask(master, request.getStatus(), request.getRemarks());

        TaskManagement task = taskManagementRepository.findByApplicationNumberAndAssignedToRoleAndTaskStatusAndServiceCode
                (app.getApplicationNumber(),"DIRECTOR", "GR SUBMITTED", SERVICE_CODE);

        Long directorId = task.getAssignedToUserId();

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

                    if (app.getApplicantEmail() != null) {
                        notificationClient.sendStatusUpdateNotification(
                                app.getApplicantEmail(),
                                app.getApplicantName(),
                                app.getApplicationNumber(),
                                "Mining Engineer Review",
                                "Your application has been returned to the Mining Engineer for review.");
                    }
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
                    // Region -> HQ region -> escalate is now handled natively by the resolver.
                    UserWorkloadProjection assignedMiningChief =
                            assignMiningChief("APPROVED", app.getRegionId(), app.getApplicationNumber());

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

                    if (assignedMiningChief.getUserId() != null) {
                        String title = "An new application has been assigned.";
                        String message = "An application for mining lease has been assigned for review. Application No. "+ app.getApplicationNumber() +" Please login in review the application";
                        String serviceId = "78";
                        notificationClient.sendUserNotification(title, message, assignedMiningChief.getUserId(), serviceId, "STAFF");
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


                    // Newly added after review
                    app.setNameOfMine(request.getNameOfMine());
                    app.setLeaseStartDate(request.getLeaseStartDate());
                    app.setLeaseEndDate(request.getLeaseEndDate());


                    FmfsDetails fmfsDetails = new FmfsDetails();
                    fmfsDetails.setApplicantCid(app.getApplicantCid());
                    fmfsDetails.setApplicantContact(app.getApplicantContact());
                    fmfsDetails.setApplicantEmail(app.getApplicantEmail());
                    fmfsDetails.setApplicantName(app.getApplicantName());
                    fmfsDetails.setApplicantUserId(app.getApplicantUserId());
                    fmfsDetails.setApplicantUserId(app.getApplicantUserId());

                    fmfsDetails.setBusinessLicenseNo(app.getBusinessLicenseNo());
                    fmfsDetails.setCompanyName(app.getCompanyName());

                    fmfsDetails.setApplicationNumber(app.getApplicationNumber());
                    fmfsDetails.setApprovedAt(LocalDateTime.now());

                    fmfsDetails.setCurrentStatus(app.getCurrentStatus());
                    fmfsDetails.setDungkhag(app.getDungkhag());
                    fmfsDetails.setDzongkhag(app.getDzongkhag().getDzongkhagName());
                    fmfsDetails.setGewog(app.getGewog().getGewogName());

                    fmfsDetails.setLeaseEndDate(request.getLeaseEndDate());
                    fmfsDetails.setLeaseStartDate(request.getLeaseStartDate());

                    fmfsDetails.setLicenseNo(app.getLicenseNo());
                    fmfsDetails.setNearestVillage(app.getNearestVillage().getVillageName());
                    fmfsDetails.setPlaceOfMiningActivity(app.getPlaceOfMiningActivity());

                    fmfsDetails.setPostalAddress(app.getPostalAddress());
                    fmfsDetails.setProposedLeasePeriod(app.getProposedLeasePeriod());

                    fmfsDetails.setRequiredInvestment(app.getRequiredInvestment());
                    fmfsDetails.setSourceOfFinance(app.getSourceOfFinance());
                    fmfsDetails.setSrf(app.getSrf());

                    fmfsDetails.setTechnicalCompetenceExperience(app.getTechnicalCompetenceExperience());
                    fmfsDetails.setTelephoneNo(app.getTelephoneNo());
                    fmfsDetails.setTotalLand(app.getTotalLand());
                    fmfsDetails.setTypeOfMineralsProducts(app.getTypeOfMineralsProducts());
                    fmfsDetails.setTypeOfMines(app.getTypeOfMines());

                    fmfsDetails.setWorkforceRequirementRecruitment(app.getWorkforceRequirementRecruitment());
                    fmfsDetails.setApplicationMasterId(app.getApplicationMaster().getId());
                    fmfsDetails.setApplicationType(app.getApplicationType());
                    fmfsDetails.setApplicationFeesRequired(app.getApplicationFeesRequired());
                    fmfsDetails.setConsentLetterDocId(app.getConsentLetterDocId());
                    fmfsDetails.setExplorationReportDocId(app.getExplorationReportDocId());
                    fmfsDetails.setFinancialCapabilityDocId(app.getFinancialCapabilityDocId());
                    fmfsDetails.setFmfsDocId(app.getFmfsDocId());
                    fmfsDetails.setGeologicalReportDocId(app.getGeologicalReportDocId());
                    fmfsDetails.setLocationMapDocId(app.getLocationMapDocId());
                    fmfsDetails.setPfsDocId(app.getPfsDocId());

                    fmfsDetails.setMpcdFileUploadIdPa(app.getMpcdFileUploadIdPA());
                    fmfsDetails.setRemarksMpcd(app.getRemarksMPCD());
                    fmfsDetails.setRemarksGeologist(app.getRemarksGeologist());

                    fmfsDetails.setApprovedArea(app.getApprovedArea());
                    fmfsDetails.setApprovedErb(app.getApprovedErb());
                    fmfsDetails.setApprovedLeasePeriod(app.getApprovedLeasePeriod());
                    fmfsDetails.setApprovedMineral(app.getApprovedMineral());

                    fmfsDetails.setChiefReviewedAt(app.getChiefReviewedAt());
                    fmfsDetails.setDirectorReviewedAt(app.getDirectorReviewedAt());

                    fmfsDetails.setFmfsStatus(app.getFmfsStatus());
                    fmfsDetails.setGeologicalReportStatus(app.getGeologicalReportStatus());
                    fmfsDetails.setGeologistReviewedAt(app.getGeologistReviewedAt());

                    fmfsDetails.setLlcDocId(app.getLlcDocId());
                    fmfsDetails.setMeReviewedAt(app.getMeReviewedAt());

                    fmfsDetails.setMlaDocId(app.getMlaDocId());
                    fmfsDetails.setMlaSignedAt(app.getMlaSignedAt());
                    fmfsDetails.setMlaStatus(app.getMlaStatus());

                    fmfsDetails.setMpcdReviewedAt(app.getMpcdReviewedAt());
                    fmfsDetails.setNotesheetDocId(app.getNotesheetDocId());
                    fmfsDetails.setRemarksChief(app.getRemarksChief());
                    fmfsDetails.setRemarksDirector(app.getRemarksDirector());
                    fmfsDetails.setRemarksMe(app.getRemarksME());
                    fmfsDetails.setMpcdFileUploadIdMa(app.getMpcdFileUploadIdMa());
                    fmfsDetails.setFileUploadIdGr(app.getFileUploadIdGr());
                    fmfsDetails.setFileUploadIdPaFc(app.getFileUploadIdFC());
                    fmfsDetails.setFmfsId(app.getFmfsId());
                    fmfsDetails.setCompanyRegistrationNo(app.getCompanyRegistrationNo());
                    fmfsDetails.setCompanyType(app.getCompanyType());
                    fmfsDetails.setEcNo(app.getEcNumber());
                    fmfsDetails.setEcStatus(app.getECStatus());

                    fmfsDetails.setBankGurantorDocId(app.getBankGuarantorDocId());
                    fmfsDetails.setUpfrontPaymentAmount(app.getUpfrontPaymentAmount());
                    fmfsDetails.setWorkOrderDocId(app.getWorkOrderDocId());
                    fmfsDetails.setWorkOrderRemarks(app.getWorkOrderRemarks());
                    fmfsDetails.setDzongkhagId(app.getDzongkhag().getId());
                    fmfsDetails.setGewogId(Integer.valueOf(app.getGewog().getGewogId()));
                    fmfsDetails.setVillageId(Integer.valueOf(app.getNearestVillage().getVillageId()));

                    fmfsDetails.setFileUploadIdPa(app.getFileUploadIdPA());
                    fmfsDetails.setFileUploadIdFc(app.getFileUploadIdFC());
                    fmfsDetails.setFileUploadIdPublicClearance(app.getFileUploadIdPublicClearance());

                    fmfsDetails.setCreatedAt(LocalDateTime.now());
                    fmfsDetails.setCreatedBy(app.getApplicantUserId());

                    fmfsDetailsRepository.save(fmfsDetails);

                    if (master != null) {
                        master.setCurrentStatus("MINING_CHIEF_REVIEW");
                        applicationMasterRepository.save(master);
                    }

                    // =====================================================
                    // 8. ASSIGN DIRECTOR + CREATE TASK
                    // =====================================================
                    // Region -> HQ region -> escalate is now handled natively by the resolver.
                    UserWorkloadProjection assignedMiningChief =
                            assignMiningChief("MINING_CHIEF_REVIEW", app.getRegionId(), app.getApplicationNumber());

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

                    if (assignedMiningChief.getUserId() != null && assignedMiningChief.getEmail() != null) {
                        notificationClient.sendAssignmentNotification(
                                assignedMiningChief.getEmail(),
                                assignedMiningChief.getUsername(),
                                app.getApplicationNumber(),
                                "Mining Chief Review");

                        String title = "An new application has been assigned.";
                        String message = "An application for mining lease has been assigned for review. Application No. "+ app.getApplicationNumber() +" Please login in review the application";
                        String serviceId = "78";
                        notificationClient.sendUserNotification(title, message, assignedMiningChief.getUserId(), serviceId, "STAFF");
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

                    if (master != null) {
                        master.setCurrentStatus("RESUBMIT FMFS");
                        applicationMasterRepository.save(master);
                    }

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

    /**
     * Resolves the Mining Chief to forward to, via the admin-configured
     * mas_db.t_workflow_step rule. When no eligible candidate is found, the
     * returned projection has a null userId — masters has already recorded it
     * in the escalation queue.
     */
    @Transactional
    public UserWorkloadProjection assignMiningChief(String triggerStatus, Long regionId, String applicationNumber) {
        var resolved = workflowAssignmentClient.resolve(SERVICE_CODE, triggerStatus, regionId, applicationNumber);
        return toProjection(resolved);
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

    /** Team-wide Mine Engineer queue — every pending application regardless of who it auto-assigned to */
    public SuccessResponse<List<MiningLeaseResponse>> getMineEngineerTeamQueue(Pageable pageable, String search) {
        Page<MiningLeaseApplication> page = (search == null || search.isBlank())
                ? miningLeaseApplicationRepository.findTeamQueueMineEngineer(pageable)
                : miningLeaseApplicationRepository.findTeamQueueWithSearchMineEngineer(search.trim(), pageable);

        Page<MiningLeaseResponse> responsePage = page.map(app -> {
            MiningLeaseResponse response = mapper.toResponse(app);
            taskManagementRepository.findByApplicationNumber(app.getApplicationNumber())
                    .stream()
                    .filter(t -> t.getAssignedToUserId() != null)
                    .findFirst()
                    .ifPresent(t -> {
                        response.setAssignedEngineerUserId(t.getAssignedToUserId());
                        UserWorkloadProjection engineer = miningLeaseApplicationRepository.findUserDetails(t.getAssignedToUserId());
                        response.setAssignedEngineerName(engineer != null ? engineer.getUsername() : null);
                    });
            return response;
        });

        return SuccessResponse.fromPage(
                "Team-wide queue fetched successfully",
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

                // Keep the open Mining Engineer task alive but record progress, since the same
                // engineer still has the Note Sheet + Work Order steps left before this task closes.
                workflowTrackingService.updateCurrentTask(
                        quarryLeaseApplication1.getApplicationNumber(), SERVICE_CODE,
                        "IN_PROGRESS", "LLC_UPLOADED", "LLC document uploaded by mine engineer.");

                if(quarryLeaseApplication1.getApplicantUserId() != null) {
                    String title = "LLC has been uploaded by mine engineer.";
                    String message = "LLC for you application has been uploaded by mine engineer.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, quarryLeaseApplication1.getApplicantUserId(), serviceId, "CITIZEN");
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

                // Same open Mining Engineer task, one step closer to the Work Order — not closed yet.
                workflowTrackingService.updateCurrentTask(
                        quarryLeaseApplication1.getApplicationNumber(), SERVICE_CODE,
                        "IN_PROGRESS", "NOTE_SHEET_UPLOADED", "Note sheet uploaded by mine engineer.");

                if(quarryLeaseApplication1.getApplicantUserId() != null) {
                    String title = "Note sheet has been uploaded by mine engineer.";
                    String message = "Note sheet for you application has been uploaded by mine engineer.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, quarryLeaseApplication1.getApplicantUserId(), serviceId, "CITIZEN");
                }

            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        }
        return mapper.toResponse(quarryLeaseApplication1);
    }

    @Transactional
    public MiningLeaseResponse submitWorkOrder(@Valid MiningLeaseWorkOrderRequest request) {
        MiningLeaseApplication miningLeaseApplication = null;
        if (request.getApplicationNo() != null) {
            Optional<MiningLeaseApplication> quarryLeaseApplication = miningLeaseApplicationRepository.findByApplicationNumber(request.getApplicationNo());
            if (quarryLeaseApplication.isPresent()) {
                miningLeaseApplication = quarryLeaseApplication.get();
                ApplicationMaster applicationMaster = miningLeaseApplication.getApplicationMaster();
                LocalDateTime now = LocalDateTime.now();
                applicationMaster.setCurrentStatus("MINING LEASE APPROVED");
                applicationMaster.setApprovedAt(now);
                applicationMaster.setCompletedAt(now);
                miningLeaseApplication.setWorkOrderDocId(request.getWorkOrderDocId());
                miningLeaseApplication.setWorkOrderRemarks(request.getRemarks());
                miningLeaseApplication.setCurrentStatus("MINING LEASE APPROVED");
                applicationMasterRepository.save(applicationMaster);
                miningLeaseApplicationRepository.save(miningLeaseApplication);

                SiteMaster provisionedSite = siteProvisioningService.provisionSiteForApprovedLease(miningLeaseApplication);
                applicationMaster.setSiteId(provisionedSite.getId());
                applicationMasterRepository.save(applicationMaster);
                recordApprovedForThreshold(miningLeaseApplication);

                // Terminal approval — close out the open Mining Engineer task instead of leaving
                // it dangling in the queue forever.
                workflowTrackingService.completeCurrentTask(
                        miningLeaseApplication.getApplicationNumber(), SERVICE_CODE,
                        "APPROVED", "Work order uploaded; mining lease approved.");

                if(miningLeaseApplication.getApplicantUserId() != null) {
                    String title = "Work order has been uploaded by mine engineer.";
                    String message = "Work order for your application has been uploaded by mine engineer. Your application for mining lease has been approved.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, miningLeaseApplication.getApplicantUserId(), serviceId, "CITIZEN");
                }

                HouseholdPermitThresholdEntity entity = new HouseholdPermitThresholdEntity();

                entity.setServiceType(SERVICE_CODE);
                entity.setApplicationNo(miningLeaseApplication.getApplicationNumber());
                entity.setPermitNo(miningLeaseApplication.getExpPermitNo());
                entity.setApplicantCid(miningLeaseApplication.getApplicantCid());
                entity.setStatus("ACTIVE");

                householdPermitThresholdRepository.save(entity);

            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
            }
        }
        return mapper.toResponse(miningLeaseApplication);
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
                if (taskManagement != null && !taskManagement.isEmpty()) {
                    TaskManagement taskManagement1 = taskManagement.getFirst();
                    directorId = taskManagement1.getAssignedToUserId();
                }

                createTask(applicationMaster,quarryLeaseApplication1,"DIRECTOR", userId, directorId);

                UserWorkloadProjection assignedDirectorDetails = miningLeaseApplicationRepository.findUserDetails(directorId);
                if(assignedDirectorDetails != null && assignedDirectorDetails.getUserId() != null) {
                    String title = "Mining lease application has been assigned for MLA review.";
                    String message = "Mining lease application has been  assigned for MLA review.";
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, assignedDirectorDetails.getUserId(), serviceId, "STAFF");
                }

                if(quarryLeaseApplication1.getApplicantUserId() != null) {
                    String title = "MLA submitted successfully.";
                    String message = "Your MLA has been submitted and forwarded to the Director for review."+ "Application No. : "+ quarryLeaseApplication1.getApplicationNumber();
                    String serviceId = "78";
                    notificationClient.sendUserNotification(title, message, quarryLeaseApplication1.getApplicantUserId(), serviceId, "CITIZEN");
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

        List<TaskManagement> taskManagement = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(request.getApplicationNumber(), "GEOLOGIST", "GEOLOGIST", SERVICE_CODE);
        if(!taskManagement.isEmpty()) {
            TaskManagement taskManagement1 = taskManagement.getFirst();
            geologistId = taskManagement1.getAssignedToUserId();
        }

        List<TaskManagement> taskManagement2 = taskManagementRepository.findByApplicationNumberAndTaskStatusAndAssignedToRoleAndServiceCode(request.getApplicationNumber(), "MPCD_FOCAL", "MPCD", SERVICE_CODE);
        if(!taskManagement2.isEmpty()) {
            TaskManagement taskManagement3 = taskManagement.getFirst();
            mpcdId = taskManagement3.getAssignedToUserId();
        }

        if(request.getFileType().equals("PFS")) {

            quarryLeaseApplication.setPfsDocId(request.getFileId());

            if(quarryLeaseApplication.getCurrentStatus().equals("RESUBMIT PFS GEOLOGIST")) {
                applicationMaster.setCurrentStatus("RESUBMITTED PFS GEOLOGIST");
                quarryLeaseApplication.setCurrentStatus("RESUBMITTED PFS GEOLOGIST");
                createTask(applicationMaster,quarryLeaseApplication,"GEOLOGIST",userId,geologistId);
                notifyStaffAssignment(geologistId, quarryLeaseApplication.getApplicationNumber(), "PFS resubmitted for your review.");
            }else {
                applicationMaster.setCurrentStatus("RESUBMITTED PFS MPCD");
                quarryLeaseApplication.setCurrentStatus("RESUBMITTED PFS MPCD");
                createTask(applicationMaster,quarryLeaseApplication,"MPCD FOCAL",userId,mpcdId);
                notifyStaffAssignment(mpcdId, quarryLeaseApplication.getApplicationNumber(), "PFS resubmitted for your review.");
            }
        } else if (request.getFileType().equals("GR")) {
            quarryLeaseApplication.setFileUploadIdGr(Long.valueOf(request.getFileId()));
            applicationMaster.setCurrentStatus("RESUBMITTED GR");
            quarryLeaseApplication.setCurrentStatus("RESUBMITTED GR");

            createTask(applicationMaster,quarryLeaseApplication,"GEOLOGIST",userId,geologistId);
            notifyStaffAssignment(geologistId, quarryLeaseApplication.getApplicationNumber(), "Geological report resubmitted for your review.");

        }else {
            quarryLeaseApplication.setFmfsDocId(request.getFileId());
            applicationMaster.setCurrentStatus("RESUBMITTED FMFS");
            quarryLeaseApplication.setCurrentStatus("RESUBMITTED FMFS");

            createTask(applicationMaster,quarryLeaseApplication,"GEOLOGIST",userId,geologistId);
            createTask(applicationMaster,quarryLeaseApplication,"MPCD FOCAL",userId,mpcdId);
            notifyStaffAssignment(geologistId, quarryLeaseApplication.getApplicationNumber(), "FMFS resubmitted for your review.");
            notifyStaffAssignment(mpcdId, quarryLeaseApplication.getApplicationNumber(), "FMFS resubmitted for your review.");
        }
        applicationMasterRepository.save(applicationMaster);
        miningLeaseApplicationRepository.save(quarryLeaseApplication);

        if (quarryLeaseApplication.getApplicantUserId() != null) {
            notificationClient.sendUserNotification(
                    "Application resubmitted successfully.",
                    "Your mining lease application " + quarryLeaseApplication.getApplicationNumber() + " has been resubmitted.",
                    quarryLeaseApplication.getApplicantUserId(), "78", "CITIZEN");
        }

        return mapper.toResponse(quarryLeaseApplication);
    }

    private void notifyStaffAssignment(Long staffUserId, String applicationNumber, String message) {
        if (staffUserId == null) {
            return;
        }
        notificationClient.sendUserNotification(
                "Mining lease application resubmitted.",
                "Application " + applicationNumber + ": " + message,
                staffUserId, "78", "STAFF");
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

    private Integer getExistingPermitCount(MiningLeaseGRRequest request, Long userId) {

        String applicantType = request.getApplicantType();

        if ("INDIVIDUAL".equalsIgnoreCase(applicantType)) {
            String householdNumber =
                    miningLeaseApplicationRepository.findUserHouseHoldNumber(userId);

            return miningLeaseApplicationRepository
                    .countMiningLeasesByHousehold(householdNumber);
        }

        String identifier;

        if ("BUSINESS_LICENSE".equalsIgnoreCase(applicantType)) {
            identifier = request.getBusinessLicenseNo();
        } else if ("REGISTERED_COMPANY".equalsIgnoreCase(applicantType)) {
            identifier = request.getCompanyRegistrationNo();
        } else {
            throw new BusinessException(
                    ErrorCodes.INVALID_REQUEST,
                    "Invalid applicant type."
            );
        }

        return miningLeaseApplicationRepository
                .countByApplicantCidAndServiceType(identifier, SERVICE_CODE);
    }

    public SuccessResponse<List<MiningLeaseResponse>> getArchivedApplicationApproved(Long userId, Pageable pageable, String search) {

        Page<MiningLeaseApplication> page;

        List<String> archivedStatuses = List.of(
                "MINING LEASE APPROVED"
        );

        if (search == null || search.isBlank()) {

            page = miningLeaseApplicationRepository
                    .findByStatusIn(archivedStatuses, pageable);

        } else {

            page = miningLeaseApplicationRepository
                    .findByStatusInAndSearch(
                            archivedStatuses,
                            search,
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
}
