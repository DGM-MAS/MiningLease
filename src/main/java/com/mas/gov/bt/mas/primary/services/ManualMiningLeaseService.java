package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.QuarryLeaseApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.QuarryLeaseResponse;
import com.mas.gov.bt.mas.primary.dto.request.MiningLeaseApplicationRequest;
import com.mas.gov.bt.mas.primary.dto.response.ApplicationListResponse;
import com.mas.gov.bt.mas.primary.dto.response.MiningLeaseResponse;
import com.mas.gov.bt.mas.primary.entity.*;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.exception.ResourceNotFoundException;
import com.mas.gov.bt.mas.primary.exception.UnauthorizedOperationException;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.mapper.MiningLeaseMapper;
import com.mas.gov.bt.mas.primary.mapper.QuarryLeaseMapper;
import com.mas.gov.bt.mas.primary.repository.*;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
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
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class ManualMiningLeaseService {

    private final MiningLeaseApplicationRepository miningLeaseApplicationRepository;

    private final QuarryLeaseApplicationRepository applicationRepository;

    private final DzongkhagLookupRepository dzongkhagLookupRepository;

    private final GewogLookupRepository gewogLookupRepository;

    private final VillageLookupRepository villageLookupRepository;

    private final ApplicationMasterRepository applicationMasterRepository;

    private final TaskManagementRepository taskManagementRepository;

    private final MiningLeaseMapper mapper;

    private final QuarryLeaseMapper mapperQuarry;

    private final NotificationClient notificationClient;

    private static final String SERVICE_CODE = "MINING_LEASE_MANUAL_ENTRY";

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

//        // ============================================================
//        // 0. Checking if the user has not more than two mining lease
//        // ============================================================
//        String houseHoldNumber = miningLeaseApplicationRepository.findUserHouseHoldNumber(userId);
//        Integer miningLeaseCount = miningLeaseApplicationRepository.findLeaseCountForMining(houseHoldNumber);

//        if(miningLeaseCount <=1){
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
                if(Objects.equals(request.getApplicationType(), "Submitted")) {
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
                application.setCurrentStatus("MINING LEASE APPROVED");
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

            // =====================================================
            // 6. SUBMIT FLOW
            // =====================================================

            application.setSubmittedAt(now);
            application.setApplicationNumber(applicationNumber);
            // =====================================================
            // 6. UPDATE APPLICATION MASTER
            // =====================================================
            ApplicationMaster master = application.getApplicationMaster();

            master.setApplicationNumber(applicationNumber);
            master.setCurrentStatus(application.getCurrentStatus());
            master.setServiceCode(SERVICE_CODE);
            master.setSubmittedAt(now);
            master.setApprovedAt(now);
            applicationMasterRepository.save(master);

            createTask(master,application,userId);

            miningLeaseApplicationRepository.save(application);
            // =====================================================
            // 7. NOTIFICATIONS
            // =====================================================
            MiningLeaseApplication app = findApplicationById(application.getId());
            ApplicationMaster master1 = app.getApplicationMaster();

            if (app.getApplicantEmail() != null) {
                notificationClient.sendApprovalNotification(
                        app.getApplicantEmail(),
                        app.getApplicantName(),
                        app.getApplicationNumber());
            }
            // =====================================================
            // 9. FINAL SAVE (ONLY ONCE)
            // =====================================================
            application.setIsManualEntry("TRUE");
            application.setApprovedAt(now);
            application.setCreatedBy(userId);
            application.setLeaseStartDate(LocalDate.from(now.plusYears(Long.parseLong(request.getProposedLeasePeriod()))));
            miningLeaseApplicationRepository.save(application);

            log.info("Application submitted successfully: {}",
                    application.getApplicationNumber());

            return mapper.toResponse(application);
//        }else {
//            throw new BusinessException("The number of mining lease count has exceeded");
//        }

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
    private synchronized String generateApplicationNumber() {
        int year = Year.now().getValue();
        String prefix = String.format("ML-%d-", year);

        // Get max sequence from database for current year
        Integer maxSequence = miningLeaseApplicationRepository.findMaxSequenceByPrefix(prefix);
        long nextSequence = (maxSequence == null ? 0 : maxSequence) + 1;

        return String.format("ML-%d-%06d", year, nextSequence);
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
    private void createTask(ApplicationMaster master, MiningLeaseApplication application, Long userId) {

        try {
            LocalDateTime now = LocalDateTime.now();

            TaskManagement task = new TaskManagement();
            task.setApplicationNumber(application.getApplicationNumber());
            task.setServiceCode(SERVICE_CODE);
            task.setAssignedByUserId(userId);
            task.setTaskStatus(master.getCurrentStatus());

            task.setDeadlineDate(now.plusDays('2'));
            task.setCreatedBy(userId);

            TaskManagement savedTask = taskManagementRepository.save(task);

        } catch (Exception ex) {
            log.error("Error creating task for director", ex);
            throw new BusinessException(
                    ErrorCodes.DATABASE_CONNECTION_FAILED,
                    "Failed to create task assignment",
                    ex
            );
        }
    }

    private MiningLeaseApplication findApplicationById(Long id) {
        return miningLeaseApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));
    }

    public QuarryLeaseResponse createApplicationQuarry(@Valid QuarryLeaseApplicationRequest request, Long userId) {
        log.info("Creating/updating quarry lease application for user: {}", userId);

        boolean isDraft = "Draft".equalsIgnoreCase(request.getApplicationType());
        LocalDateTime now = LocalDateTime.now();

        // =====================================================
        // 1. FETCH EXISTING APPLICATION OR CREATE NEW
        // =====================================================
        QuarryLeaseApplication application =
                applicationRepository.findByApplicationNumber(request.getApplicationNo())
                        .orElse(new QuarryLeaseApplication());

        boolean isNew = application.getId() == null;

        // =====================================================
        // 2. GENERATE APPLICATION NUMBER (ONLY IF NEW)
        // =====================================================
        String applicationNumber;

        if (request.getApplicationNo() == null) {
            applicationNumber = isDraft
                    ? generateDraftApplicationNumber()
                    : generateApplicationNumberQuarry();
        } else {
            if(Objects.equals(request.getApplicationType(), "Submitted")) {
                applicationNumber = generateApplicationNumberQuarry();
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
            application.setCurrentStatus("QUARRY LEASE APPROVED");
            application.setIsActive(true);
        }

        // =====================================================
        // 4. UPDATE ENTITY FROM REQUEST (SAFE UPDATE)
        // =====================================================
        mapperQuarry.updateEntityFromRequest(request, application);

        if (request.getDzongkhag() != null && !request.getDzongkhag().isEmpty()) {
            DzongkhagLookup dzongkhag = dzongkhagLookupRepository
                    .findById(request.getDzongkhag())
                    .orElseThrow(() -> new RuntimeException("Invalid Dzongkhag ID"));

            application.setDzongkhag(dzongkhag);
        }

        if (request.getGewog() != null && !request.getGewog().isEmpty()) {
            GewogLookup gewog = gewogLookupRepository
                    .findByGewogSerialNo(Integer.valueOf(request.getGewog()))
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

        // =====================================================
        // 6. SUBMIT FLOW
        // =====================================================

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
        // 10. FINAL SAVE (ONLY ONCE)
        // =====================================================
        QuarryLeaseApplication app = findApplicationByIdQuarry(application.getId());
        ApplicationMaster master1 = app.getApplicationMaster();

        applicationRepository.save(application);

        log.info("Application submitted successfully: {}",
                application.getApplicationNumber());

        return mapperQuarry.toResponse(application);
    }

    private synchronized String generateApplicationNumberQuarry() {
        int year = Year.now().getValue();
        String prefix = String.format("QL-%d-", year);

        // Get max sequence from database for current year
        Integer maxSequence = applicationRepository.findMaxSequenceByPrefix(prefix);
        long nextSequence = (maxSequence == null ? 0 : maxSequence) + 1;

        return String.format("QL-%d-%06d", year, nextSequence);
    }

    private QuarryLeaseApplication findApplicationByIdQuarry(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public Page<ApplicationListResponse> getArchivedApplications(Pageable pageable, String search, Long userId) {
        List<String> archivedStatuses = List.of("MINING LEASE APPROVED");
        Page<MiningLeaseApplication> applications;

        if (search == null || search.isBlank()) {
            applications = miningLeaseApplicationRepository.findByApplicantUserIdAndStatusIn(
                    userId,
                    archivedStatuses,
                    pageable);
        }
        else {

            applications = miningLeaseApplicationRepository.findArchivedAssignedToUserAndSearch(
                    userId,
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
        List<String> archivedStatuses = List.of("MINING LEASE APPROVED");
        Page<MiningLeaseApplication> applications ;

        if (search == null || search.isBlank()) {
            applications =  miningLeaseApplicationRepository.findByApplicantUserIdAndStatusIn(userId, archivedStatuses, pageable);
        } else {
            applications = miningLeaseApplicationRepository.findByApplicantUserIdAndSearch(userId, archivedStatuses, search.trim(), pageable);
        }

        return applications.map(mapper::toListResponse);
    }
}
