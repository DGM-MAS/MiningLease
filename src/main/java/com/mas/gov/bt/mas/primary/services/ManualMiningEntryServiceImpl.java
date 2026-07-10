package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.ManualMiningEntryRequestDTO;
import com.mas.gov.bt.mas.primary.dto.response.ManualMiningEntryResponseDTO;
import com.mas.gov.bt.mas.primary.entity.*;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.repository.*;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import com.mas.gov.bt.mas.primary.utility.SuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ManualMiningEntryServiceImpl implements ManualMiningEntryService {

    private final MiningLeaseApplicationRepository mlRepo;
    private final QuarryLeaseApplicationRepository qlRepo;
    private final SurfaceCollectionPermitRepository scRepo;
    private final ManualMiningAttachmentRepository attachmentRepository;
    private final ApplicationMasterRepository applicationMasterRepository;
    private final NotificationClient notificationClient;
    private final ApplicantAccountProvisioningService applicantAccountProvisioningService;
    private final ManualEntryValidator validator;
    private final ManualMiningEntryRepository entryRepository; // user lookup only
    private final DzongkhagLookupRepository dzongkhagLookupRepository;
    private final GewogLookupRepository gewogLookupRepository;
    private final VillageLookupRepository villageLookupRepository;

    @Autowired
    private StockLiftingRepository stockLiftingRepository;

    private static final String SERVICE_CODE = "MANUAL_ENTRY_SERVICE";
    private static final String IS_MANUAL = "TRUE";

    // -------------------------------------------------------
    // CREATE
    // -------------------------------------------------------

    @Override
    @Transactional
    public ManualMiningEntryResponseDTO createApplication(ManualMiningEntryRequestDTO request, Long userId) {
        validator.validate(request);

        String type = request.getActivityType().toUpperCase();
        String prefix = resolvePrefix(type);
        String finalStatus = resolveFinalStatus(type);
        LocalDateTime now = LocalDateTime.now();

        ManualMiningEntryResponseDTO response = switch (type) {
            case "MINING_LEASE" -> createMlEntry(request, userId, prefix, finalStatus, now);
            case "QUARRY_LEASE" -> createQlEntry(request, userId, prefix, finalStatus, now);
            case "SURFACE_COLLECTION" -> createScEntry(request, userId, prefix, finalStatus, now);
            case "STOCK_LIFTING" -> createSLEntry(request, userId, prefix, finalStatus, now);
            default -> throw new BusinessException(ErrorCodes.INVALID_REQUEST, "Unknown activityType: " + type);
        };

        registerApplicantAccountAfterCommit(request);

        return response;
    }

    // Runs only once the submission has actually committed, in its own transaction,
    // so a failure provisioning the applicant's account/email can never roll back
    // (or be rolled back by) the manual-entry submission itself.
    private void registerApplicantAccountAfterCommit(ManualMiningEntryRequestDTO req) {
        Runnable provision = () -> applicantAccountProvisioningService.provisionForApplicant(
                req.getApplicantType(), req.getApplicantCid(), req.getApplicantName(),
                req.getApplicantContact(), req.getApplicantEmail(),
                req.getLicenseNo(), req.getBusinessLicenseNo(),
                req.getCompanyRegistrationNo(), req.getCompanyName(), req.getCompanyType());

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            provision.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                provision.run();
            }
        });
    }

    private ManualMiningEntryResponseDTO createMlEntry(ManualMiningEntryRequestDTO req, Long userId,
                                                        String prefix, String status, LocalDateTime now) {
        MiningLeaseApplication ml = new MiningLeaseApplication();

        Long regionId = 0L;
        if(req.getDzongkhag() != null){

            Optional<DzongkhagLookup> dzongkhagLookup = Optional.of(new DzongkhagLookup());

            dzongkhagLookup = dzongkhagLookupRepository.findById(req.getDzongkhag());

            if(dzongkhagLookup.isPresent()){
                DzongkhagLookup lookup = dzongkhagLookup.get();
                regionId = lookup.getRegion().getId();
            }

        }
        ml.setApplicantType(req.getApplicantType());
        ml.setApplicantCid(req.getApplicantCid());
        ml.setApplicantName(req.getApplicantName());
        ml.setApplicantContact(req.getApplicantContact());
        ml.setApplicantEmail(req.getApplicantEmail());
        ml.setPostalAddress(req.getPostalAddress());
        ml.setTelephoneNo(req.getTelephoneNo());
        ml.setLicenseNo(req.getLicenseNo());
        ml.setBusinessLicenseNo(req.getBusinessLicenseNo());
        ml.setCompanyRegistrationNo(req.getCompanyRegistrationNo());
        ml.setCompanyName(req.getCompanyName());
        ml.setCompanyType(req.getCompanyType());
        ml.setApplicationType("Submitted");
        ml.setNameOfMine(req.getNameOfMine());
        ml.setEcFileId(req.getEcFileId());
        ml.setEcNumber(req.getEcNumber());
        ml.setPlaceOfMiningActivity(req.getPlaceOfActivity());
        DzongkhagLookup mlDzongkhag = resolveDzongkhag(req.getDzongkhag());
        if (mlDzongkhag != null) {
            ml.setDzongkhag(mlDzongkhag);
            ml.setRegionId(mlDzongkhag.getRegion() != null ? mlDzongkhag.getRegion().getId() : null);
        }
        ml.setGewog(resolveGewog(req.getGewog()));
        ml.setNearestVillage(resolveVillage(req.getNearestVillage()));
        ml.setDungkhag(req.getDungkhag());
        ml.setTypeOfMines(req.getTypeOfMines());
        ml.setTypeOfMineralsProducts(req.getTypeOfMinerals());
        ml.setRequiredInvestment(req.getRequiredInvestment());
        ml.setSourceOfFinance(req.getSourceOfFinance());
        ml.setTechnicalCompetenceExperience(req.getTechnicalCompetenceExperience());
        ml.setWorkforceRequirementRecruitment(req.getWorkforceRequirementRecruitment());
        ml.setProposedLeasePeriod(req.getProposedLeasePeriod());
        ml.setSrf(req.getSrf());
        ml.setLandPrivate(req.getLandPrivate());
        ml.setTotalLand(req.getTotalLand());
        ml.setApprovedArea(req.getApprovedArea());
        ml.setApprovedErb(req.getApprovedErb());
        ml.setApprovedLeasePeriod(req.getApprovedLeasePeriod());
        ml.setApprovedMineral(req.getApprovedMineral());
        ml.setLeaseStartDate(req.getLeaseStartDate());
        ml.setLeaseEndDate(req.getLeaseEndDate());
        ml.setLeasePeriodYears(req.getLeasePeriodYears());
        ml.setUpfrontPaymentAmount(req.getUpfrontPaymentAmount());
        ml.setFmfsStatus(req.getFmfsStatus());
        ml.setFmfsId(req.getFmfsId());
        ml.setECStatus(req.getEcStatus());
        ml.setEcExpiryDate(toDate(req.getEcExpiryDate()));
        ml.setMlaStatus(req.getMlaStatus());
        ml.setGeologicalReportStatus(req.getGeologicalReportStatus());
        ml.setPfsDocId(req.getPfsDocId());
        ml.setLocationMapDocId(req.getLocationMapDocId());
        ml.setFinancialCapabilityDocId(req.getFinancialCapabilityDocId());
        ml.setExplorationReportDocId(req.getExplorationReportDocId());
        ml.setConsentLetterDocId(req.getConsentLetterDocId());
        ml.setGeologicalReportDocId(req.getGeologicalReportDocId());
        ml.setFmfsDocId(req.getFmfsDocId());
        ml.setLlcDocId(req.getLlcDocId());
        ml.setNotesheetDocId(req.getNotesheetDocId());
        ml.setMlaDocId(req.getMlaDocId());
        ml.setFileUploadIdGr(req.getFileUploadIdGr());
        ml.setFileUploadIdKmz(req.getFileUploadIdKmz());
        ml.setFileUploadIdPA(req.getFileUploadIdPA());
        ml.setFileUploadIdFC(req.getFileUploadIdFC());
        ml.setFileUploadIdPublicClearance(req.getFileUploadIdPublicClearance());
        ml.setMpcdFileUploadIdPA(req.getMpcdFileUploadIdPA());
        ml.setMpcdFileUploadIdMa(req.getMpcdFileUploadIdMa());
        ml.setSignedPFSId(req.getSignedPFSId());
        ml.setBankGuarantorDocId(req.getBankGuarantorDocId());
        ml.setWorkOrderDocId(req.getWorkOrderDocId());
        ml.setApplicationNumber(generateMlAppNumber(prefix));
        ml.setCurrentStatus(status);
        ml.setApplicantUserId(userId);
        ml.setCreatedBy(userId);
        ml.setIsManualEntry(IS_MANUAL);
        ml.setManualEntryBy(userId);
        ml.setManualEntryOn(now);
        ml.setSubmittedAt(now);
        ml.setApprovedAt(now);
        ml.setIsActive(true);

        ml.setRegionId(regionId);


        MiningLeaseApplication saved = mlRepo.save(ml);
        saveAttachments(req.getFileIds(), saved.getApplicationNumber());
        ApplicationMaster master = createApplicationMaster(saved.getApplicationNumber(), userId, status, now);
        saved.setApplicationMaster(master);
        mlRepo.save(saved);
        notifyPromoter(req.getPromoterId(), saved.getApplicationNumber());

        return toResponseFromMl(saved, req.getFileIds());
    }

    private ManualMiningEntryResponseDTO createQlEntry(ManualMiningEntryRequestDTO req, Long userId,
                                                        String prefix, String status, LocalDateTime now) {
        QuarryLeaseApplication ql = new QuarryLeaseApplication();

        Long regionId = 0L;
        if(req.getDzongkhag() != null){

            Optional<DzongkhagLookup> dzongkhagLookup = Optional.of(new DzongkhagLookup());

            dzongkhagLookup = dzongkhagLookupRepository.findById(req.getDzongkhag());

            if(dzongkhagLookup.isPresent()){
                DzongkhagLookup lookup = dzongkhagLookup.get();
                regionId = lookup.getRegion().getId();
            }

        }

        ql.setApplicantType(req.getApplicantType());
        ql.setApplicantCid(req.getApplicantCid());
        ql.setApplicantName(req.getApplicantName());
        ql.setApplicantContact(req.getApplicantContact());
        ql.setApplicantEmail(req.getApplicantEmail());
        ql.setPostalAddress(req.getPostalAddress());
        ql.setTelephoneNo(req.getTelephoneNo());
        ql.setLicenseNo(req.getLicenseNo());
        ql.setBusinessLicenseNo(req.getBusinessLicenseNo());
        ql.setCompanyRegistrationNo(req.getCompanyRegistrationNo());
        ql.setCompanyName(req.getCompanyName());
        ql.setCompanyType(req.getCompanyType());
        ql.setApplicationType("Submitted");
        ql.setNameOfQuarry(req.getNameOfQuarry());
        ql.setMlaSignedDocId(req.getMlaSignedDocId());
        ql.setPlaceOfMiningActivity(req.getPlaceOfActivity());
        DzongkhagLookup qlDzongkhag = resolveDzongkhag(req.getDzongkhag());
        if (qlDzongkhag != null) {
            ql.setDzongkhag(qlDzongkhag);
        }
        ql.setGewog(resolveGewog(req.getGewog()));
        ql.setNearestVillage(resolveVillage(req.getNearestVillage()));
        ql.setDungkhag(req.getDungkhag());
        ql.setTypeOfMines(req.getTypeOfMines());
        ql.setTypeOfMineralsProducts(req.getTypeOfMinerals());
        ql.setRequiredInvestment(req.getRequiredInvestment());
        ql.setSourceOfFinance(req.getSourceOfFinance());
        ql.setTechnicalCompetenceExperience(req.getTechnicalCompetenceExperience());
        ql.setWorkforceRequirementRecruitment(req.getWorkforceRequirementRecruitment());
        ql.setProposedLeasePeriod(req.getProposedLeasePeriod());
        ql.setSrf(req.getSrf());
        ql.setLandPrivate(req.getLandPrivate());
        ql.setTotalLand(req.getTotalLand());
        ql.setApprovedArea(req.getApprovedArea());
        ql.setApprovedErb(req.getApprovedErb());
        ql.setApprovedLeasePeriod(req.getApprovedLeasePeriod());
        ql.setApprovedMineral(req.getApprovedMineral());
        ql.setLeaseStartDate(req.getLeaseStartDate());
        ql.setLeaseEndDate(req.getLeaseEndDate());
        ql.setLeasePeriodYears(req.getLeasePeriodYears());
        ql.setUpfrontPaymentAmount(req.getUpfrontPaymentAmount());
        ql.setFmfsStatus(req.getFmfsStatus());
        ql.setFmfsId(req.getFmfsId());
        ql.setECStatus(req.getEcStatus());
        ql.setECExpiryDate(toDate(req.getEcExpiryDate()));
        ql.setMlaStatus(req.getMlaStatus());
        ql.setGeologicalReportStatus(req.getGeologicalReportStatus());
        ql.setPfsDocId(req.getPfsDocId());
        ql.setLocationMapDocId(req.getLocationMapDocId());
        ql.setFinancialCapabilityDocId(req.getFinancialCapabilityDocId());
        ql.setExplorationReportDocId(req.getExplorationReportDocId());
        ql.setConsentLetterDocId(req.getConsentLetterDocId());
        ql.setGeologicalReportDocId(req.getGeologicalReportDocId());
        ql.setFmfsDocId(req.getFmfsDocId());
        ql.setLlcDocId(req.getLlcDocId());
        ql.setNotesheetDocId(req.getNotesheetDocId());
        ql.setMlaDocId(req.getMlaDocId());
        ql.setFileUploadIdGr(req.getFileUploadIdGr());
        ql.setFileUploadIdPA(req.getFileUploadIdPA());
        ql.setFileUploadIdFC(req.getFileUploadIdFC());
        ql.setFileUploadIdPublicClearance(req.getFileUploadIdPublicClearance());
        ql.setMpcdFileUploadIdPA(req.getMpcdFileUploadIdPA());
        ql.setMpcdFileUploadIdMa(req.getMpcdFileUploadIdMa());
        ql.setBankGuarantorDocId(req.getBankGuarantorDocId());
        ql.setWorkOrderDocId(req.getWorkOrderDocId());
        ql.setApplicationNumber(generateQlAppNumber(prefix));
        ql.setCurrentStatus(status);
        ql.setApplicantUserId(userId);
        ql.setCreatedBy(userId);
        ql.setIsManualEntry(IS_MANUAL);
        ql.setManualEntryBy(userId);
        ql.setManualEntryOn(now);
        ql.setSubmittedAt(now);
        ql.setApprovedAt(now);
        ql.setIsActive(true);

        ql.setRegionId(regionId);

        QuarryLeaseApplication saved = qlRepo.save(ql);
        saveAttachments(req.getFileIds(), saved.getApplicationNumber());
        ApplicationMaster master = createApplicationMaster(saved.getApplicationNumber(), userId, status, now);
        saved.setApplicationMaster(master);
        qlRepo.save(saved);
        notifyPromoter(req.getPromoterId(), saved.getApplicationNumber());

        return toResponseFromQl(saved, req.getFileIds());
    }

    private ManualMiningEntryResponseDTO createScEntry(ManualMiningEntryRequestDTO req, Long userId,
                                                        String prefix, String status, LocalDateTime now) {
        SurfaceCollectionPermitEntity sc = new SurfaceCollectionPermitEntity();

        Long regionId = 0L;
        if(req.getDzongkhag() != null){

            Optional<DzongkhagLookup> dzongkhagLookup = Optional.of(new DzongkhagLookup());

            dzongkhagLookup = dzongkhagLookupRepository.findById(req.getDzongkhag());

            if(dzongkhagLookup.isPresent()){
                DzongkhagLookup lookup = dzongkhagLookup.get();
                regionId = lookup.getRegion().getId();
            }

        }

        sc.setApplicantCid(req.getApplicantCid());
        sc.setApplicantName(req.getApplicantName());
        sc.setMobileNo(req.getApplicantContact());
        sc.setEmail(req.getApplicantEmail());
        sc.setSecurityClearanceValidity(req.getSecurityClearanceValidity());
        sc.setTaxClearanceValidity(req.getTaxClearanceValidity());
        sc.setIsStateOwned(req.getIsStateOwned());
        sc.setIsRpBased(req.getIsRpBased());
        sc.setDzongkhag(req.getDzongkhag());
        sc.setGewog(req.getGewog());
        sc.setPlaceVillage(req.getNearestVillage());
        sc.setTypeOfActivity(req.getTypeOfActivity());
        sc.setTypeOfMaterials(req.getTypeOfMaterials());
        sc.setCollectionSite(req.getCollectionSite());
        sc.setProposedAreaSrf(req.getProposedAreaSrf());
        sc.setProposedAreaStateLand(req.getProposedAreaStateLand());
        sc.setProposedAreaPrivate(req.getProposedAreaPrivate());
        sc.setProposedAreaRow(req.getProposedAreaRow());
        sc.setPermitNo(req.getPermitNo());
        sc.setEcNo(req.getEcNo());
        sc.setAttachmentMapFileId(req.getAttachmentMapFileId());
        sc.setRecommendationLetterFileId(req.getRecommendationLetterFileId());
        sc.setConsentLetterFileId(req.getScConsentLetterFileId());
        sc.setFcFileId(req.getFcFileId());
        sc.setIeeFileId(req.getIeeFileId());
        sc.setEmpFileId(req.getEmpFileId());
        sc.setAdmApprovalFileId(req.getAdmApprovalFileId());
        sc.setUndertakingFileId(req.getUndertakingFileId());
        sc.setBgFileId(req.getBgFileId());
        sc.setMpcdReportFileId(req.getMpcdReportFileId());
        sc.setIomFileId(req.getIomFileId());
        sc.setRcReportFileId(req.getRcReportFileId());
        sc.setMiReportFileId(req.getMiReportFileId());
        sc.setEcFileId(req.getScEcFileId());
        sc.setApplicationNo(generateScAppNumber(prefix));
        sc.setStatus(status);
        sc.setCreatedBy(userId);
        sc.setIsManualEntry(IS_MANUAL);
        sc.setManualEntryBy(userId);
        sc.setManualEntryOn(now);
        sc.setIsActive(true);

        sc.setRegionId(regionId);

        SurfaceCollectionPermitEntity saved = scRepo.save(sc);
        saveAttachments(req.getFileIds(), saved.getApplicationNo());
        createApplicationMaster(saved.getApplicationNo(), userId, status, now);
        notifyPromoter(req.getPromoterId(), saved.getApplicationNo());

        return toResponseFromSc(saved, req.getFileIds());
    }


    private ManualMiningEntryResponseDTO createSLEntry(ManualMiningEntryRequestDTO req, Long userId,
                                                       String prefix, String status, LocalDateTime now) {
        StockLiftingApplication sl = new StockLiftingApplication();

        sl.setApplicantCid(req.getApplicantCid());
        sl.setApplicantName(req.getApplicantName());
        sl.setApplicantType(req.getApplicantType());
        sl.setApplicantEmail(req.getApplicantEmail());
        sl.setApplicantCid(req.getApplicantCid());
        sl.setApplicantContact(req.getApplicantContact());
        sl.setPostalAddress(req.getPostalAddress());
        sl.setTelephoneNo(req.getTelephoneNo());
        sl.setLicenseNo(req.getLicenseNo());
        sl.setBusinessLicenseNo(req.getBusinessLicenseNo());
        sl.setCompanyRegistrationNo(req.getCompanyRegistrationNo());
        sl.setCompanyName(req.getCompanyName());
        sl.setCompanyType(req.getCompanyType());

        // New Columns added
        sl.setValidityDate(req.getValidityDate());
        sl.setQuantity(req.getQuantity());

        sl.setStockLiftingPermitNo(generatePermitNumber());
        sl.setApplicationFileId(req.getApplicationFileId());
        sl.setPermitFileId(req.getPermitFileId());
        sl.setIomFileId(req.getIomFileId());
        sl.setRcReportFileId(req.getRcReportFileId());
        sl.setApplicationNo(generateSLApplicationNumber());
        sl.setStatus(status);

        sl.setManualEntryOn(now);
        sl.setManualEntryBy(userId);
        sl.setIsManualEntry(IS_MANUAL);

        sl.setCreatedBy(userId);

        sl.setIsActive(true);

        StockLiftingApplication saved = stockLiftingRepository.save(sl);
        saveAttachments(req.getFileIds(), saved.getApplicationNo());
        createApplicationMaster(saved.getApplicationNo(), userId, status, now);
        notifyPromoter(req.getPromoterId(), saved.getApplicationNo());

        return toResponseFromSL(saved, req.getFileIds());
    }

    // -------------------------------------------------------
    // GET LIST
    // -------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public SuccessResponse<List<ManualMiningEntryResponseDTO>> getApplications(Long userId, Pageable pageable, String search) {
        List<MiningLeaseApplication> mlList = mlRepo.findByIsManualEntryAndManualEntryBy(IS_MANUAL, userId);
        List<QuarryLeaseApplication> qlList = qlRepo.findByIsManualEntryAndManualEntryBy(IS_MANUAL, userId);
        List<SurfaceCollectionPermitEntity> scList = scRepo.findByIsManualEntryAndManualEntryBy(IS_MANUAL, userId);
        List<StockLiftingApplication> slList = stockLiftingRepository.findByIsManualEntryAndManualEntryBy(IS_MANUAL, userId);
        return buildPagedResponse(combine(mlList, qlList, scList, slList, search), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public SuccessResponse<List<ManualMiningEntryResponseDTO>> getAllApplications(Pageable pageable, String search) {
        List<MiningLeaseApplication> mlList = mlRepo.findByIsManualEntry(IS_MANUAL);
        List<QuarryLeaseApplication> qlList = qlRepo.findByIsManualEntry(IS_MANUAL);
        List<SurfaceCollectionPermitEntity> scList = scRepo.findByIsManualEntry(IS_MANUAL);
        List<StockLiftingApplication> slList = stockLiftingRepository.findByIsManualEntry(IS_MANUAL);
        return buildPagedResponse(combine(mlList, qlList, scList,slList, search), pageable);
    }

    // -------------------------------------------------------
    // GET SINGLE
    // -------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ManualMiningEntryResponseDTO getApplicationByNo(String applicationNo) {
        List<String> fileIds = attachmentRepository.findByApplicationNo(applicationNo)
                .stream().map(ManualMiningAttachmentEntity::getFileId).toList();

        if (applicationNo.startsWith("MAN-QL-")) {
            QuarryLeaseApplication ql = qlRepo.findByApplicationNumber(applicationNo)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));
            return toResponseFromQl(ql, fileIds);
        }
        if (applicationNo.startsWith("MAN-SC-") || applicationNo.startsWith("MAN-SL-")) {
            SurfaceCollectionPermitEntity sc = scRepo.findByApplicationNo(applicationNo)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));
            return toResponseFromSc(sc, fileIds);
        }
        MiningLeaseApplication ml = mlRepo.findByApplicationNumber(applicationNo)
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));
        return toResponseFromMl(ml, fileIds);
    }

    // -------------------------------------------------------
    // RESPONSE BUILDERS
    // -------------------------------------------------------

    private ManualMiningEntryResponseDTO toResponseFromMl(MiningLeaseApplication ml, List<String> fileIds) {
        return ManualMiningEntryResponseDTO.builder()
                .id(ml.getId())
                .applicationNo(ml.getApplicationNumber())
                .activityType("MINING_LEASE")
                .status(ml.getCurrentStatus())
                .isManualEntry(true)
                .applicantType(ml.getApplicantType())
                .applicantCid(ml.getApplicantCid())
                .applicantName(ml.getApplicantName())
                .applicantContact(ml.getApplicantContact())
                .applicantEmail(ml.getApplicantEmail())
                .postalAddress(ml.getPostalAddress())
                .telephoneNo(ml.getTelephoneNo())
                .licenseNo(ml.getLicenseNo())
                .businessLicenseNo(ml.getBusinessLicenseNo())
                .companyRegistrationNo(ml.getCompanyRegistrationNo())
                .companyName(ml.getCompanyName())
                .companyType(ml.getCompanyType())
                .nameOfMine(ml.getNameOfMine())
                .ecFileId(ml.getEcFileId())
                .ecNumber(ml.getEcNumber())
                .placeOfActivity(ml.getPlaceOfMiningActivity())
                .dungkhag(ml.getDungkhag())
                .typeOfMines(ml.getTypeOfMines())
                .typeOfMinerals(ml.getTypeOfMineralsProducts())
                .requiredInvestment(ml.getRequiredInvestment())
                .sourceOfFinance(ml.getSourceOfFinance())
                .technicalCompetenceExperience(ml.getTechnicalCompetenceExperience())
                .workforceRequirementRecruitment(ml.getWorkforceRequirementRecruitment())
                .proposedLeasePeriod(ml.getProposedLeasePeriod())
                .srf(ml.getSrf())
                .landPrivate(ml.getLandPrivate())
                .totalLand(ml.getTotalLand())
                .approvedArea(ml.getApprovedArea())
                .approvedErb(ml.getApprovedErb())
                .approvedLeasePeriod(ml.getApprovedLeasePeriod())
                .approvedMineral(ml.getApprovedMineral())
                .leaseStartDate(ml.getLeaseStartDate())
                .leaseEndDate(ml.getLeaseEndDate())
                .leasePeriodYears(ml.getLeasePeriodYears())
                .upfrontPaymentAmount(ml.getUpfrontPaymentAmount())
                .fmfsStatus(ml.getFmfsStatus())
                .fmfsId(ml.getFmfsId())
                .ecStatus(ml.getECStatus())
                .ecExpiryDate(toLocalDate(ml.getEcExpiryDate()))
                .mlaStatus(ml.getMlaStatus())
                .geologicalReportStatus(ml.getGeologicalReportStatus())
                .pfsDocId(ml.getPfsDocId())
                .locationMapDocId(ml.getLocationMapDocId())
                .financialCapabilityDocId(ml.getFinancialCapabilityDocId())
                .explorationReportDocId(ml.getExplorationReportDocId())
                .consentLetterDocId(ml.getConsentLetterDocId())
                .geologicalReportDocId(ml.getGeologicalReportDocId())
                .fmfsDocId(ml.getFmfsDocId())
                .llcDocId(ml.getLlcDocId())
                .notesheetDocId(ml.getNotesheetDocId())
                .mlaDocId(ml.getMlaDocId())
                .fileUploadIdGr(ml.getFileUploadIdGr())
                .fileUploadIdKmz(ml.getFileUploadIdKmz())
                .fileUploadIdPA(ml.getFileUploadIdPA())
                .fileUploadIdFC(ml.getFileUploadIdFC())
                .fileUploadIdPublicClearance(ml.getFileUploadIdPublicClearance())
                .mpcdFileUploadIdPA(ml.getMpcdFileUploadIdPA())
                .mpcdFileUploadIdMa(ml.getMpcdFileUploadIdMa())
                .signedPFSId(ml.getSignedPFSId())
                .bankGuarantorDocId(ml.getBankGuarantorDocId())
                .workOrderDocId(ml.getWorkOrderDocId())
                .fileIds(fileIds != null ? fileIds : Collections.emptyList())
                .createdBy(ml.getManualEntryBy())
                .createdOn(ml.getManualEntryOn())
                .regionId(ml.getRegionId())
                .build();
    }

    private ManualMiningEntryResponseDTO toResponseFromQl(QuarryLeaseApplication ql, List<String> fileIds) {
        return ManualMiningEntryResponseDTO.builder()
                .id(ql.getId())
                .applicationNo(ql.getApplicationNumber())
                .activityType("QUARRY_LEASE")
                .status(ql.getCurrentStatus())
                .isManualEntry(true)
                .applicantType(ql.getApplicantType())
                .applicantCid(ql.getApplicantCid())
                .applicantName(ql.getApplicantName())
                .applicantContact(ql.getApplicantContact())
                .applicantEmail(ql.getApplicantEmail())
                .postalAddress(ql.getPostalAddress())
                .telephoneNo(ql.getTelephoneNo())
                .licenseNo(ql.getLicenseNo())
                .businessLicenseNo(ql.getBusinessLicenseNo())
                .companyRegistrationNo(ql.getCompanyRegistrationNo())
                .companyName(ql.getCompanyName())
                .companyType(ql.getCompanyType())
                .nameOfQuarry(ql.getNameOfQuarry())
                .mlaSignedDocId(ql.getMlaSignedDocId())
                .placeOfActivity(ql.getPlaceOfMiningActivity())
                .dungkhag(ql.getDungkhag())
                .typeOfMines(ql.getTypeOfMines())
                .typeOfMinerals(ql.getTypeOfMineralsProducts())
                .requiredInvestment(ql.getRequiredInvestment())
                .sourceOfFinance(ql.getSourceOfFinance())
                .technicalCompetenceExperience(ql.getTechnicalCompetenceExperience())
                .workforceRequirementRecruitment(ql.getWorkforceRequirementRecruitment())
                .proposedLeasePeriod(ql.getProposedLeasePeriod())
                .srf(ql.getSrf())
                .landPrivate(ql.getLandPrivate())
                .totalLand(ql.getTotalLand())
                .approvedArea(ql.getApprovedArea())
                .approvedErb(ql.getApprovedErb())
                .approvedLeasePeriod(ql.getApprovedLeasePeriod())
                .approvedMineral(ql.getApprovedMineral())
                .leaseStartDate(ql.getLeaseStartDate())
                .leaseEndDate(ql.getLeaseEndDate())
                .leasePeriodYears(ql.getLeasePeriodYears())
                .upfrontPaymentAmount(ql.getUpfrontPaymentAmount())
                .fmfsStatus(ql.getFmfsStatus())
                .fmfsId(ql.getFmfsId())
                .ecStatus(ql.getECStatus())
                .ecExpiryDate(toLocalDate(ql.getECExpiryDate()))
                .mlaStatus(ql.getMlaStatus())
                .geologicalReportStatus(ql.getGeologicalReportStatus())
                .pfsDocId(ql.getPfsDocId())
                .locationMapDocId(ql.getLocationMapDocId())
                .financialCapabilityDocId(ql.getFinancialCapabilityDocId())
                .explorationReportDocId(ql.getExplorationReportDocId())
                .consentLetterDocId(ql.getConsentLetterDocId())
                .geologicalReportDocId(ql.getGeologicalReportDocId())
                .fmfsDocId(ql.getFmfsDocId())
                .llcDocId(ql.getLlcDocId())
                .notesheetDocId(ql.getNotesheetDocId())
                .mlaDocId(ql.getMlaDocId())
                .fileUploadIdGr(ql.getFileUploadIdGr())
                .fileUploadIdPA(ql.getFileUploadIdPA())
                .fileUploadIdFC(ql.getFileUploadIdFC())
                .fileUploadIdPublicClearance(ql.getFileUploadIdPublicClearance())
                .mpcdFileUploadIdPA(ql.getMpcdFileUploadIdPA())
                .mpcdFileUploadIdMa(ql.getMpcdFileUploadIdMa())
                .bankGuarantorDocId(ql.getBankGuarantorDocId())
                .workOrderDocId(ql.getWorkOrderDocId())
                .fileIds(fileIds != null ? fileIds : Collections.emptyList())
                .createdBy(ql.getManualEntryBy())
                .createdOn(ql.getManualEntryOn())
                .regionId(ql.getRegionId())
                .build();
    }

    private ManualMiningEntryResponseDTO toResponseFromSc(SurfaceCollectionPermitEntity sc, List<String> fileIds) {
        String activityType = (sc.getTypeOfActivity() != null
                && sc.getTypeOfActivity().toLowerCase().contains("stock"))
                ? "STOCK_LIFTING" : "SURFACE_COLLECTION";

        return ManualMiningEntryResponseDTO.builder()
                .id(sc.getId())
                .applicationNo(sc.getApplicationNo())
                .activityType(activityType)
                .status(sc.getStatus())
                .isManualEntry(true)
                .applicantCid(sc.getApplicantCid())
                .applicantName(sc.getApplicantName())
                .applicantContact(sc.getMobileNo())
                .applicantEmail(sc.getEmail())
                .securityClearanceValidity(sc.getSecurityClearanceValidity())
                .taxClearanceValidity(sc.getTaxClearanceValidity())
                .isStateOwned(sc.getIsStateOwned())
                .isRpBased(sc.getIsRpBased())
                .dzongkhag(sc.getDzongkhag())
                .gewog(sc.getGewog())
                .nearestVillage(sc.getPlaceVillage())
                .typeOfActivity(sc.getTypeOfActivity())
                .typeOfMaterials(sc.getTypeOfMaterials())
                .collectionSite(sc.getCollectionSite())
                .proposedAreaSrf(sc.getProposedAreaSrf())
                .proposedAreaStateLand(sc.getProposedAreaStateLand())
                .proposedAreaPrivate(sc.getProposedAreaPrivate())
                .proposedAreaRow(sc.getProposedAreaRow())
                .permitNo(sc.getPermitNo())
                .ecNo(sc.getEcNo())
                .attachmentMapFileId(sc.getAttachmentMapFileId())
                .recommendationLetterFileId(sc.getRecommendationLetterFileId())
                .scConsentLetterFileId(sc.getConsentLetterFileId())
                .fcFileId(sc.getFcFileId())
                .ieeFileId(sc.getIeeFileId())
                .empFileId(sc.getEmpFileId())
                .admApprovalFileId(sc.getAdmApprovalFileId())
                .undertakingFileId(sc.getUndertakingFileId())
                .bgFileId(sc.getBgFileId())
                .mpcdReportFileId(sc.getMpcdReportFileId())
                .iomFileId(sc.getIomFileId())
                .rcReportFileId(sc.getRcReportFileId())
                .miReportFileId(sc.getMiReportFileId())
                .scEcFileId(sc.getEcFileId())
                .fileIds(fileIds != null ? fileIds : Collections.emptyList())
                .createdBy(sc.getManualEntryBy())
                .createdOn(sc.getManualEntryOn())
                .regionId(sc.getRegionId())
                .build();
    }

    private ManualMiningEntryResponseDTO toResponseFromSL(StockLiftingApplication ql, List<String> fileIds) {
        return ManualMiningEntryResponseDTO.builder()
                .id(ql.getId())
                .applicationNo(ql.getApplicationNo())
                .activityType("STOCK_LIFTING")
                .status(ql.getStatus())
                .isManualEntry(true)
                .applicantType(ql.getApplicantType())
                .applicantCid(ql.getApplicantCid())
                .applicantName(ql.getApplicantName())
                .applicantContact(ql.getApplicantContact())
                .applicantEmail(ql.getApplicantEmail())
                .postalAddress(ql.getPostalAddress())
                .telephoneNo(ql.getTelephoneNo())
                .licenseNo(ql.getLicenseNo())
                .businessLicenseNo(ql.getBusinessLicenseNo())
                .companyRegistrationNo(ql.getCompanyRegistrationNo())
                .companyName(ql.getCompanyName())
                .companyType(ql.getCompanyType())
                .fileIds(fileIds != null ? fileIds : Collections.emptyList())
                .createdBy(ql.getCreatedBy())
                .createdOn(ql.getCreatedOn())
                .applicationFileId(ql.getApplicationFileId())
                .rcReportFileId(ql.getRcReportFileId())
                .iomFileId(ql.getIomFileId())
                .permitFileId(ql.getPermitFileId())
                .build();
    }


    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private List<ManualMiningEntryResponseDTO> combine(
            List<MiningLeaseApplication> mlList,
            List<QuarryLeaseApplication> qlList,
            List<SurfaceCollectionPermitEntity> scList,
            List<StockLiftingApplication> slList,
            String search) {

        List<ManualMiningEntryResponseDTO> combined = Stream.of(
                        mlList.stream().map(ml -> toResponseFromMl(ml, null)),
                        qlList.stream().map(ql -> toResponseFromQl(ql, null)),
                        slList.stream().map(sl -> toResponseFromSL(sl, null)),
                        scList.stream().map(sc -> toResponseFromSc(sc, null))
                )
                .flatMap(Function.identity())
                .sorted(Comparator.comparing(
                        ManualMiningEntryResponseDTO::getCreatedOn,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(Collectors.toList());

        if (search != null && !search.isBlank()) {
            String s = search.toLowerCase();
            combined = combined.stream()
                    .filter(dto -> matches(dto, s))
                    .collect(Collectors.toList());
        }

        List<String> appNos = combined.stream()
                .map(ManualMiningEntryResponseDTO::getApplicationNo)
                .filter(Objects::nonNull)
                .toList();
        Map<String, List<String>> fileMap = buildFileMapByAppNo(appNos);
        combined.forEach(dto -> dto.setFileIds(
                fileMap.getOrDefault(dto.getApplicationNo(), Collections.emptyList())));

        return combined;
    }

    private boolean matches(ManualMiningEntryResponseDTO dto, String search) {
        return (dto.getApplicantName() != null && dto.getApplicantName().toLowerCase().contains(search))
                || (dto.getApplicationNo() != null && dto.getApplicationNo().toLowerCase().contains(search))
                || (dto.getApplicantCid() != null && dto.getApplicantCid().toLowerCase().contains(search));
    }

    private SuccessResponse<List<ManualMiningEntryResponseDTO>> buildPagedResponse(
            List<ManualMiningEntryResponseDTO> all, Pageable pageable) {
        int total = all.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<ManualMiningEntryResponseDTO> content = start >= total
                ? Collections.emptyList() : all.subList(start, end);
        Page<ManualMiningEntryResponseDTO> page = new PageImpl<>(content, pageable, total);
        return SuccessResponse.fromPage("Applications fetched successfully", page);
    }

    private void saveAttachments(List<String> fileIds, String applicationNo) {
        if (fileIds == null || fileIds.isEmpty()) return;
        List<ManualMiningAttachmentEntity> attachments = fileIds.stream()
                .map(fileId -> ManualMiningAttachmentEntity.builder()
                        .fileId(fileId)
                        .applicationNo(applicationNo)
                        .build())
                .toList();
        attachmentRepository.saveAll(attachments);
    }

    private ApplicationMaster createApplicationMaster(String appNo, Long userId, String status, LocalDateTime now) {
        ApplicationMaster master = new ApplicationMaster();
        master.setApplicationNumber(appNo);
        master.setServiceCode(SERVICE_CODE);
        master.setApplicantUserId(userId);
        master.setCurrentStatus(status);
        master.setSubmittedAt(now);
        master.setApprovedAt(now);
        return applicationMasterRepository.save(master);
    }

    private void notifyPromoter(Long promoterId, String applicationNo) {
        if (promoterId == null) return;
        UserWorkloadProjection promoter = entryRepository.findUserDetails(promoterId);
        if (promoter == null || promoter.getEmail() == null) return;
        notificationClient.sendApprovalManualEntryNotification(
                promoter.getEmail(), promoter.getUsername(), applicationNo);
    }

    private Map<String, List<String>> buildFileMapByAppNo(List<String> appNos) {
        if (appNos.isEmpty()) return Collections.emptyMap();
        return attachmentRepository.findByApplicationNoIn(appNos).stream()
                .filter(a -> a.getApplicationNo() != null)
                .collect(Collectors.groupingBy(
                        ManualMiningAttachmentEntity::getApplicationNo,
                        Collectors.mapping(ManualMiningAttachmentEntity::getFileId, Collectors.toList())));
    }

    private DzongkhagLookup resolveDzongkhag(String dzongkhagId) {
        if (dzongkhagId == null || dzongkhagId.isBlank()) return null;
        return dzongkhagLookupRepository.findById(dzongkhagId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.INVALID_REQUEST, "Invalid Dzongkhag ID: " + dzongkhagId));
    }

    private GewogLookup resolveGewog(String gewogId) {
        if (gewogId == null || gewogId.isBlank()) return null;
        return (GewogLookup) gewogLookupRepository.findByGewogId(gewogId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.INVALID_REQUEST, "Invalid Gewog ID: " + gewogId));
    }

    private VillageLookup resolveVillage(String villageSerialNo) {
        if (villageSerialNo == null || villageSerialNo.isBlank()) return null;
        return villageLookupRepository.findByVillageSerialNo(Integer.parseInt(villageSerialNo))
                .orElseThrow(() -> new BusinessException(ErrorCodes.INVALID_REQUEST, "Invalid Village ID: " + villageSerialNo));
    }

    private Date toDate(LocalDate localDate) {
        if (localDate == null) return null;
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private synchronized String generateMlAppNumber(String prefix) {
        Integer max = mlRepo.findMaxDraftSequenceByPrefix(prefix);
        return prefix + String.format("%06d", (max == null ? 0L : max) + 1L);
    }

    private synchronized String generateQlAppNumber(String prefix) {
        Integer max = qlRepo.findMaxManualEntrySequenceByPrefix(prefix);
        return prefix + String.format("%06d", (max == null ? 0L : max) + 1L);
    }

    private synchronized String generateScAppNumber(String prefix) {
        Integer max = scRepo.findMaxSequenceByPrefix(prefix);
        return prefix + String.format("%06d", (max == null ? 0L : max) + 1L);
    }

    private String generateSLApplicationNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "APPSL-" + date + String.format("%05d", monthlySequence());
    }

    private long monthlySequence() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        return stockLiftingRepository.countByMonth(monthStart, monthEnd) + 1;
    }

    private String resolveFinalStatus(String activityType) {
        return switch (activityType) {
            case "MINING_LEASE" -> "MINING LEASE APPROVED";
            case "QUARRY_LEASE" -> "QUARRY LEASE APPROVED";
            case "SURFACE_COLLECTION" -> "SC PERMIT APPROVED";
            case "STOCK_LIFTING" -> "STOCK LIFTING APPROVED";
            default -> "APPROVED";
        };
    }

    private String resolvePrefix(String activityType) {
        int year = Year.now().getValue();
        return switch (activityType) {
            case "MINING_LEASE" -> String.format("MAN-ML-%d-", year);
            case "QUARRY_LEASE" -> String.format("MAN-QL-%d-", year);
            case "SURFACE_COLLECTION" -> String.format("MAN-SC-%d-", year);
            case "STOCK_LIFTING" -> String.format("MAN-SL-%d-", year);
            default -> String.format("MAN-ENTRY-%d-", year);
        };
    }

    // STOCK LIFTING PERMIT NUMBER GENERATOR
    private String generatePermitNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "SLP-" + date + String.format("%05d", monthlySequence());
    }
}
