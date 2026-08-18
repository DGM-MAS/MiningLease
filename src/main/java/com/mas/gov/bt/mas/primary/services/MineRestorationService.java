package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.dto.UserWorkloadProjection;
import com.mas.gov.bt.mas.primary.dto.request.*;
import com.mas.gov.bt.mas.primary.dto.response.MineRestorationCompletionReportResponse;
import com.mas.gov.bt.mas.primary.dto.response.MineRestorationProgressReportResponse;
import com.mas.gov.bt.mas.primary.dto.response.MineRestorationResponse;
import com.mas.gov.bt.mas.primary.entity.*;
import com.mas.gov.bt.mas.primary.exception.BusinessException;
import com.mas.gov.bt.mas.primary.exception.ResourceNotFoundException;
import com.mas.gov.bt.mas.primary.integration.MenuIdResolver;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class MineRestorationService {

    private static final String SERVICE_CODE = "MINE RESTORATION";

    // Real sidebar menu ids (permissions.id) per recipient role for this service — used to target
    // notification.serviceId so the sidebar dot/click-through lands on the correct menu item.
    // NOT the same thing as SERVICE_CODE above, which is an unrelated t_application_master.service_code value.
    // No ServiceMenuMapping.java entry exists for this service; these were resolved directly against
    // the live `permissions` table (parent_menu_id = 120, "Mine Restoration") — flagged for human review.
    private String MENU_ID_APPLICANT       = "121"; // "Promoter Application List" (/MineRestorationappliantlist)
    private String MENU_ID_MINING_ENGINEER = "122"; // "Mining Division Application List" (/MineRestorationminingdivisionapplist)
    private String MENU_ID_RC              = "123"; // "Regional Coordinator Application List" (/MineRestorationregionalCoordinatorapplist)

    // Statuses
    public static final String STATUS_MRP_DRAFT = "MRP_DRAFT";
    public static final String STATUS_PROGRESS_REPORT_DRAFT = "PROGRESS_REPORT_DRAFT";
    public static final String STATUS_COMPLETION_REPORT_DRAFT = "COMPLETION_REPORT_DRAFT";
    public static final String STATUS_MRP_SUBMITTED = "MRP_SUBMITTED";
    public static final String STATUS_MRP_REVISION_REQUESTED = "MRP_REVISION_REQUESTED";
    public static final String STATUS_MRP_APPROVED = "MRP_APPROVED";
    public static final String STATUS_MRP_REJECTED = "MRP_REJECTED";
    public static final String STATUS_RESTORATION_IN_PROGRESS = "RESTORATION_IN_PROGRESS";
    public static final String STATUS_PROGRESS_REPORT_SUBMITTED = "PROGRESS_REPORT_SUBMITTED";
    public static final String STATUS_COMPLETION_REPORT_REQUESTED = "COMPLETION_REPORT_REQUESTED";
    public static final String STATUS_COMPLETION_REPORT_SUBMITTED = "COMPLETION_REPORT_SUBMITTED";
    public static final String STATUS_VERIFICATION_SUBMITTED = "VERIFICATION_SUBMITTED";
    public static final String STATUS_ERB_APPROVED = "ERB_APPROVED";
    public static final String STATUS_ERB_RELEASED = "ERB_RELEASED";
    public static final String STATUS_ERB_UTILIZED = "ERB_UTILIZED";

    private final MineRestorationApplicationRepository restorationApplicationRepository;
    private final MineRestorationProgressReportRepository progressReportRepository;
    private final MineRestorationCompletionReportRepository completionReportRepository;
    private final MiningLeaseApplicationRepository miningLeaseApplicationRepository;
    private final NotificationClient notificationClient;

    private final MenuIdResolver menuIdResolver;

    private final QuarryLeaseApplicationRepository queryLeaseApplicationRepository;

    private final HouseholdPermitThresholdRepository householdPermitThresholdRepository;

    @jakarta.annotation.PostConstruct
    private void resolveMenuIds() {
        MENU_ID_APPLICANT       = menuIdResolver.resolve("/MineRestorationappliantlist", MENU_ID_APPLICANT);
        MENU_ID_MINING_ENGINEER = menuIdResolver.resolve("/MineRestorationminingdivisionapplist", MENU_ID_MINING_ENGINEER);
        MENU_ID_RC              = menuIdResolver.resolve("/MineRestorationregionalCoordinatorapplist", MENU_ID_RC);
    }

    // =====================================================
    // PROMOTER — MRP Submission
    // =====================================================

    @Transactional
    public MineRestorationResponse submitMRP(MineRestorationMRPRequest request, Long userId) {

        MiningLeaseApplication miningLeaseApplication = null;
        QuarryLeaseApplication quarryLeaseApplication = null;

        // =========================================================
        // FIND MINING / QUARRY APPLICATION
        // =========================================================

        Long regionId = null;

        Optional<MiningLeaseApplication> lease = miningLeaseApplicationRepository
                .findByApplicationNumber(request.getMiningLeaseApplicationNumber());

        if (lease.isPresent()){
            miningLeaseApplication = lease.get();
        }else{
            Optional<QuarryLeaseApplication> quarry = queryLeaseApplicationRepository.findByApplicationNumber(request.getMiningLeaseApplicationNumber());

            if(quarry.isPresent()){
                quarryLeaseApplication = quarry.get();
            }else {
                throw new BusinessException(ErrorCodes.RECORD_NOT_FOUND, "The application number is not present in Quarry and Mining lease table.");
            }
        }

        // =========================================================
        // LOAD / CREATE RESTORATION APPLICATION
        // =========================================================
        // Load existing draft if ID provided, otherwise create new
        MineRestorationApplication restoration;

        if (request.getRestorationApplicationId() != null) {
            restoration = findById(request.getRestorationApplicationId());
            if (!STATUS_MRP_DRAFT.equals(restoration.getCurrentStatus())) {
                throw new BusinessException(ErrorCodes.INVALID_STATE);
            }
            restoration.setUpdatedBy(userId);
        } else {
            restoration = new MineRestorationApplication();
            restoration.setMiningLeaseApplicationNumber(request.getMiningLeaseApplicationNumber());
            restoration.setRestorationType(request.getRestorationType());
            restoration.setApplicantUserId(userId);

            if (miningLeaseApplication != null){
                restoration.setApplicantName(miningLeaseApplication.getApplicantName());
                restoration.setApplicantEmail(miningLeaseApplication.getApplicantEmail());
                restoration.setApplicantContact(miningLeaseApplication.getApplicantContact());
                restoration.setNameOfMine(miningLeaseApplication.getNameOfMine());
                restoration.setLeaseAreaAcres(miningLeaseApplication.getTotalLand());
                restoration.setLeaseEndDate(miningLeaseApplication.getLeaseEndDate());
                restoration.setRegionId(miningLeaseApplication.getRegionId());
                regionId = miningLeaseApplication.getRegionId();
            }

            if (quarryLeaseApplication != null){
                restoration.setApplicantName(quarryLeaseApplication.getApplicantName());
                restoration.setApplicantEmail(quarryLeaseApplication.getApplicantEmail());
                restoration.setApplicantContact(quarryLeaseApplication.getApplicantContact());
                restoration.setNameOfMine(quarryLeaseApplication.getNameOfQuarry());
                restoration.setLeaseAreaAcres(quarryLeaseApplication.getTotalLand());
                restoration.setLeaseEndDate(quarryLeaseApplication.getLeaseEndDate());
                restoration.setRegionId(quarryLeaseApplication.getRegionId());
                regionId = quarryLeaseApplication.getRegionId();
            }

            restoration.setCreatedBy(userId);

        }

        // =========================================================
        // DOCUMENT
        // =========================================================
        restoration.setMrpDocId(request.getMrpDocId());

        // =========================================================
        // DRAFT / SUBMISSION
        // =========================================================
        boolean isDraft = "DRAFT".equalsIgnoreCase(request.getStatus());
        if (isDraft) {
            restoration.setCurrentStatus(STATUS_MRP_DRAFT);
        } else {

            // Validate TERMINATED status + Application Master status
            // + two-month submission deadline
            LocalDateTime deadline =
                    validateMRPSubmissionEligibility(
                            miningLeaseApplication,
                            quarryLeaseApplication
                    );

            restoration.setCurrentStatus(STATUS_MRP_SUBMITTED);
            restoration.setMrpSubmittedAt(LocalDateTime.now());

            if (restoration.getApplicationNumber() == null) {
                restoration.setApplicationNumber(
                        generateApplicationNumber()
                );
            }

            // Auto-assign ME by workload
            // =====================================================
            // ASSIGN MINING ENGINEER
            // =====================================================
            UserWorkloadProjection assignedME =
                    restorationApplicationRepository
                            .findMEWithLeastWorkload(regionId);

            if (assignedME == null) {

                assignedME =
                        restorationApplicationRepository
                                .findMEWithLeastWorkload(9L);

                if (assignedME == null) {
                    throw new BusinessException(
                            ErrorCodes.RECORD_NOT_FOUND,
                            "Mining Engineer with required permission, role and region not found."
                    );
                }
            }

            restoration.setAssignedMeUserId(
                    assignedME.getUserId()
            );

            // =====================================================
            // NOTIFICATIONS
            // =====================================================
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
                    MENU_ID_MINING_ENGINEER,
                    "STAFF",
                    true,
                    restoration.getApplicationNumber()
            );
        }

        // =========================================================
        // SAVE
        // =========================================================
        restorationApplicationRepository.save(restoration);
        return toResponse(restoration);
    }

    private LocalDateTime validateMRPSubmissionEligibility(
            MiningLeaseApplication miningLeaseApplication,
            QuarryLeaseApplication quarryLeaseApplication) {

        String leaseStatus;
        ApplicationMaster applicationMaster;

        if (miningLeaseApplication != null) {

            leaseStatus = miningLeaseApplication.getCurrentStatus();
            applicationMaster = miningLeaseApplication.getApplicationMaster();

        } else {

            leaseStatus = quarryLeaseApplication.getCurrentStatus();
            applicationMaster = quarryLeaseApplication.getApplicationMaster();
        }

        // ---------------------------------------------------------
        // 1. Lease/Application must be TERMINATED
        // ---------------------------------------------------------

        if (!"TERMINATED".equalsIgnoreCase(leaseStatus)) {

            throw new BusinessException(
                    ErrorCodes.INVALID_STATE,
                    "Mine Restoration Plan can only be submitted for a terminated application."
            );
        }


        // ---------------------------------------------------------
        // 2. Application Master must exist
        // ---------------------------------------------------------

        if (applicationMaster == null) {

            throw new BusinessException(
                    ErrorCodes.RECORD_NOT_FOUND,
                    "Application Master record was not found."
            );
        }


        // ---------------------------------------------------------
        // 3. Application Master status must be valid
        // ---------------------------------------------------------

        String masterStatus = applicationMaster.getCurrentStatus();

        boolean validMasterStatus =
                "TERMINATED".equalsIgnoreCase(masterStatus)
                        || "TEMPORARY CLOSURE APPROVED".equalsIgnoreCase(masterStatus);

        if (!validMasterStatus) {

            throw new BusinessException(
                    ErrorCodes.INVALID_STATE,
                    "Mine Restoration Plan cannot be submitted because the Application Master status is "
                            + masterStatus
                            + "."
            );
        }


        // ---------------------------------------------------------
        // 4. Approved date must exist
        // ---------------------------------------------------------

        LocalDateTime approvedAt = applicationMaster.getApprovedAt();

        if (approvedAt == null) {

            throw new BusinessException(
                    ErrorCodes.INVALID_STATE,
                    "Application Master approval date is not available."
            );
        }


        // ---------------------------------------------------------
        // 5. Calculate MRP submission deadline
        // ---------------------------------------------------------

        LocalDateTime deadline = approvedAt.plusMonths(2);

        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(deadline)) {

            throw new BusinessException(
                    ErrorCodes.INVALID_STATE,
                    "The deadline for submitting the Mine Restoration Plan has passed. "
                            + "The deadline was " + deadline.toLocalDate() + "."
            );
        }

        return deadline;
    }

    @Transactional
    public MineRestorationResponse resubmitMRP(MineRestorationMRPResubmitRequest request, Long userId) {
        MineRestorationApplication restoration = findById(request.getRestorationApplicationId());

        if (!STATUS_MRP_REVISION_REQUESTED.equals(restoration.getCurrentStatus())
                && !STATUS_MRP_DRAFT.equals(restoration.getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.INVALID_STATE, "Invalid current status of the application for resubmission.");
        }

        // =========================================================
        // VALIDATE RESUBMISSION DEADLINE
        // =========================================================

        if (STATUS_MRP_REVISION_REQUESTED.equals(
                restoration.getCurrentStatus())) {

            Date resubmissionDateLine =
                    restoration.getResubmissionDateLine();

            if (resubmissionDateLine == null) {

                throw new BusinessException(
                        ErrorCodes.INVALID_STATE,
                        "Resubmission deadline has not been set for this application."
                );
            }

            LocalDate deadline =
                    resubmissionDateLine.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

            LocalDate today = LocalDate.now();

            if (today.isAfter(deadline)) {

                throw new BusinessException(
                        ErrorCodes.INVALID_STATE,
                        "The resubmission deadline has passed. "
                                + "The deadline was " + deadline + "."
                );
            }
        }


        restoration.setMrpDocId(request.getMrpDocId());
        restoration.setCurrentStatus(STATUS_MRP_SUBMITTED);
        restoration.setMrpSubmittedAt(LocalDateTime.now());
        restoration.setUpdatedBy(userId);
        if (restoration.getApplicationNumber() == null) {
            restoration.setApplicationNumber(generateApplicationNumber());
        }

        restorationApplicationRepository.save(restoration);

        // Notify ME
        if (restoration.getAssignedMeUserId() != null) {
            notificationClient.sendUserNotification(
                    "Revised MRP submitted",
                    "The promoter has resubmitted the Mining Restoration Plan for application "
                            + restoration.getApplicationNumber(),
                    restoration.getAssignedMeUserId(),
                    MENU_ID_MINING_ENGINEER,
                    "STAFF",
                    true,
                    restoration.getApplicationNumber()
            );
        }
        return toResponse(restoration);
    }

    // =====================================================
    // PROMOTER — Progress Report
    // =====================================================

    @Transactional
    public MineRestorationProgressReportResponse submitProgressReport(
            MineRestorationProgressReportRequest request,
            Long userId) {

        MineRestorationApplication restoration =
                restorationApplicationRepository
                        .findByApplicationNumber(
                                request.getRestorationApplicationNumber())
                        .orElseThrow(() ->
                                new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

        boolean isDraft =
                "DRAFT".equalsIgnoreCase(request.getStatus());

        /*
         * Only enforce the six-month rule when the applicant
         * actually submits the progress report.
         */
        if (!isDraft) {
            validateProgressReportSubmission(restoration);
        }

        MineRestorationProgressReport report =
                new MineRestorationProgressReport();

        report.setRestorationApplicationNumber(
                request.getRestorationApplicationNumber());

        report.setNameOfMine(
                restoration.getNameOfMine());

        report.setLeaseAreaAcres(
                restoration.getLeaseAreaAcres());

        report.setNameOfLessee(
                restoration.getApplicantName());

        report.setLocationImageDocId(
                request.getLocationImageDocId());

        report.setStartDateOfMineRestoration(
                request.getStartDateOfMineRestoration());

        report.setDateOfProgressReport(
                request.getDateOfProgressReport());

        report.setActivityDescription(
                request.getActivityDescription());

        report.setFinancialProgress(
                request.getFinancialProgress());

        report.setPhysicalProgress(
                request.getPhysicalProgress());

        report.setPictorialEvidenceDocId(
                request.getPictorialEvidenceDocId());

        report.setSubmittedBy(userId);

        if (isDraft) {

            report.setStatus(STATUS_PROGRESS_REPORT_DRAFT);

            restoration.setCurrentStatus(
                    STATUS_PROGRESS_REPORT_DRAFT);

            restorationApplicationRepository.save(restoration);

        } else {

            long count =
                    progressReportRepository.countSubmittedReports(
                            request.getRestorationApplicationNumber());

            report.setProgressReportNumber(
                    (int) count + 1);

            report.setStatus(
                    "PROGRESS_REPORT_SUBMITTED");

            restoration.setCurrentStatus(
                    STATUS_PROGRESS_REPORT_SUBMITTED);

            restorationApplicationRepository.save(restoration);

            // Auto-assign RC
            long assignedRcUserId =
                    new Random().nextBoolean() ? 18L : 21L;

            report.setAssignedRcUserId(assignedRcUserId);

            notificationClient.sendUserNotification(
                    "Progress Report Assigned for Verification",
                    "A progress report for application "
                            + restoration.getApplicationNumber()
                            + " has been assigned to you for verification.",
                    assignedRcUserId,
                    MENU_ID_RC,
                    "STAFF",
                    true,
                    restoration.getApplicationNumber()
            );

            // Notify ME
            if (restoration.getAssignedMeUserId() != null) {

                notificationClient.sendUserNotification(
                        "Progress Report Submitted",
                        "Progress report #"
                                + report.getProgressReportNumber()
                                + " submitted for application "
                                + restoration.getApplicationNumber(),
                        restoration.getAssignedMeUserId(),
                        MENU_ID_MINING_ENGINEER,
                        "STAFF",
                        false,
                        restoration.getApplicationNumber()
                );
            }
        }

        progressReportRepository.save(report);

        return toProgressReportResponse(report);
    }

    private void validateProgressReportSubmission(
            MineRestorationApplication restoration) {

        if (restoration.getWorkOrderIssuedAt() == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_STATE,
                    "Work order has not been issued. Progress report cannot be submitted."
            );
        }

        LocalDate today = LocalDate.now();

        Optional<MineRestorationProgressReport> latestReport =
                progressReportRepository
                        .findTopByRestorationApplicationNumberAndStatusOrderByDateOfProgressReportDesc(
                                restoration.getApplicationNumber(),
                                "PROGRESS_REPORT_SUBMITTED"
                        );

        LocalDate eligibleDate;

        if (latestReport.isEmpty()) {

            // First progress report
            eligibleDate = restoration
                    .getWorkOrderIssuedAt()
                    .toLocalDate()
                    .plusMonths(6);

        } else {

            // Subsequent progress report
            MineRestorationProgressReport previousReport =
                    latestReport.get();

            if (previousReport.getDateOfProgressReport() == null) {
                throw new BusinessException(
                        ErrorCodes.INVALID_STATE,
                        "Previous progress report does not have a report date."
                );
            }

            eligibleDate = previousReport
                    .getDateOfProgressReport()
                    .plusMonths(6);
        }

        if (today.isBefore(eligibleDate)) {

            throw new BusinessException(
                    ErrorCodes.INVALID_STATE,
                    "Progress report cannot be submitted before "
                            + eligibleDate + ". The next progress report is due after six months."
            );
        }
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

//        if (!STATUS_PROGRESS_REPORT_SUBMITTED.equals(restoration.getCurrentStatus())
//                && !STATUS_RESTORATION_IN_PROGRESS.equals(restoration.getCurrentStatus())
//                && !STATUS_COMPLETION_REPORT_REQUESTED.equals(restoration.getCurrentStatus())) {
//            throw new BusinessException(ErrorCodes.INVALID_STATE);
//        }

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
            report.setStatus(STATUS_COMPLETION_REPORT_DRAFT);
            restoration.setCurrentStatus(STATUS_COMPLETION_REPORT_DRAFT);
            restorationApplicationRepository.save(restoration);
        } else {
            MineRestorationProgressReport progressReport = progressReportRepository
                    .findByRestorationApplicationNumberAndStatus(
                            request.getRestorationApplicationNumber(), "COMPLETION_REPORT_REQUESTED")
                    .stream().findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));

            report.setStatus("COMPLETION_REPORT_SUBMITTED");
            restoration.setCurrentStatus(STATUS_COMPLETION_REPORT_SUBMITTED);
            restorationApplicationRepository.save(restoration);

            progressReport.setStatus(STATUS_COMPLETION_REPORT_SUBMITTED);
            progressReportRepository.save(progressReport);

            if (restoration.getAssignedMeUserId() != null) {
                notificationClient.sendUserNotification(
                        "Restoration Completion Report Submitted",
                        "The promoter has submitted the Restoration Completion Report for application "
                                + restoration.getApplicationNumber(),
                        restoration.getAssignedMeUserId(),
                        MENU_ID_MINING_ENGINEER,
                        "STAFF",
                        true,
                        restoration.getApplicationNumber()
                );
            }
        }

        completionReportRepository.save(report);
        return toCompletionReportResponse(report);
    }

    // =====================================================
    // PROMOTER — Queries
    // =====================================================

    public long countMyApplications(Long userId) {
        return restorationApplicationRepository.countByApplicantUserId(userId);
    }

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

    /**
     * Full application detail looked up by its public application number — used by the
     * Track Applications "View Details" deep link, which only knows the application number
     * and not the reviewer's own role (getApplicationById's role-prefixed routes are all
     * backed by this same open lookup, just keyed by numeric id).
     */
    public MineRestorationResponse getApplicationByApplicationNumber(String applicationNumber) {
        MineRestorationApplication application = restorationApplicationRepository
                .findByApplicationNumber(applicationNumber)
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));
        return toResponse(application);
    }

    public SuccessResponse<List<MineRestorationProgressReportResponse>> getProgressReports(
            String applicationNumber, Pageable pageable) {
        Page<MineRestorationProgressReport> page =
                progressReportRepository.findByRestorationApplicationNumber(applicationNumber, pageable);
        return SuccessResponse.fromPage("Progress reports retrieved successfully",
                page.map(this::toPromoterProgressReportResponse));
    }

    /**
     * Progress report view for the promoter's own dashboard (getProgressReports above). The
     * RC/MI verification report and remarks, and the ME's remarks, are the reviewers'
     * internal working notes — stripped here rather than in the shared toProgressReportResponse
     * mapper, which is also used by the RC-facing view (getProgressReportsForRC) that does
     * need them.
     */
    private MineRestorationProgressReportResponse toPromoterProgressReportResponse(MineRestorationProgressReport r) {
        MineRestorationProgressReportResponse res = toProgressReportResponse(r);
        res.setVerificationReportDocId(null);
        res.setVerificationSubmittedAt(null);
        res.setVerificationRemarks(null);
        res.setMeRemarks(null);
        res.setMeReviewedAt(null);
        return res;
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
                        "Your application " + restoration.getApplicationNumber() + " for Mine Restoration has been approved. A work order has been issued to begin restoration."
                );
                notificationClient.sendUserNotification(
                        "MRP Approved — Work Order Issued",
                        "Your application " + restoration.getApplicationNumber()
                                + " for Mine Restoration has been approved. Work order has been issued.",
                        restoration.getApplicantUserId(),
                        MENU_ID_APPLICANT,
                        "CITIZEN",
                        false,
                        restoration.getApplicationNumber()
                );
                updateLeaseApplicationStatus(restoration.getMiningLeaseApplicationNumber(),STATUS_MRP_APPROVED,STATUS_MRP_APPROVED);
            }
            case "REVISION_REQUESTED" -> {
                restoration.setCurrentStatus(STATUS_MRP_REVISION_REQUESTED);
                restoration.setResubmissionDateLine(request.getResubmissionDateLine());
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
                        MENU_ID_APPLICANT,
                        "CITIZEN",
                        true,
                        restoration.getApplicationNumber()
                );
            }
            case "REJECTED" -> {
                if (request.getRemarks() == null || request.getRemarks().trim().isEmpty()) {
                    throw new BusinessException(ErrorCodes.MISSING_REQUIRED_FIELD, "Remarks are mandatory for rejection.");
                }
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
                        "Your application " + restoration.getApplicationNumber()
                                + " for Mine Restoration has been rejected. Reason: " + request.getRemarks(),
                        restoration.getApplicantUserId(),
                        MENU_ID_APPLICANT,
                        "CITIZEN",
                        false,
                        restoration.getApplicationNumber()
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

        LocalDateTime workOrderIssuedAt = LocalDateTime.now();

        restoration.setWorkOrderDocId(workOrderDocId);

        restoration.setWorkOrderIssuedAt(
                workOrderIssuedAt
        );

        /*
         * Progress Report #1 is due exactly six months
         * from the date the work order was issued.
         */
        restoration.setNextProgressReportDueDate(
                workOrderIssuedAt
                        .toLocalDate()
                        .plusMonths(6)
        );

        /*
         * No reminder has been sent yet because this
         * is a newly issued work order.
         */
        restoration.setProgressReportReminderSentAt(null);

        restoration.setCurrentStatus(
                STATUS_RESTORATION_IN_PROGRESS
        );

        restoration.setUpdatedBy(userId);

        restorationApplicationRepository.save(restoration);

        // Email notification
        notificationClient.sendWorkOrderNotification(
                restoration.getApplicantEmail(),
                restoration.getApplicantName(),
                restoration.getApplicationNumber()
        );

        // Applicant notification
        notificationClient.sendUserNotification(
                "Work Order Issued",
                "Work order for the restoration works has been issued. You shall submit a progress report for the "
                        + "restoration work in the 6th month from the date of issue of work order.",
                restoration.getApplicantUserId(),
                MENU_ID_APPLICANT,
                "CITIZEN",
                false,
                restoration.getApplicationNumber()
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
                report.setStatus(STATUS_RESTORATION_IN_PROGRESS);
                restoration.setCurrentStatus(STATUS_RESTORATION_IN_PROGRESS);
                notificationClient.sendUserNotification(
                        "Progress Report Reviewed",
                        "Your progress report for application " + restoration.getApplicationNumber()
                                + " has been reviewed. Continue restoration.",
                        restoration.getApplicantUserId(),
                        MENU_ID_APPLICANT,
                        "CITIZEN",
                        false,
                        restoration.getApplicationNumber()
                );
            }
            case "COMPLETION_REQUESTED" -> {
                report.setStatus("COMPLETION_REPORT_REQUESTED");
                restoration.setCurrentStatus(STATUS_COMPLETION_REPORT_REQUESTED);
                notificationClient.sendUserNotification(
                        "Please Submit Restoration Completion Report",
                        "The Mining Engineer has confirmed restoration is complete. Please submit the "
                                + "Restoration Completion Report for application " + restoration.getApplicationNumber(),
                        restoration.getApplicantUserId(),
                        MENU_ID_APPLICANT,
                        "CITIZEN",
                        true,
                        restoration.getApplicationNumber()
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

        MineRestorationProgressReport progressReport = progressReportRepository
                .findByRestorationApplicationNumberAndStatus(
                        restoration.getApplicationNumber(), "COMPLETION_REPORT_SUBMITTED")
                .stream().findFirst().orElse(null);

        completionReport.setMeRemarks(request.getRemarks());
        completionReport.setMeReviewedAt(LocalDateTime.now());
        restoration.setErbDecision(request.getDecision().toUpperCase());
        restoration.setErbDecidedAt(LocalDateTime.now());
        restoration.setErbRemarks(request.getRemarks());
        restoration.setUpdatedBy(userId);

        switch (request.getDecision().toUpperCase()) {
            case "ERB_RELEASED" -> {
                completionReport.setStatus(STATUS_ERB_APPROVED);
                restoration.setCurrentStatus(STATUS_ERB_APPROVED);
                notificationClient.sendUserNotification(
                        "Restoration Approved — ERB Release Letter Pending",
                        "Your application " + restoration.getApplicationNumber()
                                + " for Mine Restoration has been approved. The ERB Release Letter will be issued shortly.",
                        restoration.getApplicantUserId(),
                        MENU_ID_APPLICANT,
                        "CITIZEN",
                        false,
                        restoration.getApplicationNumber()
                );
            }
            case "ERB_UTILIZED" -> {
                completionReport.setStatus(STATUS_ERB_UTILIZED);
                restoration.setCurrentStatus(STATUS_ERB_UTILIZED);
                if (progressReport != null) {
                    progressReport.setStatus(STATUS_ERB_UTILIZED);
                    progressReportRepository.save(progressReport);
                }
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
                        MENU_ID_APPLICANT,
                        "CITIZEN",
                        false,
                        restoration.getApplicationNumber()
                );
            }
            default -> throw new BusinessException(ErrorCodes.INVALID_INPUT_DATA);
        }

        completionReportRepository.save(completionReport);
        restorationApplicationRepository.save(restoration);
        return toResponse(restoration);
    }

    @Transactional
    public MineRestorationResponse issueERBReleaseLetter(Long restorationApplicationId, String erbReleaseLetterDocId, Long userId) {
        MineRestorationApplication restoration = findById(restorationApplicationId);

        if (!STATUS_ERB_APPROVED.equals(restoration.getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.INVALID_STATE);
        }

        restoration.setErbReleaseLetterDocId(erbReleaseLetterDocId);
        restoration.setErbReleaseLetterIssuedAt(LocalDateTime.now());
        restoration.setCurrentStatus(STATUS_ERB_RELEASED);
        restoration.setUpdatedBy(userId);

        // Sync progress report
        MineRestorationProgressReport progressReport = progressReportRepository
                .findByRestorationApplicationNumberAndStatus(restoration.getApplicationNumber(), STATUS_COMPLETION_REPORT_SUBMITTED)
                .stream().findFirst().orElse(null);
        if (progressReport != null) {
            progressReport.setStatus(STATUS_ERB_RELEASED);
            progressReportRepository.save(progressReport);
        }

        // Sync completion report
        MineRestorationCompletionReport completionReport = completionReportRepository
                .findByRestorationApplicationNumber(restoration.getApplicationNumber())
                .orElse(null);
        if (completionReport != null) {
            completionReport.setStatus(STATUS_ERB_RELEASED);
            completionReportRepository.save(completionReport);
        }

        restorationApplicationRepository.save(restoration);

        notificationClient.sendApprovalNotification(
                restoration.getApplicantEmail(),
                restoration.getApplicantName(),
                restoration.getApplicationNumber()
        );
        notificationClient.sendUserNotification(
                "ERB Release Letter Issued",
                "The ERB Release Letter has been issued for your restoration application "
                        + restoration.getApplicationNumber() + ". The Environmental Restoration Bond (ERB) will be refunded through BIRMS.",
                restoration.getApplicantUserId(),
                MENU_ID_APPLICANT,
                "CITIZEN",
                false,
                restoration.getApplicationNumber()
        );

        return toResponse(restoration);
    }

    @Transactional
    public MineRestorationResponse issueERBUtilizationLetter(Long restorationApplicationId, String erbUtilizationLetterDocId, Long userId) {
        MineRestorationApplication restoration = findById(restorationApplicationId);

        if (!STATUS_ERB_UTILIZED.equals(restoration.getCurrentStatus())) {
            throw new BusinessException(ErrorCodes.INVALID_STATE);
        }

        restoration.setErbUtilizationLetterDocId(erbUtilizationLetterDocId);
        restoration.setErbUtilizationLetterIssuedAt(LocalDateTime.now());
        restoration.setUpdatedBy(userId);
        restorationApplicationRepository.save(restoration);

        notificationClient.sendUserNotification(
                "ERB Utilization Letter Issued",
                "The ERB Utilization Letter has been issued for your restoration application "
                        + restoration.getApplicationNumber() + ".",
                restoration.getApplicantUserId(),
                MENU_ID_APPLICANT,
                "CITIZEN",
                false,
                restoration.getApplicationNumber()
        );

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
                    MENU_ID_MINING_ENGINEER,
                    "STAFF",
                    true,
                    restoration.getApplicationNumber()
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

//        if (!"PROGRESS_REPORT_SUBMITTED".equals(report.getStatus())) {
//            throw new BusinessException(ErrorCodes.INVALID_STATE);
//        }

        report.setAssignedRcUserId(userId);
        report.setVerificationReportDocId(request.getVerificationReportDocId());
        report.setVerificationRemarks(request.getRemarks());
        report.setVerificationSubmittedAt(LocalDateTime.now());
        report.setStatus(STATUS_VERIFICATION_SUBMITTED);

        progressReportRepository.save(report);

        MineRestorationApplication restoration = restorationApplicationRepository
                .findByApplicationNumber(report.getRestorationApplicationNumber())
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));
        restoration.setCurrentStatus(STATUS_VERIFICATION_SUBMITTED);
        restorationApplicationRepository.save(restoration);

        // Notify ME
        if (restoration.getAssignedMeUserId() != null) {
            notificationClient.sendUserNotification(
                    "Verification Report Submitted",
                    "RC/MI has submitted the verification report for progress report #"
                            + report.getProgressReportNumber() + " of application "
                            + restoration.getApplicationNumber(),
                    restoration.getAssignedMeUserId(),
                    MENU_ID_MINING_ENGINEER,
                    "STAFF",
                    true,
                    restoration.getApplicationNumber()
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
        res.setErbReleaseLetterDocId(app.getErbReleaseLetterDocId());
        res.setErbReleaseLetterIssuedAt(app.getErbReleaseLetterIssuedAt());
        res.setErbUtilizationLetterDocId(app.getErbUtilizationLetterDocId());
        res.setErbUtilizationLetterIssuedAt(app.getErbUtilizationLetterIssuedAt());
        res.setCurrentStatus(app.getCurrentStatus());
        res.setCurrentStatusDisplayName(getStatusDisplayName(app.getCurrentStatus()));
        res.setRejectionReason(app.getRejectionReason());
        res.setCreatedBy(app.getCreatedBy());
        res.setCreatedOn(app.getCreatedOn());
        res.setUpdatedOn(app.getUpdatedOn());
        res.setResubmissionDateLine(app.getResubmissionDateLine());
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
            case "PROGRESS_REPORT_DRAFT" -> "Progress Report Draft";
            case "COMPLETION_REPORT_DRAFT" -> "Completion Report Draft";
            case "MRP_SUBMITTED" -> "MRP Submitted";
            case "MRP_REVISION_REQUESTED" -> "MRP Revision Requested";
            case "MRP_APPROVED" -> "MRP Approved";
            case "MRP_REJECTED" -> "MRP Rejected";
            case "RESTORATION_IN_PROGRESS" -> "Restoration In Progress";
            case "PROGRESS_REPORT_SUBMITTED" -> "Progress Report Submitted";
            case "COMPLETION_REPORT_REQUESTED" -> "Completion Report Requested";
            case "COMPLETION_REPORT_SUBMITTED" -> "Completion Report Submitted";
            case "VERIFICATION_SUBMITTED" -> "Verification Submitted";
            case "ERB_APPROVED" -> "ERB Approved — Release Letter Pending";
            case "ERB_RELEASED" -> "ERB Released";
            case "ERB_UTILIZED" -> "ERB Utilized by DGM";
            default -> status.replace("_", " ");
        };
    }

    /**
     * Updates the underlying lease application's status once a termination is decided, trying
     * Mining Lease first and falling back to Quarry Lease, mirroring resolveAndMarkUnderReview.
     */
    private void updateLeaseApplicationStatus(String appNo, String miningStatus, String quarryStatus) {
        Optional<MiningLeaseApplication> miningLeaseApplication =
                miningLeaseApplicationRepository.findByApplicationNumber(appNo);

        if (miningLeaseApplication.isPresent()) {
            MiningLeaseApplication application = miningLeaseApplication.get();
            application.setCurrentStatus(miningStatus);
            miningLeaseApplicationRepository.save(application);
            return;
        }

        QuarryLeaseApplication application = queryLeaseApplicationRepository.findByApplicationNumber(appNo)
                .orElseThrow(() -> new BusinessException(ErrorCodes.RECORD_NOT_FOUND));
        application.setCurrentStatus(quarryStatus);
        queryLeaseApplicationRepository.save(application);

        Optional<HouseholdPermitThresholdEntity> householdPermitThresholdEntity = householdPermitThresholdRepository.findByApplicationNoAndServiceType(appNo, SERVICE_CODE);

        if (householdPermitThresholdEntity.isPresent()) {
            HouseholdPermitThresholdEntity thresholdEntity = householdPermitThresholdEntity.get();
            thresholdEntity.setStatus(quarryStatus);

            householdPermitThresholdRepository.save(thresholdEntity);
        }else {
            throw new BusinessException(ErrorCodes.BUSINESS_RULE_VIOLATION, "The application is not present in household permit table.");
        }
    }
}
