package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.MiningLeaseApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.request.MiningLeaseGRRequest;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.entity.*;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.exception.UnauthorizedOperationException;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.mapper.MiningLeaseMapper;
import com.mas.gov.bt.mas.primary.repository.*;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
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

    private static final String SERVICE_CODE = "MINING LEASE";
    private static final String SERVICE_NAME = "Mining Lease Application";
    private static final int DEFAULT_TAT_DAYS = 2;

    private final MiningLeaseMapper mapper;

    private final MiningLeaseApplicationRepository miningLeaseApplicationRepository;

    private final DzongkhagLookupRepository dzongkhagLookupRepository;

    private final GewogLookupRepository  gewogLookupRepository;

    private final VillageLookupRepository villageLookupRepository;

    private final PaymentMasterRepository paymentMasterRepository;

    private final ApplicationMasterRepository applicationMasterRepository;

    private final TaskManagementRepository taskManagementRepository;

    private final NotificationClient notificationClient;
    /**
     * Create a new application.
     * If applicationType is "Draft", save as draft. Otherwise, submit immediately.
     */
    @Transactional
    public MiningLeaseResponse createApplication(
            MiningLeaseApplicationRequest request,
            Long userId) {

        log.info("Creating/updating quarry lease application for user: {}", userId);

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
                    .findById(String.valueOf(request.getDzongkhag()))
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

//        validateApplicationForSubmission(application);

        boolean feeRequired =
                paymentMaster != null &&
                        Boolean.TRUE.equals(paymentMaster.getIsApplicationFeeEnabled());

        if (feeRequired) {
            application.setCurrentStatus("PAYMENT PENDING");
            application.setApplicationFeesAmount(application.getApplicationFeesAmount());
            application.setApplicationFeesRequired(true);
        } else {
            application.setCurrentStatus("SUBMITTED");
        }

        application.setSubmittedAt(now);
        application.setApplicationNumber(applicationNumber);
        // =====================================================
        // 7. UPDATE APPLICATION MASTER
        // =====================================================
        ApplicationMaster master = application.getApplicationMaster();

        master.setApplicationNumber(applicationNumber);
        master.setCurrentStatus(application.getCurrentStatus());
        master.setSubmittedAt(now);
        applicationMasterRepository.save(master);



        // =====================================================
        // 9. NOTIFICATIONS
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
                    "37"
            );
        }



        // =====================================================
        // 10. FINAL SAVE (ONLY ONCE)
        // =====================================================
        miningLeaseApplicationRepository.save(application);

        log.info("Application submitted successfully: {}",
                application.getApplicationNumber());

        return mapper.toResponse(application);
    }

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

    private ApplicationMaster createApplicationMaster(String applicationNumber, Long userId) {
        ApplicationMaster master = new ApplicationMaster();
        master.setApplicationNumber(applicationNumber);
        master.setServiceCode(SERVICE_CODE);
        master.setApplicantUserId(userId);
        master.setCurrentStatus("GR SUBMITTED");
        return applicationMasterRepository.save(master);
    }

    private synchronized String generateApplicationNumber() {
        int year = Year.now().getValue();
        String prefix = String.format("ML-%d-", year);

        // Get max sequence from database for current year
        Integer maxSequence = miningLeaseApplicationRepository.findMaxSequenceByPrefix(prefix);
        long nextSequence = (maxSequence == null ? 0 : maxSequence) + 1;

        return String.format("ML-%d-%06d", year, nextSequence);
    }

    private synchronized String generateDraftApplicationNumber() {
        int year = Year.now().getValue();
        String prefix = String.format("DRAFT-%d-", year);

        Integer maxSequence =
                miningLeaseApplicationRepository.findMaxDraftSequenceByPrefix(prefix);

        long nextSequence = (maxSequence == null ? 0 : maxSequence) + 1;

        return String.format("DRAFT-%d-%06d", year, nextSequence);
    }

    @Transactional
    public UserWorkloadProjection assignDirector(Long requisitionId, Long userId) {

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

        miningLeaseApplication.setFileUploadIdGr(request.getGRDocId());
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

        miningLeaseApplicationRepository.save(miningLeaseApplication);

        // =====================================================
        // 2. ASSIGN DIRECTOR
        // =====================================================
        UserWorkloadProjection assignedDirector =
                assignDirector(miningLeaseApplication.getId(), userId);

        // =====================================================
        // 3. Application master and create task for director
        // =====================================================
        ApplicationMaster master = createApplicationMaster(miningLeaseApplication.getApplicationNumber(), userId);
        createTask(master,miningLeaseApplication,"DIRECTOR",userId,assignedDirector.getUserId());

        if (assignedDirector.getEmail() != null) {
            notificationClient.sendMailToDirectorAssigned(
                    assignedDirector.getEmail(),
                    assignedDirector.getUsername(),
                    miningLeaseApplication.getApplicationNumber());
        }

        if(assignedDirector.getUserId()!= null) {
            String title = "Mining lease application has been assigned.";
            String message = "An application for mining lease has been assigned for review. Application No."+" Please login in review the Geological report.";
            String serviceId = "37";
            notificationClient.sendUserNotification(title, message, assignedDirector.getUserId(), serviceId);
        }else {
            throw new RuntimeException(ErrorCodes.DATA_TYPE_MISMATCH);
        }

        return mapper.toResponse(miningLeaseApplication);
    }
}
