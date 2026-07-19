package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.CitizenRegisterRequest;
import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.*;
import com.mas.gov.bt.mas.primary.entity.*;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.integration.CitizenRegistrationClient;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.repository.*;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import com.mas.gov.bt.mas.primary.utility.LookupHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurfaceCollectionAuctionServiceImpl implements SurfaceCollectionAuctionService {

    private final SurfaceCollectionAuctionRepository auctionRepository;

    private final SurfaceCollectionAttachmentRepository surfaceCollectionAttachmentRepository;

    private final SurfaceCollectionBankGuaranteeRepository surfaceCollectionBankGuaranteeRepository;

    private final ApplicationMasterRepository applicationMasterRepository;

    private final TaskManagementRepository taskManagementRepository;

    private final NotificationClient notificationClient;

    private final DzongkhagLookupRepository dzongkhagLookupRepository;

    private final GewogLookupRepository gewogLookupRepository;

    private final VillageLookupRepository villageLookupRepository;

    private final RegionMasterRepository regionMasterRepository;

    private final LookupHelper lookupHelper;

    private final MiningLeaseApplicationRepository miningLeaseApplicationRepository;

    private final HouseholdPermitCapConfigRefRepository capConfigRepository;

    private static final String SERVICE_CODE = "SURFACE_COLLECTION_AUCTION";

    // Real sidebar menu ids (permissions.id) per recipient role for this service — used to target
    // notification.serviceId so the sidebar dot/click-through lands on the correct menu item.
    // NOT the same thing as SERVICE_CODE above, which is an unrelated t_application_master.service_code value.
    private static final String MENU_ID_MPCD            = "72"; // "MPCD" — SURFACE_COLLECTION_AUCTION
    private static final String MENU_ID_PROMOTER         = "73"; // "PROMOTER"
    private static final String MENU_ID_MINING_DIRECTOR  = "74"; // "MINING_DIRECTOR"

    private static final int DEFAULT_TAT_DAYS = 2;

    private static final int DEFAULT_MAX_APPLICATIONS = 2;

    private final CitizenRegistrationClient citizenRegistrationClient;

    @Override
    @Transactional
    public SurfaceCollectionAuctionResponseDTO createAuction(
            SurfaceCollectionAuctionRequestDTO dto,
            Long userId
    )
    {

        Long regionId;

        DzongkhagLookup dzongkhagLookup =
                lookupHelper.fetchLookup(dto.getDzongkhagId(), dzongkhagLookupRepository, "Dzongkhag");

        regionId = dzongkhagLookup.getRegion().getId();

        GewogLookup gewogLookup =
                lookupHelper.fetchLookup(dto.getGewogId(), gewogLookupRepository, "Gewog");

        VillageLookup villageLookup =
                lookupHelper.fetchLookup(dto.getVillageId(), villageLookupRepository, "Village");

        RegionMaster regionMaster =
                lookupHelper.fetchLookup(dzongkhagLookup.getRegion().getId(), regionMasterRepository, "RegionMaster");

        // =====================================================
        // 2. ASSIGN MINING DIRECTOR
        // =====================================================
        UserWorkloadProjection assignedMD = assignMD(regionId);

        if(assignedMD == null){
            assignedMD = assignMD(9L);
        }

        try {
            SurfaceCollectionAuctionApplication entity =
                SurfaceCollectionAuctionApplication.builder()
                        .applicationNo(generateApplicationNo())
                        .location(dto.getLocation())
                        .area(dto.getArea())
                        .material(dto.getMaterial())
                        .auctionStatus("SUBMITTED")
                        .createdBy(userId)
                        .dzongkhagId(dzongkhagLookup)
                        .gewogId(gewogLookup)
                        .villageId(villageLookup)
                        .regionId(regionMaster)
                        .createdOn(LocalDateTime.now())
                        .build();


        List<SurfaceCollectionAttachment> attachments =
                dto.getAttachments()
                        .stream()
                        .map(att -> SurfaceCollectionAttachment.builder()
                                .attachmentType(att.getAttachmentType())
                                .uploadFileId(att.getFileId())
                                .auctionApplication(entity)
                                .build())
                        .toList();

        entity.setAttachments(attachments);

        entity.setAssignedMdUserId(assignedMD.getUserId());

        // =====================================================
        // 3. Application master and create task for director
        // =====================================================
        ApplicationMaster master;
        try
        {
            master = createApplicationMaster(entity.getApplicationNo(), userId);
        }catch (BusinessException ex){
            throw new BusinessException(ex.getErrorCode(), "The application master could not be created");
        }

        entity.setApplicationMaster(master);

        auctionRepository.save(entity);

        createTask(master,entity,"MINING_DIRECTOR",userId, assignedMD.getUserId());

        // Notification and Email
        if (assignedMD.getEmail() != null) {
            notificationClient.sendSurfaceCollectionAuctionMailToMDAssigned(
                    assignedMD.getEmail(),
                    assignedMD.getUsername(),
                    entity.getApplicationNo());
        }else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND,"Assigned MD Email ID not Found");
        }


        if(assignedMD.getUserId()!= null) {
            String title = "Surface Collection Auction application has been assigned.";
            String message = "An application for surface collection auction has been assigned for review. Application No. "+ entity.getApplicationNo();
            String serviceId = MENU_ID_MINING_DIRECTOR;
            notificationClient.sendUserNotification(title, message, assignedMD.getUserId(), serviceId, "STAFF", true, entity.getApplicationNo());
        }else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND,"Assigned MD user ID not Found");
        }

        return mapToResponse(entity);

        } catch (DataIntegrityViolationException ex) {

            throw new BusinessException(
                    ErrorCodes.DATA_INTEGRITY_VIOLATION,
                    "Failed to save application due to database constraint issue."
            );

        } catch (Exception ex) {

            throw new BusinessException(
                    ErrorCodes.INTERNAL_SERVER_ERROR,
                    "Failed to create auction application."
            );
        }
    }

    /**
     * Pre-flight cap check — called when the applicant clicks to open the
     * application form, before they fill anything in. Also reused by
     * submitGR as the authoritative server-side guard.
     */
    public CapCheckResponse checkCap(Long userId) {
        String[] grouping = resolveGroupingKey(userId);
        int maxAllowed = getMaxAllowedForService(SERVICE_CODE, grouping[2]);
        Integer total = auctionRepository.countMiningLeasesForGrouping(grouping[0], grouping[1]);
        int current = total != null ? total : 0;
        boolean allowed = current < maxAllowed;
        String message = allowed ? null
                : "Only " + maxAllowed + " Surface collection auction application(s) are permitted per " +
                  ("CID".equals(grouping[0]) ? "applicant" : "household/entity") + ".";
        return new CapCheckResponse(allowed, current, maxAllowed, message);
    }

    private int getMaxAllowedForService(String serviceType, String registrationType) {
        String rt = isNotBlank(registrationType) ? registrationType : "INDIVIDUAL";
        return capConfigRepository.findByServiceTypeAndRegistrationType(serviceType, rt)
                .map(c -> c.getMaxAllowed() != null ? c.getMaxAllowed() : DEFAULT_MAX_APPLICATIONS)
                .orElse(DEFAULT_MAX_APPLICATIONS);
    }

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

    private void createTask(ApplicationMaster master, SurfaceCollectionAuctionApplication application, String role, Long userId, Long directorId) {
        LocalDateTime now = LocalDateTime.now();

        TaskManagement task = new TaskManagement();
        task.setApplicationNumber(application.getApplicationNo());
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
        master.setSubmittedOn(LocalDateTime.now());
        return applicationMasterRepository.save(master);
    }

    @Transactional
    public UserWorkloadProjection assignMD(Long id) {

        UserWorkloadProjection miningDirector =
                auctionRepository.findMDSurfaceCollection(id);

        if (miningDirector == null && id == 9L) {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "Mining Engineer with required permission, region and role not found.");
        }
        return miningDirector;
    }

    @Override
    public SurfaceCollectionAuctionResponseDTO submitForEC(Long auctionId, String fileECid) {

        SurfaceCollectionAuctionApplication entity = getAuction(auctionId);

        entity.setFileECid(fileECid);
        entity.setSubmittedForEc(true);
        entity.setEcStatus("APPROVED");
        entity.setAuctionStatus("EC_APPROVED");

        auctionRepository.save(entity);

        return mapToResponse(entity);
    }

    @Override
    public SurfaceCollectionAuctionResponseDTO submitForFC(Long auctionId) {

        SurfaceCollectionAuctionApplication entity = getAuction(auctionId);

        entity.setSubmittedForFc(true);
        entity.setFcStatus("PENDING");

        auctionRepository.save(entity);

        return mapToResponse(entity);
    }

    @Override
    public SurfaceCollectionAuctionResponseDTO updateEcApproval(Long auctionId, SurfaceCollectionAuctionECRequest fileECid, Long userId) {

        SurfaceCollectionAuctionApplication entity = getAuction(auctionId);

        entity.setFileECid(fileECid.getEcFileId());
        entity.setEcNumber(fileECid.getEcNumber());
        entity.setEcValidUpto(fileECid.getEcValidUpto());
        entity.setMdRemarks(fileECid.getRemarks());
        entity.setEcStatus("EC_APPROVED");
        entity.setEcApprovedOn(LocalDateTime.now());
        entity.setAuctionStatus("EC_APPROVED");

        ApplicationMaster applicationMaster = entity.getApplicationMaster();
        applicationMaster.setCurrentStatus("EC_APPROVED");

        applicationMasterRepository.save(applicationMaster);

        createTask(entity.getApplicationMaster(), entity, "MPCD", userId, entity.getCreatedBy());

        auctionRepository.save(entity);

        if(entity.getCreatedBy()!= null) {
            String title = "EC for Surface Collection Auction application has been approved.";
            String message = "EC for surface collection auction has been APPROVED. Application No. "+ entity.getApplicationNo();
            String serviceId = MENU_ID_MPCD;
            notificationClient.sendUserNotification(title, message, entity.getCreatedBy(), serviceId, "STAFF", true, entity.getApplicationNo());
        }else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "MPCD User details not present for notification.");
        }

        return mapToResponse(entity);
    }

    @Override
    public SurfaceCollectionAuctionResponseDTO updateFcApproval(Long auctionId) {

        SurfaceCollectionAuctionApplication entity = getAuction(auctionId);

        entity.setFcStatus("APPROVED");
        entity.setFcApprovedOn(LocalDateTime.now());

        auctionRepository.save(entity);

        return mapToResponse(entity);
    }


    @Override
    public SurfaceCollectionAuctionResponseDTO saveBidWinner(
            Long auctionId,
            BidWinnerRequestDTO dto
    ) {

        SurfaceCollectionAuctionApplication entity = getAuction(auctionId);

        Long regionId;

        DzongkhagLookup dzongkhagLookup =
                lookupHelper.fetchLookup(dto.getDzongkhagId(), dzongkhagLookupRepository, "Dzongkhag");

        regionId = dzongkhagLookup.getRegion().getId();

        GewogLookup gewogLookup =
                lookupHelper.fetchLookup(dto.getGewogId(), gewogLookupRepository, "Gewog");

        VillageLookup villageLookup =
                lookupHelper.fetchLookup(dto.getVillageId(), villageLookupRepository, "Village");

        RegionMaster regionMaster =
                lookupHelper.fetchLookup(dzongkhagLookup.getRegion().getId(), regionMasterRepository, "RegionMaster");

        CitizenRegisterRequest registerRequest = toCitizenRegisterRequest(dto);
        if (!isValidRegisterRequest(registerRequest)) {
            throw new BusinessException(ErrorCodes.DATA_INTEGRITY_VIOLATION,
                    "Insufficient bid winner details to create a user account.");
        }

        citizenRegistrationClient.registerCitizen(registerRequest);

        UserWorkloadProjection assignedUser = auctionRepository.findCitizenByEmail(dto.getEmailAddress());
        if (assignedUser == null || assignedUser.getUserId() == null) {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "Bid winner details not found.");
        }

        SurfaceCollectionBidWinner winner =
                SurfaceCollectionBidWinner.builder()
                        .bidWinnerName(dto.getBidWinnerName())
                        .contactNumber(dto.getContactNumber())
                        .emailAddress(dto.getEmailAddress())
                        .licenseNumber(dto.getLicenseNumber())
                        .companyType(dto.getCompanyType())
                        .companyRegistrationNumber(dto.getCompanyRegistrationNumber())
                        .dzongkhagId(dzongkhagLookup)
                        .gewogId(gewogLookup)
                        .villageId(villageLookup)
                        .regionId(regionMaster)
                        .promoterId(assignedUser.getUserId())
                        .cidNumber(dto.getCidNumber())
                        .bidAmount(dto.getBidAmount())
                        .auctionApplication(entity)
                        .build();

        entity.setBidWinner(winner);
        entity.setAuctionCompleted(true);
        entity.setAuctionStatus("AUCTION_COMPLETED");

        // MPCD will set Site name while adding bid winner
        entity.setSiteName(dto.getSiteName());
        auctionRepository.save(entity);

        // =====================================================
        // Application master and create task for director
        // =====================================================
        ApplicationMaster applicationMaster = entity.getApplicationMaster();
        applicationMaster.setCurrentStatus("AUCTION_COMPLETED");

        applicationMasterRepository.save(applicationMaster);

        createTask(entity.getApplicationMaster(), entity, "PROMOTER", entity.getCreatedBy(), entity.getCreatedBy());

        // Notification and Email
        if (assignedUser.getEmail() != null) {
            notificationClient.sendSurfaceCollectionAuctionWinnerEmail(
                    assignedUser.getEmail(),
                    assignedUser.getUsername(),
                    entity.getApplicationNo());
        } else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "Bid winner Email address not found.");
        }

        if (assignedUser.getEmail() != null) {
            String title = "Surface Collection Auction Winner";
            String message = "Surface collection auction application has been won by you. Application No. " + entity.getApplicationNo();
            String serviceId = MENU_ID_PROMOTER;
            notificationClient.sendUserNotification(title, message, assignedUser.getUserId(), serviceId, "CITIZEN", true, entity.getApplicationNo());
        } else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "Bid winner user ID not present for notification.");
        }

        return mapToResponse(entity);
    }

    @Override
    public SurfaceCollectionAuctionResponseDTO requestBG(
            Long auctionId,
            BGRequestDTO dto
    ) {

        SurfaceCollectionAuctionApplication entity = getAuction(auctionId);

        entity.setBgRequested(true);
        entity.setBgInstruction(dto.getBgInstruction());
        entity.setAuctionStatus("BG_PENDING");

        auctionRepository.save(entity);

        /**
         * Send notification/email here
         */

        return mapToResponse(entity);
    }

    @Override
    public SurfaceCollectionAuctionResponseDTO generatePermit(Long auctionId) {

        SurfaceCollectionAuctionApplication entity = getAuction(auctionId);

        entity.setPermitGenerated(true);
        entity.setAuctionStatus("PERMIT_GENERATED");

        auctionRepository.save(entity);

        return mapToResponse(entity);
    }

    private SurfaceCollectionAuctionApplication getAuction(Long id) {
        return auctionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.RECORD_NOT_FOUND,
                        "Auction data not found. Auction Id is missing"
                ));
    }

    private String generateApplicationNo() {
        int year = Year.now().getValue();
        String prefix = String.format("SCA-%d-", year);

        // Get max sequence from database for current year
        Integer maxSequence = auctionRepository.findMaxSequenceByPrefix(prefix);
        long nextSequence = (maxSequence == null ? 0 : maxSequence) + 1;

        return String.format("SCA-%d-%06d", year, nextSequence);
    }

    private SurfaceCollectionAuctionResponseDTO mapToResponse(
            SurfaceCollectionAuctionApplication entity
    ) {
        return SurfaceCollectionAuctionResponseDTO.builder()
                .id(entity.getId())
                .siteName(entity.getSiteName())
                .applicationNo(entity.getApplicationNo())
                .location(entity.getLocation())
                .area(entity.getArea())
                .material(entity.getMaterial())
                .dzongkhagName(entity.getDzongkhagId().getDzongkhagName())
                .gewogName(entity.getGewogId().getGewogName())
                .villageName(entity.getVillageId().getVillageName())
                .ecFileId(entity.getFileECid())
                .ecNumber(entity.getEcNumber())
                .ecValidUpto(entity.getEcValidUpto())
                .ecStatus(entity.getEcStatus())
                .fcStatus(entity.getFcStatus())
                .auctionStatus(entity.getAuctionStatus())
                .submittedForEc(entity.getSubmittedForEc())
                .submittedForFc(entity.getSubmittedForFc())
                .bgRequested(entity.getBgRequested())
                .permitGenerated(entity.getPermitGenerated())
                .bidWinner(mapToResponseBidWinner(entity.getBidWinner()))
                .createdOn(entity.getCreatedOn())
                .build();
    }

    private BidWinnerResponseDTO mapToResponseBidWinner(SurfaceCollectionBidWinner bidWinner) {
        if  (bidWinner == null) {
            return BidWinnerResponseDTO.builder().build();
        }
        return BidWinnerResponseDTO.builder()
                .id(bidWinner.getId())
                .bidWinnerName(bidWinner.getBidWinnerName())
                .emailAddress(bidWinner.getEmailAddress())
                .contactNumber(bidWinner.getContactNumber())
                .cidNumber(bidWinner.getCidNumber())
                .licenseNumber(bidWinner.getLicenseNumber())
                .companyType(bidWinner.getCompanyType())
                .companyRegistrationNumber(bidWinner.getCompanyRegistrationNumber())
                .dzongkhagId(bidWinner.getDzongkhagId().getDzongkhagName())
                .gewogId(bidWinner.getGewogId().getGewogName())
                .villageId(bidWinner.getVillageId().getVillageName())
                .regionId(bidWinner.getRegionId().getRegionName())
                .bidAmount(bidWinner.getBidAmount())
                .build();
    }

    @Override
    public Page<SurfaceCollectionAuctionListResponseDTO> getAllApplications(
            String search,
            Pageable pageable) {

        Page<SurfaceCollectionAuctionApplication> page;

        if (search == null || search.isBlank()) {

            page = auctionRepository.findAll(pageable);

        } else {

            page = auctionRepository
                    .findByApplicationNoContainingIgnoreCaseOrLocationContainingIgnoreCase(
                            search,
                            search,
                            pageable
                    );
        }

        return page.map(this::mapListResponse);
    }

    @Override
    public List<SurfaceCollectionAttachmentResponseDTO>
    getAttachmentsByAuctionId(Long auctionId) {

        List<SurfaceCollectionAttachment> attachments =
                surfaceCollectionAttachmentRepository.findByAuctionApplicationId(auctionId);

        return attachments.stream()
                .map(this::mapAttachment)
                .toList();
    }

    @Override
    public Page<SurfaceCollectionAuctionListResponseDTO> getMyApplications(String search, Pageable pageable, Long userId) {
        Page<SurfaceCollectionAuctionApplication> page;

        List<String> archivedStatuses = List.of("SUBMITTED", "EC_APPROVED", "AUCTION_COMPLETED","BG_PENDING", "BG SUBMITTED", "BG RESUBMIT", "BG RESUBMITTED");

        if (search == null || search.isBlank()) {

            page = auctionRepository.findByCreatedByAndAuctionStatusIn(
                    userId,
                    archivedStatuses,
                    pageable
            );

        } else {

            page = auctionRepository
                    .findByCreatedByAndApplicationNoContainingIgnoreCaseOrLocationContainingIgnoreCaseAndAuctionStatusIn(
                            userId,
                            search,
                            search,
                            archivedStatuses,
                            pageable
                    );
        }

        return page.map(this::mapListResponse);
    }

    @Override
    public List<BGResponseDTO> getBGAttachmentsByAuctionId(Long auctionId) {

        Optional<SurfaceCollectionBankGuarantee> attachments =surfaceCollectionBankGuaranteeRepository
                .findByAuctionApplicationId(auctionId);

        if (attachments.isEmpty()) {
             throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "No surface collection bank guarantee found");
        }

        return attachments.stream()
                .map(this::map)
                .toList();
    }

    @Override
    public Page<SurfaceCollectionAuctionListResponseDTO> getMyArchive(String search, Pageable pageable, Long userId) {
        Page<SurfaceCollectionAuctionApplication> page;

        List<String> archivedStatuses = List.of("APPROVED");

        if (search == null || search.isBlank()) {

            page = auctionRepository.findByCreatedByAndAuctionStatusIn(userId,archivedStatuses,pageable);

        } else {

            page = auctionRepository
                    .findByCreatedByAndApplicationNoContainingIgnoreCaseOrLocationContainingIgnoreCaseAndAuctionStatusIn(
                            userId,
                            search,
                            search,
                            archivedStatuses,
                            pageable
                    );
        }

        return page.map(this::mapListResponse);
    }

    private SurfaceCollectionAttachmentResponseDTO mapAttachment(
            SurfaceCollectionAttachment attachment
    ) {

        return SurfaceCollectionAttachmentResponseDTO.builder()
                .id(attachment.getId())
                .attachmentType(attachment.getAttachmentType())
                .uploadFileId(attachment.getUploadFileId())
                .build();
    }

    private SurfaceCollectionAuctionListResponseDTO mapListResponse(
            SurfaceCollectionAuctionApplication entity
    ) {

        return SurfaceCollectionAuctionListResponseDTO.builder()
                .id(entity.getId())
                .siteName(entity.getSiteName())
                .applicationNo(entity.getApplicationNo())
                .location(entity.getLocation())
                .area(entity.getArea())
                .material(entity.getMaterial())
                .dzongkhagName(entity.getDzongkhagId().getDzongkhagName())
                .gewogName(entity.getGewogId().getGewogName())
                .villageName(entity.getVillageId().getVillageName())
                .ecFileId(entity.getFileECid())
                .ecNumber(entity.getEcNumber())
                .ecValidUpto(entity.getEcValidUpto())
                .ecStatus(entity.getEcStatus())
                .fcStatus(entity.getFcStatus())
                .auctionStatus(entity.getAuctionStatus())
                .bgRequested(entity.getBgRequested())
                .bgInstruction(entity.getBgInstruction())
                .permitGenerated(entity.getPermitGenerated())
                .createdOn(entity.getCreatedOn())
                .build();
    }

    private BGResponseDTO map(SurfaceCollectionBankGuarantee bg) {
        return BGResponseDTO.builder()
                .id(bg.getId())
                .auctionId(bg.getAuctionApplication().getId())
                .bgFileId(bg.getBgFileId())
                .bgInstruction(bg.getBgInstruction())
                .status(bg.getStatus())
                .submittedOn(bg.getSubmittedOn())
                .resubmittedOn(bg.getResubmittedOn())
                .build();
    }

    private CitizenRegisterRequest toCitizenRegisterRequest(BidWinnerRequestDTO dto) {
        CitizenRegisterRequest request = new CitizenRegisterRequest();
        request.setEmail(dto.getEmailAddress());
        request.setPhoneNumber(dto.getContactNumber());

        if (isNotBlank(dto.getCompanyRegistrationNumber())) {
            request.setRegistrationType("REGISTERED_COMPANY");
            request.setCompanyRegistrationNumber(dto.getCompanyRegistrationNumber());
            request.setCompanyName(dto.getBidWinnerName());
            request.setCompanyType(dto.getCompanyType());
        } else if (isNotBlank(dto.getLicenseNumber())) {
            request.setRegistrationType("BUSINESS_LICENSE");
            request.setLicenseNumber(dto.getLicenseNumber());
            request.setBusinessName(dto.getBidWinnerName());
            request.setBusinessOwner(dto.getBidWinnerName());
        } else {
            request.setRegistrationType("INDIVIDUAL");
            request.setCid(dto.getCidNumber());
            request.setName(dto.getBidWinnerName());
            request.setMobileNumber(dto.getContactNumber());
        }
        return request;
    }

    private boolean isValidRegisterRequest(CitizenRegisterRequest r) {
        return switch (r.getRegistrationType()) {
            case "INDIVIDUAL" -> isNotBlank(r.getCid()) && isNotBlank(r.getName());
            case "BUSINESS_LICENSE" -> isNotBlank(r.getLicenseNumber()) && isNotBlank(r.getBusinessName()) && isNotBlank(r.getBusinessOwner());
            case "REGISTERED_COMPANY" -> isNotBlank(r.getCompanyRegistrationNumber()) && isNotBlank(r.getCompanyName()) && isNotBlank(r.getCompanyType());
            default -> false;
        };
    }

}