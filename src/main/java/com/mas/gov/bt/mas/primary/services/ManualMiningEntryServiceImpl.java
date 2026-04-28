package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.ManualMiningEntryRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.ReviewManualEntryRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.ManualMiningEntryResponseDTO;
import com.mas.gov.bt.mas.primary.entity.ApplicationMaster;
import com.mas.gov.bt.mas.primary.entity.ManualMiningAttachmentEntity;
import com.mas.gov.bt.mas.primary.entity.ManualMiningEntryEntity;
import com.mas.gov.bt.mas.primary.entity.TaskManagement;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.mapper.ManualMiningEntryMapper;
import com.mas.gov.bt.mas.primary.repository.ApplicationMasterRepository;
import com.mas.gov.bt.mas.primary.repository.ManualMiningAttachmentRepository;
import com.mas.gov.bt.mas.primary.repository.ManualMiningEntryRepository;
import com.mas.gov.bt.mas.primary.repository.TaskManagementRepository;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ManualMiningEntryServiceImpl implements ManualMiningEntryService {

    private final ManualMiningEntryRepository entryRepository;

    private final ManualMiningAttachmentRepository attachmentRepository;

    private final ManualMiningEntryMapper mapper;

    private final ApplicationMasterRepository applicationMasterRepository;

    private final TaskManagementRepository taskManagementRepository;

    private final NotificationClient notificationClient;

    private static final String SERVICE_CODE = "MANUAL_ENTRY_SERVICE";

    @Override
    public ManualMiningEntryResponseDTO createApplication(
            ManualMiningEntryRequestDTO request,
            Long userId) {

        // 1. Map DTO → Entity
        ManualMiningEntryEntity entity = mapper.toEntity(request);

        // 2. Generate Application Number
        entity.setApplicationNo(generateApplicationNumber());
        // 3. Audit fields
        entity.setCreatedBy(userId);
        entity.setCreatedOn(LocalDateTime.now());
        entity.setStatus("SUBMITTED");

        // 4. Save parent first
        ManualMiningEntryEntity savedEntity = entryRepository.save(entity);

        UserWorkloadProjection assignedChief;

        assignedChief = assignChief();

        if (assignedChief.getUserId() == null) {
            throw new RuntimeException("No available chief for assignment");
        }

        savedEntity.setAssignedChiefId(assignedChief.getUserId());
        entryRepository.save(savedEntity);

        ApplicationMaster master = createApplicationMaster(savedEntity, userId);

        savedEntity.setApplicationMaster(master);

        createTask(
                master,
                savedEntity,
                "CHIEF",
                userId,
                assignedChief.getUserId()
        );



        // 5. Save attachments (loop)
        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {

            List<ManualMiningAttachmentEntity> attachments =
                    request.getFileIds().stream()
                            .map(fileId -> ManualMiningAttachmentEntity.builder()
                                    .fileId(fileId)
                                    .manualMiningEntryId(savedEntity.getId())
                                    .build()
                            )
                            .collect(Collectors.toList());

            attachmentRepository.saveAll(attachments);
        }

        UserWorkloadProjection userDetails =
                entryRepository.findUserDetails(userId);

        // Notify applicant
        notificationClient.sendApplicationSubmittedManualEntryNotification(
                userDetails.getEmail(),
                userDetails.getUsername(),
                savedEntity.getApplicationNo()
        );

        // Notify assigned chief
        if (assignedChief.getUserId() != null) {
            String title = "New application assigned";
            String message = "Manual Entry application assigned for review. Application No: "
                    + savedEntity.getApplicationNo();

            notificationClient.sendUserNotification(
                    title,
                    message,
                    assignedChief.getUserId(), // FIXED (see below)
                    "78"
            );
        }

        // 6. Prepare response
        ManualMiningEntryResponseDTO response = mapper.toResponse(savedEntity);

        response.setFileIds(request.getFileIds());

        return response;
    }

    @Override
    public SuccessResponse<List<ManualMiningEntryResponseDTO>> getAssignedToChief(Long userId, Pageable pageable, String search) {
        Page<ManualMiningEntryEntity> page;

        List<String> ApplicationStatus = List.of(
                "SUBMITTED",
                "ASSIGNED",
                "DRAFT",
                "PAYMENT PENDING");

        if (search == null || search.isBlank()) {

            page = entryRepository
                    .findByAssignedChiefIdAndStatusIn(userId, pageable, ApplicationStatus);

        } else {

            page = entryRepository
                    .findByAssignedChiefIdAndApplicationNoContainingIgnoreCaseAndStatusIn(
                            userId,
                            search.trim(),
                            pageable,
                            ApplicationStatus
                    );
        }

        List<Long> entryIds = page.getContent()
                .stream()
                .map(ManualMiningEntryEntity::getId)
                .toList();

        List<ManualMiningAttachmentEntity> attachments =
                attachmentRepository.findByManualMiningEntryIdIn(entryIds);

        Map<Long, List<String>> fileMap = attachments.stream()
                .collect(Collectors.groupingBy(
                        ManualMiningAttachmentEntity::getManualMiningEntryId,
                        Collectors.mapping(
                                ManualMiningAttachmentEntity::getFileId,
                                Collectors.toList()
                        )
                ));

        Page<ManualMiningEntryResponseDTO> responsePage =
                page.map(mapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Override
    public ManualMiningEntryResponseDTO reviewApplicationChief(ReviewManualEntryRequestDTO request, Long userId) {
        ManualMiningEntryEntity manualMiningEntry = entryRepository.findByApplicationNo(request.getApplicationNo())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        ApplicationMaster applicationMaster = manualMiningEntry.getApplicationMaster();

        UserWorkloadProjection applicantDetails = entryRepository.findUserDetails(manualMiningEntry.getCreatedBy());

        UserWorkloadProjection promoterDetails = entryRepository.findUserDetails(manualMiningEntry.getPromoterId());

        if (request.getStatus() != null) {
            switch (request.getStatus()) {
                case "REJECTED" -> {
                    manualMiningEntry.setStatus("REJECTED");
                    manualMiningEntry.setAssignedChiefRemarks(request.getRemarks());
                    entryRepository.save(manualMiningEntry);

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("REJECTED");
                        applicationMaster.setRejectionRemarks(request.getRemarks());
                        applicationMaster.setRejectedAt(LocalDateTime.now());
                        applicationMasterRepository.save(applicationMaster);
                    }
                    if (applicantDetails != null) {
                        notificationClient.sendRejectionManualEntryNotification(
                                applicantDetails.getEmail(),
                                applicantDetails.getUsername(),
                                manualMiningEntry.getApplicationNo(),
                                "REJECTED"
                        );

                        String title = "Your manual entry application has been rejected by Chief.";
                        String message = " Your manual entry has been rejected by Chief. Application No : " + manualMiningEntry.getApplicationNo();
                        String serviceId = "78";
                        notificationClient.sendUserNotification(title, message, applicantDetails.getUserId(), serviceId);

                    } else {
                        throw new BusinessException("Applicant details not found for notification");
                    }

                }
                case "Approved" -> {
                    manualMiningEntry.setStatus("APPROVED");
                    manualMiningEntry.setAssignedChiefRemarks(request.getRemarks());

                    entryRepository.save(manualMiningEntry);

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("APPROVED");
                        applicationMaster.setApprovedAt(LocalDateTime.now());
                        applicationMasterRepository.save(applicationMaster);
                    }

                    UserWorkloadProjection assignedDirector;

                    assignedDirector = assignDirector();

                    manualMiningEntry.setAssignedDirectorId(assignedDirector.getUserId());
                    entryRepository.save(manualMiningEntry);

                    assert applicationMaster != null;
                    createTask(
                            applicationMaster,
                            manualMiningEntry,
                            "DIRECTOR",
                            userId,
                            assignChief().getUserId()
                    );

                    if (assignedDirector.getUserId() == null) {
                        throw new RuntimeException("No available chief for assignment");
                    }

                    if (assignedDirector.getUserId() != null){
                        notificationClient.sendAssignmentManualEntryNotification(
                                assignedDirector.getEmail(),
                                applicantDetails.getUsername(),
                                manualMiningEntry.getApplicationNo(),
                                manualMiningEntry.getStatus());
                    }

                    if (applicantDetails.getUserId() != null) {
                        createTask(
                                applicationMaster,
                                manualMiningEntry,
                                "MPCD_FOCAL",
                                userId,
                                applicantDetails.getUserId()
                        );
                        createTask(
                                applicationMaster,
                                manualMiningEntry,
                                "PROMOTER",
                                userId,
                                manualMiningEntry.getPromoterId()
                        );

                        notificationClient.sendApprovalManualEntryNotification(
                                applicantDetails.getEmail(),
                                applicantDetails.getUsername(),
                                manualMiningEntry.getApplicationNo());

                        notificationClient.sendApprovalManualEntryNotification(
                                promoterDetails.getEmail(),
                                promoterDetails.getUsername(),
                                manualMiningEntry.getApplicationNo());
                    } else {
                        throw new BusinessException("Assigned GHD Chief details not found for notification");
                    }
                }
            }
        }
        return mapper.toResponse(manualMiningEntry);
    }

    @Override
    public SuccessResponse<List<ManualMiningEntryResponseDTO>> getAssignedToDirector(Long userId, Pageable pageable, String search) {
        Page<ManualMiningEntryEntity> page;

        List<String> ApplicationStatus = List.of(
                "SUBMITTED",
                "ASSIGNED",
                "DRAFT",
                "PAYMENT PENDING");

        if (search == null || search.isBlank()) {

            page = entryRepository
                    .findByAssignedDirectorIdAndStatusIn(userId, pageable, ApplicationStatus);

        } else {

            page = entryRepository
                    .findByAssignedDirectorIdAndApplicationNoContainingIgnoreCaseAndStatusIn(
                            userId,
                            search.trim(),
                            pageable,
                            ApplicationStatus
                    );
        }

        List<Long> entryIds = page.getContent()
                .stream()
                .map(ManualMiningEntryEntity::getId)
                .toList();

        List<ManualMiningAttachmentEntity> attachments =
                attachmentRepository.findByManualMiningEntryIdIn(entryIds);

        Map<Long, List<String>> fileMap = attachments.stream()
                .collect(Collectors.groupingBy(
                        ManualMiningAttachmentEntity::getManualMiningEntryId,
                        Collectors.mapping(
                                ManualMiningAttachmentEntity::getFileId,
                                Collectors.toList()
                        )
                ));

        Page<ManualMiningEntryResponseDTO> responsePage =
                page.map(mapper::toResponse);

        return SuccessResponse.fromPage(
                "Assigned applications fetched successfully",
                responsePage
        );
    }

    @Override
    public ManualMiningEntryResponseDTO reviewApplicationDirector(ReviewManualEntryRequestDTO request, Long userId) {
        ManualMiningEntryEntity manualMiningEntry = entryRepository.findByApplicationNo(request.getApplicationNo())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        ApplicationMaster applicationMaster = manualMiningEntry.getApplicationMaster();

        UserWorkloadProjection applicantDetails = entryRepository.findUserDetails(manualMiningEntry.getCreatedBy());

        UserWorkloadProjection promoterDetails = entryRepository.findUserDetails(manualMiningEntry.getPromoterId());

        if (request.getStatus() != null) {
            switch (request.getStatus()) {
                case "REJECTED" -> {
                    manualMiningEntry.setStatus("REJECTED");
                    manualMiningEntry.setAssignedChiefRemarks(request.getRemarks());
                    entryRepository.save(manualMiningEntry);

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("REJECTED");
                        applicationMaster.setRejectionRemarks(request.getRemarks());
                        applicationMaster.setRejectedAt(LocalDateTime.now());
                        applicationMasterRepository.save(applicationMaster);
                    }
                    if (applicantDetails != null) {
                        notificationClient.sendRejectionManualEntryNotification(
                                applicantDetails.getEmail(),
                                applicantDetails.getUsername(),
                                manualMiningEntry.getApplicationNo(),
                                "REJECTED"
                        );

                        String title = "Your manual entry application has been rejected by Chief.";
                        String message = " Your manual entry has been rejected by Chief. Application No : " + manualMiningEntry.getApplicationNo();
                        String serviceId = "78";
                        notificationClient.sendUserNotification(title, message, applicantDetails.getUserId(), serviceId);

                    } else {
                        throw new BusinessException("Applicant details not found for notification");
                    }

                }
                case "Work Order" -> {
                    manualMiningEntry.setStatus("WORK ORDER GENERATED");
                    manualMiningEntry.setAssignedChiefRemarks(request.getRemarks());

                    entryRepository.save(manualMiningEntry);

                    if (applicationMaster != null) {
                        applicationMaster.setCurrentStatus("WORK ORDER GENERATED");
                        applicationMaster.setApprovedAt(LocalDateTime.now());
                        applicationMasterRepository.save(applicationMaster);
                    }

                    assert applicationMaster != null;
                    createTask(
                            applicationMaster,
                            manualMiningEntry,
                            "PROMOTER",
                            userId,
                            promoterDetails.getUserId()

                    );

                    if (manualMiningEntry.getPromoterId() != null) {
                        createTask(
                                applicationMaster,
                                manualMiningEntry,
                                "PROMOTER",
                                userId,
                                manualMiningEntry.getPromoterId()
                        );

                        notificationClient.sendApprovalSampleTransportNotification(
                                promoterDetails.getEmail(),
                                promoterDetails.getUsername(),
                                manualMiningEntry.getApplicationNo());
                    } else {
                        throw new BusinessException("Assigned GHD Chief details not found for notification");
                    }
                }
            }
        }
        return mapper.toResponse(manualMiningEntry);
    }

    // Application number generator
    private synchronized String generateApplicationNumber() {

        int year = Year.now().getValue();
        String prefix = String.format("MAN-MIN-%d-", year);

        Integer maxSequence =
                entryRepository.findMaxSequenceByPrefix(prefix);

        long nextSequence = (maxSequence == null ? 0 : maxSequence) + 1;

        return String.format("MAN-MIN-%d-%06d", year, nextSequence);
    }

    private UserWorkloadProjection assignChief() {
        UserWorkloadProjection chief =
                entryRepository.findChiefManualEntry();

        if (chief == null) {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
        }
        return chief;
    }

    private UserWorkloadProjection assignDirector() {
        UserWorkloadProjection director =
                entryRepository.findDirectorManualEntry();

        if (director == null) {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND);
        }
        return director;
    }

    @Transactional
    private ApplicationMaster createApplicationMaster(ManualMiningEntryEntity geoPhysicsApplication1, Long userId) {
        ApplicationMaster master = new ApplicationMaster();
        master.setApplicationNumber(geoPhysicsApplication1.getApplicationNo());
        master.setServiceCode(SERVICE_CODE);
        master.setApplicantUserId(userId);
        master.setCurrentStatus(geoPhysicsApplication1.getStatus());
        return applicationMasterRepository.save(master);
    }

    private void createTask(ApplicationMaster master, ManualMiningEntryEntity manualMiningEntry, String role, Long assignedBy, Long assignedTo) {
        LocalDateTime now = LocalDateTime.now();

        TaskManagement task = new TaskManagement();
        task.setApplicationNumber(manualMiningEntry.getApplicationNo());
        task.setServiceCode(SERVICE_CODE);
        task.setAssignedToRole(role);
        task.setAssignedByUserId(assignedBy);
        task.setAssignedToUserId(assignedTo);
        task.setAssignedAt(now);
        task.setTaskStatus(master.getCurrentStatus());
        task.setCreatedBy(assignedBy);

        taskManagementRepository.save(task);
        log.info("Created task for role {}", role);
    }
}