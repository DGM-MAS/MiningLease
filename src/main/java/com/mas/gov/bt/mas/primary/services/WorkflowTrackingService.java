package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.entity.ApplicationMaster;
import com.mas.gov.bt.mas.primary.entity.TaskManagement;
import com.mas.gov.bt.mas.primary.repository.ApplicationMasterRepository;
import com.mas.gov.bt.mas.primary.repository.TaskManagementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Shared helper for recording application lifecycle events into:
 *   - t_application_master  (one row per application, tracks current status)
 *   - t_task_management     (one row per task assignment / action)
 *
 * Mirrors mas-royalty-service's
 * {@code ApplicationMaster/Service/WorkflowTrackingService.java}, adapted to this
 * project's entity field names (no {@code applicationNo}/{@code isActive} columns here;
 * {@code completedAt}/{@code completedOn} and {@code submittedAt}/{@code submittedOn} are
 * kept in sync via {@link ApplicationMaster#syncTimestampColumnPairs()}).
 *
 * MiningLeaseService and MiningLeaseRenewalService both hand-roll this triad per-method
 * today (see each file's own private {@code createTask}/{@code completeCurrentTask}
 * helpers) — this class does NOT replace those existing call sites. It exists so the
 * specific gaps tracked in WORKFLOW_SYNC_FINDINGS.md can be fixed without further
 * copy-pasting, and so future new transitions have a shared helper to reach for instead
 * of hand-rolling another one-off.
 *
 * Audit logging is intentionally not duplicated here: {@code ApplicationMasterAuditAspect}
 * already intercepts every {@code ApplicationMasterRepository.save(...)} call (including
 * the ones inside this class) and writes to {@code audit_logs} automatically. There is no
 * equivalent aspect for TaskManagement changes today.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowTrackingService {

    private final ApplicationMasterRepository applicationMasterRepository;
    private final TaskManagementRepository taskManagementRepository;

    // ── ApplicationMaster ────────────────────────────────────────────────────────

    /**
     * Creates a new record in t_application_master when an application is first submitted.
     * No-op if a row for this application/service already exists.
     */
    @Transactional
    public void saveApplicationMaster(String applicationNo, String serviceCode, String serviceId,
                                       Long applicantId, String status, LocalDateTime submittedOn) {
        if (applicationMasterRepository.findByApplicationNumberAndServiceCode(applicationNo, serviceCode).isPresent()) {
            return;
        }
        ApplicationMaster master = new ApplicationMaster();
        master.setApplicationNumber(applicationNo);
        master.setServiceCode(serviceCode);
        master.setServiceId(serviceId);
        master.setApplicantUserId(applicantId);
        master.setCurrentStatus(status);
        LocalDateTime submitted = submittedOn != null ? submittedOn : LocalDateTime.now();
        master.setSubmittedAt(submitted);
        master.setSubmittedOn(submitted);
        applicationMasterRepository.save(master);
        log.info("Created application master for {} ({})", applicationNo, serviceCode);
    }

    /**
     * Updates the current status of an existing t_application_master record.
     * Also sets completedAt/completedOn when the application reaches a terminal status.
     */
    @Transactional
    public void updateApplicationMasterStatus(String applicationNo, String serviceCode,
                                               String status, boolean isTerminal) {
        applicationMasterRepository
                .findByApplicationNumberAndServiceCode(applicationNo, serviceCode)
                .ifPresent(master -> {
                    master.setCurrentStatus(status);
                    if (isTerminal) {
                        LocalDateTime now = LocalDateTime.now();
                        master.setCompletedAt(now);
                        master.setCompletedOn(now);
                    }
                    applicationMasterRepository.save(master);
                });
    }

    // ── TaskManagement ───────────────────────────────────────────────────────────

    /**
     * Creates a new task record when an application is assigned to a focal officer.
     */
    @Transactional
    public void createTask(String applicationNo, String serviceCode,
                            Long assignedToUserId, String assignedToRole,
                            Long assignedByUserId, Long createdBy) {
        TaskManagement task = new TaskManagement();
        task.setApplicationNumber(applicationNo);
        task.setServiceCode(serviceCode);
        task.setAssignedToUserId(assignedToUserId);
        task.setAssignedToRole(assignedToRole);
        task.setAssignedByUserId(assignedByUserId);
        task.setCreatedBy(createdBy);
        task.setAssignedAt(LocalDateTime.now());
        task.setTaskStatus("ASSIGNED");
        task.setReassignmentCount(0);
        taskManagementRepository.save(task);
        log.info("Created task for application {} ({}) assigned to user {} ({})",
                applicationNo, serviceCode, assignedToUserId, assignedToRole);
    }

    /**
     * Marks the most-recent task for an application as COMPLETED.
     */
    @Transactional
    public void completeCurrentTask(String applicationNo, String serviceCode,
                                     String actionTaken, String remarks) {
        taskManagementRepository
                .findTopByApplicationNumberAndServiceCodeOrderByAssignedAtDesc(applicationNo, serviceCode)
                .ifPresent(task -> {
                    task.setTaskStatus("COMPLETED");
                    task.setActionTaken(actionTaken);
                    task.setActionRemarks(remarks);
                    taskManagementRepository.save(task);
                    log.info("Completed task {} for application {} ({})", task.getId(), applicationNo, serviceCode);
                });
    }

    /**
     * Updates the status / action of the most-recent task without closing it.
     * Use this to record progress on a task that the same assignee will keep working
     * (e.g. an intermediate document upload before the terminal action).
     */
    @Transactional
    public void updateCurrentTask(String applicationNo, String serviceCode,
                                   String taskStatus, String actionTaken, String remarks) {
        taskManagementRepository
                .findTopByApplicationNumberAndServiceCodeOrderByAssignedAtDesc(applicationNo, serviceCode)
                .ifPresent(task -> {
                    task.setTaskStatus(taskStatus);
                    task.setActionTaken(actionTaken);
                    task.setActionRemarks(remarks);
                    taskManagementRepository.save(task);
                });
    }

    /**
     * Reassigns the most-recent task to a new user and increments the reassignment counter.
     */
    @Transactional
    public void reassignTask(String applicationNo, String serviceCode,
                              Long newUserId, String newRole, Long reassignedByUserId) {
        if (newUserId == null) return;
        taskManagementRepository
                .findTopByApplicationNumberAndServiceCodeOrderByAssignedAtDesc(applicationNo, serviceCode)
                .ifPresent(task -> {
                    task.setAssignedToUserId(newUserId);
                    task.setAssignedToRole(newRole);
                    task.setAssignedByUserId(reassignedByUserId);
                    task.setAssignedAt(LocalDateTime.now());
                    task.setTaskStatus("REASSIGNED");
                    task.setActionTaken("REASSIGNED");
                    task.setReassignmentCount(
                            task.getReassignmentCount() == null ? 1 : task.getReassignmentCount() + 1);
                    taskManagementRepository.save(task);
                });
    }
}
