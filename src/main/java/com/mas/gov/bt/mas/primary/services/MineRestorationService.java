package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.MineRestorationCompletionReportResponse;
import com.mas.gov.bt.mas.primary.dto.response.MineRestorationProgressReportResponse;
import com.mas.gov.bt.mas.primary.dto.response.MineRestorationResponse;
import com.mas.gov.bt.mas.primary.entity.*;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.exception.ResourceNotFoundException;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.repository.*;
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
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MineRestorationService {

    private static final String SERVICE_CODE = "MINE RESTORATION";

    // Statuses
    public static final String STATUS_MRP_DRAFT = "MRP_DRAFT";
    public static final String STATUS_MRP_SUBMITTED = "MRP_SUBMITTED";
    public static final String STATUS_MRP_REVISION_REQUESTED = "MRP_REVISION_REQUESTED";
    public static final String STATUS_MRP_APPROVED = "MRP_APPROVED";
    public static final String STATUS_MRP_REJECTED = "MRP_REJECTED";
    public static final String STATUS_RESTORATION_IN_PROGRESS = "RESTORATION_IN_PROGRESS";
    public static final String STATUS_PROGRESS_REPORT_SUBMITTED = "PROGRESS_REPORT_SUBMITTED";
    public static final String STATUS_COMPLETION_REPORT_SUBMITTED = "COMPLETION_REPORT_SUBMITTED";
    public static final String STATUS_ERB_RELEASED = "ERB_RELEASED";
    public static final String STATUS_ERB_UTILIZED = "ERB_UTILIZED";

    private final MineRestorationApplicationRepository restorationApplicationRepository;
    private final MineRestorationProgressReportRepository progressReportRepository;
    private final MineRestorationCompletionReportRepository completionReportRepository;
    private final MiningLeaseApplicationRepository miningLeaseApplicationRepository;
    private final NotificationClient notificationClient;

    // =====================================================
    // PROMOTER — MRP Submission
    // =====================================================

    @Transactional
    public MineRestorationResponse submitMRP(MineRestorationMRPRequest request, Long userId) {
        MiningLeaseApplication lease = miningLeaseApplicationRepository
                .findByApplicationNumber(request.getMiningLeaseApplicationNumber())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        MineRestorationApplication restoration = new MineRestorationApplication();
        restoration.setMiningLeaseApplicationNumber(request.getMiningLeaseApplicationNumber());
        restoration.setRestorationType(request.getRestorationType());
        restoration.setApplicantUserId(userId);
        restoration.setApplicantName(lease.getApplicantName());
        restoration.setApplicantEmail(lease.getApplicantEmail());
        restoration.setApplicantContact(lease.getApplicantContact());
        restoration.setNameOfMine(lease.getPlaceOfMiningActivity());
        restoration.setLeaseAreaAcres(lease.getTotalLand());
        restoration.setLeaseEndDate(lease.getLeaseEndDate());
        restoration.setMrpDocId(request.getMrpDocId());
        restoration.setCreatedBy(userId);

        boolean isDraft = "DRAFT".equalsIgnoreCase(request.getStatus());
        if (isDraft) {
            restoration.setCurrentStatus(STATUS_MRP_DRAFT);
        } else {
            restoration.setCurrentStatus(STATUS_MRP_SUBMITTED);
            restoration.setMrpSubmittedAt(LocalDateTime.now());
            restoration.setApplicationNumber(generateApplicationNumber());

            // Auto-assign ME by workload
            UserWorkloadProjection assignedME = restorationApplicationRepository.findMEWithLeastWorkload();
            if (assignedME != null) {
                restoration.setAssignedMeUserId(assignedME.getUserId());
                notificationClient.sendAssignmentNotification(
                        assignedME.getEmail(),
                        assignedME.getUsername(),
                        restoration.getApplicationNumber(),
                        "Mine Restoration Plan Review"
                );
                notificationClient.sendUserNotification(
                        "New Mine Restoration Plan assigned",
                        "A Mine Restoration Plan has been assigned to you for review.",
                        assignedME.getUserId(),
                        SERVICE_CODE
                );
            }
        }

        if (restoration.getApplicationNumber() == null) {
            restoration.setApplicationNumber(generateApplicationNumber());
        }

        restorationApplicationRepository.save(restoration);
        return toResponse(restoration);
    }

    @Transactional
    public MineRestorationResponse resubmitMRP(MineRestorationMRPResubmitRequest request, Long userId) {
        MineRestorationApplication restoration = findById(request.getRestorationApplicationId());

        if (!STATUS_MRP_REVISION_REQUESTED.equals(restoration.getCurrentStatus())
                && !STATUS_MRP_DRAFT.equals(restoration.getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.INVALID_STATE);
        }

        restoration.setMrpDocId(request.getMrpDocId());
        restoration.setCurrentStatus(STATUS_MRP_SUBMITTED);
        restoration.setMrpSubmittedAt(LocalDateTime.now());
        restoration.setUpdatedBy(userId);

        restorationApplicationRepository.save(restoration);

        // Notify ME
        if (restoration.getAssignedMeUserId() != null) {
            notificationClient.sendUserNotification(
                    "Revised MRP submitted",
                    "The promoter has resubmitted the Mining Restoration Plan for application "
                            + restoration.getApplicationNumber(),
                    restoration.getAssignedMeUserId(),
                    SERVICE_CODE
            );
        }
        return toResponse(restoration);
    }

    // =====================================================
    // PROMOTER — Progress Report
    // =====================================================

    @Transactional
    public MineRestorationProgressReportResponse submitProgressReport(
            MineRestorationProgressReportRequest request, Long userId) {

        MineRestorationApplication restoration = restorationApplicationRepository
                .findByApplicationNumber(request.getRestorationApplicationNumber())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        if (!STATUS_RESTORATION_IN_PROGRESS.equals(restoration.getCurrentStatus())
                && !STATUS_PROGRESS_REPORT_SUBMITTED.equals(restoration.getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.INVALID_STATE);
        }

        MineRestorationProgressReport report = new MineRestorationProgressReport();
        report.setRestorationApplicationNumber(request.getRestorationApplicationNumber());
        report.setNameOfMine(restoration.getNameOfMine());
        report.setLeaseAreaAcres(restoration.getLeaseAreaAcres());
        report.setNameOfLessee(restoration.getApplicantName());
        report.setLocationImageDocId(request.getLocationImageDocId());
        report.setStartDateOfMineRestoration(request.getStartDateOfMineRestoration());
        report.setDateOfProgressReport(request.getDateOfProgressReport());
        report.setActivityDescription(request.getActivityDescription());
        report.setFinancialProgress(request.getFinancialProgress());
        report.setPhysicalProgress(request.getPhysicalProgress());
        report.setPictorialEvidenceDocId(request.getPictorialEvidenceDocId());
        report.setSubmittedBy(userId);

        boolean isDraft = "DRAFT".equalsIgnoreCase(request.getStatus());
        if (isDraft) {
            report.setStatus("DRAFT");
        } else {
            long count = progressReportRepository.countSubmittedReports(request.getRestorationApplicationNumber());
            report.setProgressReportNumber((int) count + 1);
            report.setStatus("SUBMITTED");
            restoration.setCurrentStatus(STATUS_PROGRESS_REPORT_SUBMITTED);
            restorationApplicationRepository.save(restoration);

            // Notify ME
            if (restoration.getAssignedMeUserId() != null) {
                notificationClient.sendUserNotification(
                        "Progress Report Submitted",
                        "Progress report #" + report.getProgressReportNumber()
                                + " submitted for application " + restoration.getApplicationNumber(),
                        restoration.getAssignedMeUserId(),
                        SERVICE_CODE
                );
            }
        }

        progressReportRepository.save(report);
        return toProgressReportResponse(report);
    }

    // =====================================================
    // PROMOTER — Completion Report
    // =====================================================

    @Transactional
    public MineRestorationCompletionReportResponse submitCompletionReport(
            MineRestorationCompletionReportRequest request, Long userId) {

        MineRestorationApplication restoration = restorationApplicationRepository
                .findByApplicationNumber(request.getRestorationApplicationNumber())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        if (!STATUS_PROGRESS_REPORT_SUBMITTED.equals(restoration.getCurrentStatus())
                && !STATUS_RESTORATION_IN_PROGRESS.equals(restoration.getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.INVALID_STATE);
        }

        MineRestorationCompletionReport report = new MineRestorationCompletionReport();
        report.setRestorationApplicationNumber(request.getRestorationApplicationNumber());
        report.setNameOfMine(restoration.getNameOfMine());
        report.setLeaseAreaAcres(restoration.getLeaseAreaAcres());
        report.setNameOfLessee(restoration.getApplicantName());
        report.setLocationImageDocId(request.getLocationImageDocId());
        report.setActivitiesUndertaken(request.getActivitiesUndertaken());
        report.setRemarks(request.getRemarks());
        report.setPictorialEvidenceDocId(request.getPictorialEvidenceDocId());
        report.setMapsAndPlansDocId(request.getMapsAndPlansDocId());
        report.setOtherDocId(request.getOtherDocId());
        report.setSubmittedBy(userId);

        boolean isDraft = "DRAFT".equalsIgnoreCase(request.getStatus());
        if (isDraft) {
            report.setStatus("DRAFT");
        } else {
            report.setStatus("SUBMITTED");
            restoration.setCurrentStatus(STATUS_COMPLETION_REPORT_SUBMITTED);
            restorationApplicationRepository.save(restoration);

            if (restoration.getAssignedMeUserId() != null) {
                notificationClient.sendUserNotification(
                        "Restoration Completion Report Submitted",
                        "The promoter has submitted the Restoration Completion Report for application "
                                + restoration.getApplicationNumber(),
                        restoration.getAssignedMeUserId(),
                        SERVICE_CODE
                );
            }
        }

        completionReportRepository.save(report);
        return toCompletionReportResponse(report);
    }

    // =====================================================
    // PROMOTER — Queries
    // =====================================================

    public SuccessResponse<List<MineRestorationResponse>> getMyApplications(
            Long userId, String search, Pageable pageable) {
        Page<MineRestorationApplication> page;
        if (search != null && !search.isBlank()) {
            page = restorationApplicationRepository.findByApplicantUserIdAndSearch(userId, search, pageable);
        } else {
            page = restorationApplicationRepository.findByApplicantUserId(userId, pageable);
        }
        return SuccessResponse.fromPage("Applications retrieved successfully",
                page.map(this::toResponse));
    }

    public MineRestorationResponse getApplicationById(Long id) {
        return toResponse(findById(id));
    }

    public SuccessResponse<List<MineRestorationProgressReportResponse>> getProgressReports(
            String applicationNumber, Pageable pageable) {
        Page<MineRestorationProgressReport> page =
                progressReportRepository.findByRestorationApplicationNumber(applicationNumber, pageable);
        return SuccessResponse.fromPage("Progress reports retrieved successfully",
                page.map(this::toProgressReportResponse));
    }

    public MineRestorationCompletionReportResponse getCompletionReport(String applicationNumber) {
        MineRestorationCompletionReport report = completionReportRepository
                .findByRestorationApplicationNumber(applicationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Completion report not found"));
        return toCompletionReportResponse(report);
    }

    // =====================================================
    // MINING ENGINEER — MRP Review
    // =====================================================

    @Transactional
    public MineRestorationResponse reviewMRP(ReviewMineRestorationMRPRequest request, Long userId) {
        MineRestorationApplication restoration = findById(request.getRestorationApplicationId());

        if (!STATUS_MRP_SUBMITTED.equals(restoration.getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.INVALID_STATE);
        }

        restoration.setRemarksME(request.getRemarks());
        restoration.setMeReviewedAt(LocalDateTime.now());
        restoration.setUpdatedBy(userId);

        switch (request.getDecision().toUpperCase()) {
            case "APPROVED" -> {
                restoration.setCurrentStatus(STATUS_MRP_APPROVED);
                // Issue work order
                restoration.setWorkOrderIssuedAt(LocalDateTime.now());
                notificationClient.sendStatusUpdateNotification(
                        restoration.getApplicantEmail(),
                        restoration.getApplicantName(),
                        restoration.getApplicationNumber(),
                        "MRP Approved — Work Order Issued",
                        "Your Mining Restoration Plan has been approved. A work order has been issued to begin restoration."
                );
                notificationClient.sendUserNotification(
                        "MRP Approved — Work Order Issued",
                        "Your MRP for application " + restoration.getApplicationNumber()
                                + " has been approved. Work order has been issued.",
                        restoration.getApplicantUserId(),
                        SERVICE_CODE
                );
            }
            case "REVISION_REQUESTED" -> {
                restoration.setCurrentStatus(STATUS_MRP_REVISION_REQUESTED);
                notificationClient.sendRevisionRequestNotification(
                        restoration.getApplicantEmail(),
                        restoration.getApplicantName(),
                        restoration.getApplicationNumber(),
                        "MRP Review",
                        request.getRemarks()
                );
                notificationClient.sendUserNotification(
                        "MRP Revision Requested",
                        "Please revise and resubmit the MRP for application "
                                + restoration.getApplicationNumber() + ". Remarks: " + request.getRemarks(),
                        restoration.getApplicantUserId(),
                        SERVICE_CODE
                );
            }
            case "REJECTED" -> {
                restoration.setCurrentStatus(STATUS_MRP_REJECTED);
                restoration.setRejectionReason(request.getRemarks());
                notificationClient.sendRejectionNotification(
                        restoration.getApplicantEmail(),
                        restoration.getApplicantName(),
                        restoration.getApplicationNumber(),
                        request.getRemarks()
                );
                notificationClient.sendUserNotification(
                        "MRP Rejected",
                        "Your MRP for application " + restoration.getApplicationNumber()
                                + " has been rejected. Reason: " + request.getRemarks(),
                        restoration.getApplicantUserId(),
                        SERVICE_CODE
                );
            }
            default -> throw new BusinessException(ErrorCodes.INVALID_INPUT_DATA);
        }

        restorationApplicationRepository.save(restoration);
        return toResponse(restoration);
    }

    @Transactional
    public MineRestorationResponse uploadWorkOrder(Long restorationApplicationId, String workOrderDocId, Long userId) {
        MineRestorationApplication restoration = findById(restorationApplicationId);

        if (!STATUS_MRP_APPROVED.equals(restoration.getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.INVALID_STATE);
        }

        restoration.setWorkOrderDocId(workOrderDocId);
        restoration.setWorkOrderIssuedAt(LocalDateTime.now());
        restoration.setCurrentStatus(STATUS_RESTORATION_IN_PROGRESS);
        restoration.setUpdatedBy(userId);

        restorationApplicationRepository.save(restoration);

        notificationClient.sendWorkOrderNotification(
                restoration.getApplicantEmail(),
                restoration.getApplicantName(),
                restoration.getApplicationNumber()
        );
        notificationClient.sendUserNotification(
                "Work Order Issued",
                "Work order has been issued for your restoration application "
                        + restoration.getApplicationNumber() + ". You may now begin the restoration process.",
                restoration.getApplicantUserId(),
                SERVICE_CODE
        );

        return toResponse(restoration);
    }

    @Transactional
    public MineRestorationProgressReportResponse reviewProgressReport(
            ReviewMineRestorationProgressRequest request, Long userId) {

        MineRestorationProgressReport report = progressReportRepository.findById(request.getProgressReportId())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        MineRestorationApplication restoration = restorationApplicationRepository
                .findByApplicationNumber(report.getRestorationApplicationNumber())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        if (!"VERIFICATION_SUBMITTED".equals(report.getStatus())) {
            throw new BusinessException(ErrorCodes.INVALID_STATE);
        }

        report.setMeRemarks(request.getRemarks());
        report.setMeReviewedAt(LocalDateTime.now());

        switch (request.getDecision().toUpperCase()) {
            case "REVIEWED" -> {
                report.setStatus("ME_REVIEWED");
                restoration.setCurrentStatus(STATUS_RESTORATION_IN_PROGRESS);
                notificationClient.sendUserNotification(
                        "Progress Report Reviewed",
                        "Your progress report for application " + restoration.getApplicationNumber()
                                + " has been reviewed. Continue restoration.",
                        restoration.getApplicantUserId(),
                        SERVICE_CODE
                );
            }
            case "COMPLETION_REQUESTED" -> {
                report.setStatus("COMPLETION_REQUESTED");
                restoration.setCurrentStatus(STATUS_RESTORATION_IN_PROGRESS);
                notificationClient.sendUserNotification(
                        "Please Submit Restoration Completion Report",
                        "The Mining Engineer has confirmed restoration is complete. Please submit the "
                                + "Restoration Completion Report for application " + restoration.getApplicationNumber(),
                        restoration.getApplicantUserId(),
                        SERVICE_CODE
                );
            }
            default -> throw new BusinessException(ErrorCodes.INVALID_INPUT_DATA);
        }

        progressReportRepository.save(report);
        restorationApplicationRepository.save(restoration);
        return toProgressReportResponse(report);
    }

    @Transactional
    public MineRestorationResponse reviewCompletionReport(
            ReviewMineRestorationCompletionRequest request, Long userId) {

        MineRestorationApplication restoration = findById(request.getRestorationApplicationId());

        if (!STATUS_COMPLETION_REPORT_SUBMITTED.equals(restoration.getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.INVALID_STATE);
        }

        MineRestorationCompletionReport completionReport = completionReportRepository
                .findByRestorationApplicationNumber(restoration.getApplicationNumber())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        completionReport.setMeRemarks(request.getRemarks());
        completionReport.setMeReviewedAt(LocalDateTime.now());
        restoration.setErbDecision(request.getDecision().toUpperCase());
        restoration.setErbDecidedAt(LocalDateTime.now());
        restoration.setErbRemarks(request.getRemarks());
        restoration.setUpdatedBy(userId);

        switch (request.getDecision().toUpperCase()) {
            case "ERB_RELEASED" -> {
                completionReport.setStatus("APPROVED");
                restoration.setCurrentStatus(STATUS_ERB_RELEASED);
                notificationClient.sendApprovalNotification(
                        restoration.getApplicantEmail(),
                        restoration.getApplicantName(),
                        restoration.getApplicationNumber()
                );
                notificationClient.sendUserNotification(
                        "Restoration Approved — ERB Released",
                        "Your restoration work for application " + restoration.getApplicationNumber()
                                + " has been approved. The Environmental Restoration Bond (ERB) will be refunded through BIRMS.",
                        restoration.getApplicantUserId(),
                        SERVICE_CODE
                );
            }
            case "ERB_UTILIZED" -> {
                completionReport.setStatus("REJECTED");
                restoration.setCurrentStatus(STATUS_ERB_UTILIZED);
                notificationClient.sendRejectionNotification(
                        restoration.getApplicantEmail(),
                        restoration.getApplicantName(),
                        restoration.getApplicationNumber(),
                        request.getRemarks() != null ? request.getRemarks()
                                : "Restoration not satisfactory. ERB will be utilized by DGM."
                );
                notificationClient.sendUserNotification(
                        "ERB Utilized — Restoration Not Satisfactory",
                        "The restoration work for application " + restoration.getApplicationNumber()
                                + " was not satisfactory. The ERB will be utilized by DGM. Remarks: " + request.getRemarks(),
                        restoration.getApplicantUserId(),
                        SERVICE_CODE
                );
            }
            default -> throw new BusinessException(ErrorCodes.INVALID_INPUT_DATA);
        }

        completionReportRepository.save(completionReport);
        restorationApplicationRepository.save(restoration);
        return toResponse(restoration);
    }

    // =====================================================
    // MINING ENGINEER — Queries
    // =====================================================

    public SuccessResponse<List<MineRestorationResponse>> getAssignedToME(
            Long userId, String search, Pageable pageable) {
        Page<MineRestorationApplication> page;
        if (search != null && !search.isBlank()) {
            page = restorationApplicationRepository.findByAssignedMeUserIdAndSearch(userId, search, pageable);
        } else {
            page = restorationApplicationRepository.findByAssignedMeUserId(userId, pageable);
        }
        return SuccessResponse.fromPage("Applications retrieved successfully",
                page.map(this::toResponse));
    }

    // =====================================================
    // DIRECTOR — Assign & Queries
    // =====================================================

    @Transactional
    public MineRestorationResponse assignApplicationDirector(RestorationTaskAssignDirector request, Long userId) {
        MineRestorationApplication restoration = findById(request.getApplicationId());

        if (request.getMiningEngineerId() == null) {
            throw new BusinessException(ErrorCodes.INVALID_INPUT_DATA);
        }

        restoration.setAssignedMeUserId(request.getMiningEngineerId());
        restoration.setUpdatedBy(userId);
        restorationApplicationRepository.save(restoration);

        UserWorkloadProjection me = restorationApplicationRepository
                .findUserDetailsById(request.getMiningEngineerId());

        if (me != null) {
            notificationClient.sendAssignmentNotification(
                    me.getEmail(),
                    me.getUsername(),
                    restoration.getApplicationNumber(),
                    "Mine Restoration Plan Review"
            );
            notificationClient.sendUserNotification(
                    "Mine Restoration Application Assigned",
                    "A Mine Restoration application " + restoration.getApplicationNumber()
                            + " has been assigned to you for review.",
                    request.getMiningEngineerId(),
                    SERVICE_CODE
            );
        }

        return toResponse(restoration);
    }

    public SuccessResponse<List<MineRestorationResponse>> getAllApplicationsForDirector(
            String search, Pageable pageable) {
        Page<MineRestorationApplication> page;
        if (search != null && !search.isBlank()) {
            page = restorationApplicationRepository.findAllWithSearch(search, pageable);
        } else {
            page = restorationApplicationRepository.findAll(pageable);
        }
        return SuccessResponse.fromPage("Applications retrieved successfully",
                page.map(this::toResponse));
    }

    // =====================================================
    // RC/MI — Verification Report
    // =====================================================

    @Transactional
    public MineRestorationProgressReportResponse submitVerificationReport(
            MineRestorationVerificationReportRequest request, Long userId) {

        MineRestorationProgressReport report = progressReportRepository.findById(request.getProgressReportId())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        if (!"SUBMITTED".equals(report.getStatus())) {
            throw new BusinessException(ErrorCodes.INVALID_STATE);
        }

        report.setAssignedRcUserId(userId);
        report.setVerificationReportDocId(request.getVerificationReportDocId());
        report.setVerificationRemarks(request.getRemarks());
        report.setVerificationSubmittedAt(LocalDateTime.now());
        report.setStatus("VERIFICATION_SUBMITTED");

        progressReportRepository.save(report);

        // Notify ME
        MineRestorationApplication restoration = restorationApplicationRepository
                .findByApplicationNumber(report.getRestorationApplicationNumber())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        if (restoration.getAssignedMeUserId() != null) {
            notificationClient.sendUserNotification(
                    "Verification Report Submitted",
                    "RC/MI has submitted the verification report for progress report #"
                            + report.getProgressReportNumber() + " of application "
                            + restoration.getApplicationNumber(),
                    restoration.getAssignedMeUserId(),
                    SERVICE_CODE
            );
        }

        return toProgressReportResponse(report);
    }

    public SuccessResponse<List<MineRestorationResponse>> getActiveApplicationsForRC(
            String search, Pageable pageable) {
        Page<MineRestorationApplication> page;
        if (search != null && !search.isBlank()) {
            page = restorationApplicationRepository.findActiveForRCWithSearch(search, pageable);
        } else {
            page = restorationApplicationRepository.findActiveForRC(pageable);
        }
        return SuccessResponse.fromPage("Applications retrieved successfully",
                page.map(this::toResponse));
    }

    public SuccessResponse<List<MineRestorationProgressReportResponse>> getProgressReportsForRC(
            Long userId, String search, Pageable pageable) {
        Page<MineRestorationProgressReport> page;
        if (search != null && !search.isBlank()) {
            page = progressReportRepository.findByAssignedRcUserIdAndSearch(userId, search, pageable);
        } else {
            page = progressReportRepository.findByAssignedRcUserId(userId, pageable);
        }
        return SuccessResponse.fromPage("Progress reports retrieved successfully",
                page.map(this::toProgressReportResponse));
    }

    // =====================================================
    // Helpers
    // =====================================================

    private MineRestorationApplication findById(Long id) {
        return restorationApplicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));
    }

    private synchronized String generateApplicationNumber() {
        String year = String.valueOf(Year.now().getValue());
        String prefix = "MR-" + year + "-";
        Integer maxSeq = restorationApplicationRepository.findMaxSequenceByPrefix(prefix);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;
        return prefix + String.format("%05d", nextSeq);
    }

    private MineRestorationResponse toResponse(MineRestorationApplication app) {
        MineRestorationResponse res = new MineRestorationResponse();
        res.setId(app.getId());
        res.setApplicationNumber(app.getApplicationNumber());
        res.setMiningLeaseApplicationNumber(app.getMiningLeaseApplicationNumber());
        res.setRestorationType(app.getRestorationType());
        res.setApplicantUserId(app.getApplicantUserId());
        res.setApplicantName(app.getApplicantName());
        res.setApplicantEmail(app.getApplicantEmail());
        res.setApplicantContact(app.getApplicantContact());
        res.setNameOfMine(app.getNameOfMine());
        res.setLeaseAreaAcres(app.getLeaseAreaAcres());
        res.setDzongkhag(app.getDzongkhag());
        res.setGewog(app.getGewog());
        res.setLeaseEndDate(app.getLeaseEndDate());
        res.setMrpDocId(app.getMrpDocId());
        res.setMrpSubmittedAt(app.getMrpSubmittedAt());
        res.setWorkOrderDocId(app.getWorkOrderDocId());
        res.setWorkOrderIssuedAt(app.getWorkOrderIssuedAt());
        res.setAssignedMeUserId(app.getAssignedMeUserId());
        res.setRemarksME(app.getRemarksME());
        res.setMeReviewedAt(app.getMeReviewedAt());
        res.setErbDecision(app.getErbDecision());
        res.setErbDecidedAt(app.getErbDecidedAt());
        res.setErbRemarks(app.getErbRemarks());
        res.setCurrentStatus(app.getCurrentStatus());
        res.setCurrentStatusDisplayName(getStatusDisplayName(app.getCurrentStatus()));
        res.setRejectionReason(app.getRejectionReason());
        res.setCreatedBy(app.getCreatedBy());
        res.setCreatedOn(app.getCreatedOn());
        res.setUpdatedOn(app.getUpdatedOn());
        return res;
    }

    private MineRestorationProgressReportResponse toProgressReportResponse(MineRestorationProgressReport r) {
        MineRestorationProgressReportResponse res = new MineRestorationProgressReportResponse();
        res.setId(r.getId());
        res.setRestorationApplicationNumber(r.getRestorationApplicationNumber());
        res.setProgressReportNumber(r.getProgressReportNumber());
        res.setNameOfMine(r.getNameOfMine());
        res.setLeaseAreaAcres(r.getLeaseAreaAcres());
        res.setNameOfLessee(r.getNameOfLessee());
        res.setLocationImageDocId(r.getLocationImageDocId());
        res.setStartDateOfMineRestoration(r.getStartDateOfMineRestoration());
        res.setDateOfProgressReport(r.getDateOfProgressReport());
        res.setActivityDescription(r.getActivityDescription());
        res.setFinancialProgress(r.getFinancialProgress());
        res.setPhysicalProgress(r.getPhysicalProgress());
        res.setPictorialEvidenceDocId(r.getPictorialEvidenceDocId());
        res.setAssignedRcUserId(r.getAssignedRcUserId());
        res.setVerificationReportDocId(r.getVerificationReportDocId());
        res.setVerificationSubmittedAt(r.getVerificationSubmittedAt());
        res.setVerificationRemarks(r.getVerificationRemarks());
        res.setMeRemarks(r.getMeRemarks());
        res.setMeReviewedAt(r.getMeReviewedAt());
        res.setStatus(r.getStatus());
        res.setCreatedOn(r.getCreatedOn());
        return res;
    }

    private MineRestorationCompletionReportResponse toCompletionReportResponse(MineRestorationCompletionReport r) {
        MineRestorationCompletionReportResponse res = new MineRestorationCompletionReportResponse();
        res.setId(r.getId());
        res.setRestorationApplicationNumber(r.getRestorationApplicationNumber());
        res.setNameOfMine(r.getNameOfMine());
        res.setLeaseAreaAcres(r.getLeaseAreaAcres());
        res.setNameOfLessee(r.getNameOfLessee());
        res.setLocationImageDocId(r.getLocationImageDocId());
        res.setActivitiesUndertaken(r.getActivitiesUndertaken());
        res.setRemarks(r.getRemarks());
        res.setPictorialEvidenceDocId(r.getPictorialEvidenceDocId());
        res.setMapsAndPlansDocId(r.getMapsAndPlansDocId());
        res.setOtherDocId(r.getOtherDocId());
        res.setMeRemarks(r.getMeRemarks());
        res.setMeReviewedAt(r.getMeReviewedAt());
        res.setStatus(r.getStatus());
        res.setCreatedOn(r.getCreatedOn());
        return res;
    }

    private String getStatusDisplayName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "MRP_DRAFT" -> "MRP Draft";
            case "MRP_SUBMITTED" -> "MRP Submitted";
            case "MRP_REVISION_REQUESTED" -> "MRP Revision Requested";
            case "MRP_APPROVED" -> "MRP Approved";
            case "MRP_REJECTED" -> "MRP Rejected";
            case "RESTORATION_IN_PROGRESS" -> "Restoration In Progress";
            case "PROGRESS_REPORT_SUBMITTED" -> "Progress Report Submitted";
            case "COMPLETION_REPORT_SUBMITTED" -> "Completion Report Submitted";
            case "ERB_RELEASED" -> "ERB Released";
            case "ERB_UTILIZED" -> "ERB Utilized by DGM";
            default -> status.replace("_", " ");
        };
    }
}
