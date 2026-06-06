package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.ManualMiningEntryRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.ManualMiningEntryResponseDTO;
import com.mas.gov.bt.mas.primary.entity.ApplicationMaster;
import com.mas.gov.bt.mas.primary.entity.ManualMiningAttachmentEntity;
import com.mas.gov.bt.mas.primary.entity.ManualMiningEntryEntity;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.mapper.ManualMiningEntryMapper;
import com.mas.gov.bt.mas.primary.repository.ApplicationMasterRepository;
import com.mas.gov.bt.mas.primary.repository.ManualMiningAttachmentRepository;
import com.mas.gov.bt.mas.primary.repository.ManualMiningEntryRepository;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Collections;
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
    private final NotificationClient notificationClient;
    private final ManualEntryValidator validator;

    private static final String SERVICE_CODE = "MANUAL_ENTRY_SERVICE";

    @Override
    @Transactional
    public ManualMiningEntryResponseDTO createApplication(ManualMiningEntryRequestDTO request, Long userId) {

        validator.validate(request);

        ManualMiningEntryEntity entity = mapper.toEntity(request);
        entity.setCreatedBy(userId);
        entity.setIsManualEntry(true);

        String prefix = resolvePrefix(request.getActivityType());
        entity.setApplicationNo(generateApplicationNumber(prefix));
        entity.setStatus(resolveFinalStatus(request.getActivityType()));

        ManualMiningEntryEntity saved = entryRepository.save(entity);

        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            List<ManualMiningAttachmentEntity> attachments = request.getFileIds().stream()
                    .map(fileId -> ManualMiningAttachmentEntity.builder()
                            .fileId(fileId)
                            .manualMiningEntryId(saved.getId())
                            .build())
                    .toList();
            attachmentRepository.saveAll(attachments);
        }

        ApplicationMaster master = createApplicationMaster(saved, userId);
        saved.setApplicationMaster(master);
        entryRepository.save(saved);

        notifyPromoter(saved);

        ManualMiningEntryResponseDTO response = mapper.toResponse(saved);
        response.setFileIds(request.getFileIds());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public SuccessResponse<List<ManualMiningEntryResponseDTO>> getApplications(Long userId, Pageable pageable, String search) {

        Page<ManualMiningEntryEntity> page;

        if (search == null || search.isBlank()) {
            page = entryRepository.findByCreatedByAndStatusIn(userId, allStatuses(), pageable);
        } else {
            page = entryRepository.findByCreatedByAndSearch(userId, search.trim(), pageable);
        }

        List<Long> entryIds = page.getContent().stream()
                .map(ManualMiningEntryEntity::getId)
                .toList();

        Map<Long, List<String>> fileMap = buildFileMap(entryIds);

        Page<ManualMiningEntryResponseDTO> responsePage = page.map(e -> {
            ManualMiningEntryResponseDTO dto = mapper.toResponse(e);
            dto.setFileIds(fileMap.getOrDefault(e.getId(), Collections.emptyList()));
            return dto;
        });

        return SuccessResponse.fromPage("Applications fetched successfully", responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public SuccessResponse<List<ManualMiningEntryResponseDTO>> getAllApplications(Pageable pageable, String search) {
        Page<ManualMiningEntryEntity> page = entryRepository.findAllBySearch(
                (search == null || search.isBlank()) ? null : search.trim(),
                pageable
        );

        Map<Long, List<String>> fileMap = buildFileMap(
                page.getContent().stream().map(ManualMiningEntryEntity::getId).toList()
        );

        Page<ManualMiningEntryResponseDTO> responsePage = page.map(e -> {
            ManualMiningEntryResponseDTO dto = mapper.toResponse(e);
            dto.setFileIds(fileMap.getOrDefault(e.getId(), Collections.emptyList()));
            return dto;
        });

        return SuccessResponse.fromPage("Applications fetched successfully", responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public ManualMiningEntryResponseDTO getApplicationByNo(String applicationNo) {
        ManualMiningEntryEntity entity = entryRepository.findByApplicationNo(applicationNo)
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        List<ManualMiningAttachmentEntity> attachments =
                attachmentRepository.findByManualMiningEntryIdIn(List.of(entity.getId()));

        ManualMiningEntryResponseDTO response = mapper.toResponse(entity);
        response.setFileIds(attachments.stream()
                .map(ManualMiningAttachmentEntity::getFileId)
                .toList());
        return response;
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private String resolveFinalStatus(String activityType) {
        if (activityType == null) return "APPROVED";
        return switch (activityType.toUpperCase()) {
            case "MINING_LEASE"         -> "MINING LEASE APPROVED";
            case "QUARRY_LEASE"         -> "QUARRY LEASE APPROVED";
            case "SURFACE_COLLECTION"   -> "SC PERMIT APPROVED";
            case "STOCK_LIFTING"        -> "STOCK LIFTING APPROVED";
            default                     -> "APPROVED";
        };
    }

    private String resolvePrefix(String activityType) {
        int year = Year.now().getValue();
        if (activityType == null) return String.format("MAN-ENTRY-%d-", year);
        return switch (activityType.toUpperCase()) {
            case "MINING_LEASE"         -> String.format("MAN-ML-%d-", year);
            case "QUARRY_LEASE"         -> String.format("MAN-QL-%d-", year);
            case "SURFACE_COLLECTION"   -> String.format("MAN-SC-%d-", year);
            case "STOCK_LIFTING"        -> String.format("MAN-SL-%d-", year);
            default                     -> String.format("MAN-ENTRY-%d-", year);
        };
    }

    private synchronized String generateApplicationNumber(String prefix) {
        int startIndex = prefix.length() + 1;
        Integer maxSeq = entryRepository.findMaxSequenceByPrefixAndStartIndex(prefix, startIndex);
        long next = (maxSeq == null ? 0L : maxSeq) + 1L;
        return prefix + String.format("%06d", next);
    }

    private ApplicationMaster createApplicationMaster(ManualMiningEntryEntity entity, Long userId) {
        ApplicationMaster master = new ApplicationMaster();
        master.setApplicationNumber(entity.getApplicationNo());
        master.setServiceCode(SERVICE_CODE);
        master.setApplicantUserId(userId);
        master.setCurrentStatus(entity.getStatus());
        master.setSubmittedAt(LocalDateTime.now());
        master.setApprovedAt(LocalDateTime.now());
        return applicationMasterRepository.save(master);
    }

    private void notifyPromoter(ManualMiningEntryEntity entity) {
        if (entity.getPromoterId() == null) return;
        UserWorkloadProjection promoter = entryRepository.findUserDetails(entity.getPromoterId());
        if (promoter == null || promoter.getEmail() == null) return;
        notificationClient.sendApprovalManualEntryNotification(
                promoter.getEmail(),
                promoter.getUsername(),
                entity.getApplicationNo()
        );
    }

    private Map<Long, List<String>> buildFileMap(List<Long> entryIds) {
        if (entryIds.isEmpty()) return Collections.emptyMap();
        return attachmentRepository.findByManualMiningEntryIdIn(entryIds).stream()
                .collect(Collectors.groupingBy(
                        ManualMiningAttachmentEntity::getManualMiningEntryId,
                        Collectors.mapping(ManualMiningAttachmentEntity::getFileId, Collectors.toList())
                ));
    }

    private List<String> allStatuses() {
        return List.of(
                "MINING LEASE APPROVED",
                "QUARRY LEASE APPROVED",
                "SC PERMIT APPROVED",
                "STOCK LIFTING APPROVED",
                "APPROVED"
        );
    }
}
