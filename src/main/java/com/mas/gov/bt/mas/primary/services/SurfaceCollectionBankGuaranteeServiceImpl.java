package com.mas.gov.bt.mas.primary.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.ResubmitBGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SubmitBGRequestDTO;
import com.mas.gov.bt.mas.primary.dto.request.SurfaceCollectionAuctionListResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.BGInstructionViewResponseDTO;
import com.mas.gov.bt.mas.primary.dto.response.BGResponseDTO;
import com.mas.gov.bt.mas.primary.entity.*;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.repository.*;
import com.mas.gov.bt.mas.primary.utility.ErrorCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurfaceCollectionBankGuaranteeServiceImpl
        implements SurfaceCollectionBankGuaranteeService {

    private final SurfaceCollectionBankGuaranteeRepository bgRepository;

    private final SurfaceCollectionAuctionRepository surfaceCollectionAuctionRepository;

    private final ApplicationMasterRepository applicationMasterRepository;

    private final TaskManagementRepository taskManagementRepository;

    private final NotificationClient notificationClient;

    private static final String SERVICE_CODE = "SURFACE_COLLECTION_AUCTION";

    private static final int DEFAULT_TAT_DAYS = 2;

    @Override
    public BGInstructionViewResponseDTO viewInstructions(Long promoterId) {

        SurfaceCollectionBankGuarantee bg = bgRepository.findByPromoterId(promoterId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "BG request not found based on promoter ID"));

        return new BGInstructionViewResponseDTO(
                bg.getAuctionApplication().getId(),
                bg.getAuctionApplication().getApplicationNo(),
                bg.getBgInstruction()
        );
    }

    @Override
    public BGResponseDTO submitBG(
            Long promoterId,
            SubmitBGRequestDTO dto
    ) {

        Optional<SurfaceCollectionAuctionApplication> surfaceCollectionAuctionApplication =
                surfaceCollectionAuctionRepository.findById(dto.getAuctionId());

        SurfaceCollectionAuctionApplication surfaceCollectionAuctionApplication1;

        if  (surfaceCollectionAuctionApplication.isPresent()) {
            surfaceCollectionAuctionApplication1 = surfaceCollectionAuctionApplication.get();
        }else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "No Auction application found.");
        }

        surfaceCollectionAuctionApplication1.setAuctionStatus("BG SUBMITTED");

        surfaceCollectionAuctionRepository.save(surfaceCollectionAuctionApplication1);

        ApplicationMaster applicationMaster = surfaceCollectionAuctionApplication1.getApplicationMaster();
        applicationMaster.setCurrentStatus("BG SUBMITTED");

        applicationMasterRepository.save(applicationMaster);

        createTask(applicationMaster , surfaceCollectionAuctionApplication1,"MINING_DIRECTOR", promoterId, surfaceCollectionAuctionApplication1.getAssignedMdUserId() );

        SurfaceCollectionBankGuarantee bg = new SurfaceCollectionBankGuarantee();

        bg.setAuctionApplication(surfaceCollectionAuctionApplication1);
        bg.setBgFileId(dto.getBgFileId());
        bg.setStatus("BG SUBMITTED");
        bg.setPromoterRemarks(dto.getPromoterRemarks());
        bg.setSubmittedOn(LocalDateTime.now());

        bgRepository.save(bg);

        UserWorkloadProjection userMDDetails =
                surfaceCollectionAuctionRepository.findUserDetails(surfaceCollectionAuctionApplication1.getAssignedMdUserId());


        // Notification and Email
        if (userMDDetails.getEmail() != null) {
            notificationClient.sendSurfaceCollectionAuctionMailToMDBGSubmitted(
                    userMDDetails.getEmail(),
                    userMDDetails.getUsername(),
                    surfaceCollectionAuctionApplication1.getApplicationNo());
        }else {
           throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "User Email ID not found");
        }

        if(userMDDetails.getUserId()!= null) {
            String title = "Bank guarantor details has been submitted Surface Collection Auction.";
            String message = "Bank guarantor details has been submitted for surface collection auction. Application No. "+ surfaceCollectionAuctionApplication1.getApplicationNo();
            String serviceId = "71";
            notificationClient.sendUserNotification(title, message, userMDDetails.getUserId(), serviceId, "STAFF");
        }else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "User ID not found.");
        }

        return map(bg);
    }

    @Transactional
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

    @Override
    public BGResponseDTO resubmitBG(
            Long promoterId,
            ResubmitBGRequestDTO dto
    ) {

        Optional<SurfaceCollectionAuctionApplication> surfaceCollectionAuctionApplication =
                surfaceCollectionAuctionRepository.findById(dto.getAuctionId());

        SurfaceCollectionAuctionApplication surfaceCollectionAuctionApplication1;

        if  (surfaceCollectionAuctionApplication.isPresent() ) {
            surfaceCollectionAuctionApplication1 = surfaceCollectionAuctionApplication.get();
        }else {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "No Auction application found.");
        }

        surfaceCollectionAuctionApplication1.setAuctionStatus("BG RESUBMITTED");

        surfaceCollectionAuctionRepository.save(surfaceCollectionAuctionApplication1);

        ApplicationMaster applicationMaster = surfaceCollectionAuctionApplication1.getApplicationMaster();
        applicationMaster.setCurrentStatus("BG RESUBMITTED");
        applicationMasterRepository.save(applicationMaster);

        createTask(applicationMaster , surfaceCollectionAuctionApplication1,"MINING_DIRECTOR", promoterId, surfaceCollectionAuctionApplication1.getAssignedMdUserId() );

        Optional<SurfaceCollectionBankGuarantee> bg = bgRepository.findByAuctionApplicationId(dto.getAuctionId());

        SurfaceCollectionBankGuarantee surfaceCollectionBankGuarantee = null;
        if(bg.isPresent()) {
            surfaceCollectionBankGuarantee = bg.get();
        }

        assert surfaceCollectionBankGuarantee != null;
        surfaceCollectionBankGuarantee.setBgFileId(dto.getBgFileId());
        surfaceCollectionBankGuarantee.setStatus("BG RESUBMITTED");
        surfaceCollectionBankGuarantee.setPromoterRemarks(dto.getRemarks());
        surfaceCollectionBankGuarantee.setResubmittedOn(LocalDateTime.now());

        bgRepository.save(surfaceCollectionBankGuarantee);

        UserWorkloadProjection userMDDetails =
                surfaceCollectionAuctionRepository.findUserDetails(surfaceCollectionAuctionApplication1.getAssignedMdUserId());


        // Notification and Email
        if (userMDDetails.getEmail() != null) {
            notificationClient.sendSurfaceCollectionAuctionMailToMDBGSubmitted(
                    userMDDetails.getEmail(),
                    userMDDetails.getUsername(),
                    surfaceCollectionAuctionApplication1.getApplicationNo());
        }else {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "User Email address not found.");
        }

        if(userMDDetails.getUserId()!= null) {
            String title = "Bank guarantor details has been resubmitted Surface Collection Auction.";
            String message = "Bank guarantor details has been resubmitted for surface collection auction. Application No. "+ surfaceCollectionAuctionApplication1.getApplicationNo();
            String serviceId = "71";
            notificationClient.sendUserNotification(title, message, userMDDetails.getUserId(), serviceId, "STAFF");
        }else {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "MD User ID not found.");
        }

        return map(surfaceCollectionBankGuarantee);
    }

    @Override
    public Page<SurfaceCollectionAuctionListResponseDTO> getMyApplications(String search, Pageable pageable, Long userId) {
        Page<SurfaceCollectionAuctionApplication> page;

        List<String> archivedStatuses = List.of( "AUCTION_COMPLETED","BG_PENDING", "BG SUBMITTED", "BG RESUBMIT", "BG RESUBMITTED");
        page = surfaceCollectionAuctionRepository.findByBidWinnerPromoterIdAndAuctionStatusIn(userId,archivedStatuses,pageable);

        return page.map(this::mapListResponse);
    }

    @Override
    public Page<SurfaceCollectionAuctionListResponseDTO> getMyArchive(String search, Pageable pageable, Long userId) {
        Page<SurfaceCollectionAuctionApplication> page;

        List<String> archivedStatuses = List.of("APPROVED");
        page = surfaceCollectionAuctionRepository.findByBidWinnerPromoterIdAndAuctionStatusIn(userId,archivedStatuses,pageable);

        return page.map(this::mapListResponse);
    }

    @Override
    public SurfaceCollectionAuctionListResponseDTO viewApplicationDetails(Long userId, String applicationNo) {
        Optional<SurfaceCollectionAuctionApplication> surfaceCollectionAuctionApplication =
                surfaceCollectionAuctionRepository.findByApplicationNoAndBidWinnerPromoterId(applicationNo, userId);

        if (surfaceCollectionAuctionApplication.isEmpty()) {
            surfaceCollectionAuctionApplication =
                    surfaceCollectionAuctionRepository.findByApplicationNoAndCreatedBy(applicationNo, userId);
        }

        SurfaceCollectionAuctionApplication surfaceCollectionAuctionApplication1;
        if (surfaceCollectionAuctionApplication.isEmpty()) {
            throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "Application No. details not found.");
        } else {
            surfaceCollectionAuctionApplication1 = surfaceCollectionAuctionApplication.get();
        }
        SurfaceCollectionAuctionListResponseDTO surfaceCollectionAuctionListResponseDTO = new SurfaceCollectionAuctionListResponseDTO();

        surfaceCollectionAuctionListResponseDTO.setSiteName(surfaceCollectionAuctionApplication1.getSiteName());
        surfaceCollectionAuctionListResponseDTO.setApplicationNo(applicationNo);
        surfaceCollectionAuctionListResponseDTO.setArea(surfaceCollectionAuctionApplication1.getArea());
        surfaceCollectionAuctionListResponseDTO.setAuctionStatus(surfaceCollectionAuctionApplication1.getAuctionStatus());
        surfaceCollectionAuctionListResponseDTO.setPermitGenerated(surfaceCollectionAuctionApplication1.getPermitGenerated());
        surfaceCollectionAuctionListResponseDTO.setBgInstruction(surfaceCollectionAuctionApplication1.getBgInstruction());
        surfaceCollectionAuctionListResponseDTO.setBgRequested(surfaceCollectionAuctionApplication1.getBgRequested());
        surfaceCollectionAuctionListResponseDTO.setLocation( surfaceCollectionAuctionApplication1.getLocation());
        surfaceCollectionAuctionListResponseDTO.setAssignedMdUserId( surfaceCollectionAuctionApplication1.getAssignedMdUserId());
        surfaceCollectionAuctionListResponseDTO.setCreatedOn(surfaceCollectionAuctionApplication1.getCreatedOn());
        surfaceCollectionAuctionListResponseDTO.setEcStatus(surfaceCollectionAuctionApplication1.getEcStatus());
        surfaceCollectionAuctionListResponseDTO.setMaterial(surfaceCollectionAuctionApplication1.getMaterial());
        surfaceCollectionAuctionListResponseDTO.setId( surfaceCollectionAuctionApplication1.getId());

        if(surfaceCollectionAuctionApplication1.getAssignedMdUserId()!=null) {
            UserWorkloadProjection MDUserDetails =
                    surfaceCollectionAuctionRepository
                            .findUserDetails(
                                    surfaceCollectionAuctionApplication1.getAssignedMdUserId()
                            );

            surfaceCollectionAuctionListResponseDTO.setAssignedMdUserName(MDUserDetails.getUsername());
        }else {
            surfaceCollectionAuctionListResponseDTO.setAssignedMdUserName(null);
        }

        return surfaceCollectionAuctionListResponseDTO;
    }

    @Override
    public byte[] generateApplicationQr(Long userId, String applicationNo) throws Exception {

        SurfaceCollectionAuctionListResponseDTO dto =
                viewApplicationDetails(userId, applicationNo);

        String qrContent =
                "Application No: " + dto.getApplicationNo() + "\n" +
                        "Location: " + dto.getLocation() + "\n" +
                        "Material: " + dto.getMaterial() + "\n" +
                        "Area: " + dto.getArea() + "\n" +
                        "Auction Status: " + dto.getAuctionStatus() + "\n" +
                        "Permit Generated: " + dto.getPermitGenerated() + "\n" +
                        "Created On: " + dto.getCreatedOn();

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        BitMatrix bitMatrix = qrCodeWriter.encode(
                qrContent,
                BarcodeFormat.QR_CODE,
                300,
                300
        );

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();

        MatrixToImageWriter.writeToStream(
                bitMatrix,
                "PNG",
                pngOutputStream
        );

        return pngOutputStream.toByteArray();
    }

    @Override
    public byte[] generateQrWithLink(
            Long userId,
            String applicationNo
    ) throws Exception {

        SurfaceCollectionAuctionApplication application =
                surfaceCollectionAuctionRepository
                        .findByApplicationNoAndAuctionStatusAndBidWinnerPromoterId(
                                applicationNo,
                                "APPROVED",
                                userId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCodes.RECORD_NOT_FOUND,
                                        "Application not found"));

        String angularUrl =
                "http://localhost:4200/Verifedscpermitcertificate/" +
                        application.getApplicationNo();

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        BitMatrix bitMatrix = qrCodeWriter.encode(
                angularUrl,
                BarcodeFormat.QR_CODE,
                300,
                300
        );

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        MatrixToImageWriter.writeToStream(
                bitMatrix,
                "PNG",
                outputStream
        );

        return outputStream.toByteArray();
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
}