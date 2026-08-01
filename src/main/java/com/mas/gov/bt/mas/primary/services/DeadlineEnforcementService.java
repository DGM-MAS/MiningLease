package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.entity.TaskManagement;
import com.mas.gov.bt.mas.primary.integration.MenuIdResolver;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.repository.TaskManagementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MiningLease's equivalent of Quarrying-Lease's DeadlineEnforcementService — MiningLease
 * previously had no scheduled overdue-task-reminder mechanism at all. Direct port of the
 * same pattern: query t_task_management for PENDING tasks past their deadline_date, and
 * notify whoever it's assigned to.
 *
 * Unlike Quarrying-Lease (a single workflow, so assignedToRole alone is unambiguous),
 * MiningLease's t_task_management is shared by ~9 different services (Mining Lease
 * application, Mining Lease Renewal, Termination, Immediate Suspension, Temporary
 * Closure, Renewal Environmental Clearance, Surface Collection Auction/BG/Review,
 * Sample Transport Clearance) that reuse the same role literals (e.g. "APPLICANT",
 * "GEOLOGIST") for completely different sidebar pages. So resolution here is keyed on
 * (serviceCode, role) instead of role alone, using task.getServiceCode() — each
 * service's own SERVICE_CODE constant — to disambiguate. The (path, fallback) pairs
 * below are copied verbatim from each service's own resolveMenuIds()/PostConstruct
 * block, so this reuses MenuIdResolver's cache rather than re-hitting masters.
 *
 * recipientType mirrors the two citizen-facing task-role literals actually used across
 * these services: "APPLICANT" (Mining Lease, Renewal, Termination, Temporary Closure,
 * Immediate Suspension, Sample Transport Clearance) and "PROMOTER" (Surface Collection
 * Auction/Review) both mean "assigned to the citizen" -> CITIZEN. Everything else is a
 * staff role -> STAFF.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeadlineEnforcementService {

    private final TaskManagementRepository taskRepository;
    private final NotificationClient notificationClient;
    private final MenuIdResolver menuIdResolver;

    private static final String SC_MENU_ID_MPCD = "72"; // SURFACE_COLLECTION_AUCTION (/mpcdauctionsurfacecoltionlist)
    private static final String SC_MENU_ID_PROMOTER = "73"; // (/auctionsurfacecollectionlist)
    private static final String SC_MENU_ID_MINING_DIRECTOR = "74"; // (/mdauctionsurfacecollectionlist) — despite the
    // "MINING_DIRECTOR" role literal, this is actually the Mining Engineer's own review
    // page: mas-frontend serves it from demo/AuctionofSurfaceCollection/miningEnginner/
    // mdauctionsurfacecollectionlist, confirmed against app-routing.module.ts.

    @Scheduled(cron = "0 0 8 * * ?")
    public void checkOverdueTasks() {
        log.info("Running daily overdue task check (MiningLease)...");

        LocalDateTime now = LocalDateTime.now();
        List<TaskManagement> overdueTasks = taskRepository.findOverdueTasks(now);

        if (overdueTasks.isEmpty()) {
            log.info("No overdue tasks found.");
            return;
        }

        for (TaskManagement task : overdueTasks) {
            log.warn("OVERDUE TASK: id={}, applicationNumber={}, serviceCode={}, role={}, deadline={}, assignedTo={}",
                    task.getId(),
                    task.getApplicationNumber(),
                    task.getServiceCode(),
                    task.getAssignedToRole(),
                    task.getDeadlineDate(),
                    task.getAssignedToUserId());

            if (task.getAssignedToUserId() != null) {
                String serviceId = resolveMenuIdForRole(task.getServiceCode(), task.getAssignedToRole());
                String recipientType = isCitizenFacingRole(task.getAssignedToRole()) ? "CITIZEN" : "STAFF";
                notificationClient.sendUserNotification(
                        "Task Overdue",
                        String.format("Your task for application %s (%s) is overdue. Deadline was %s.",
                                task.getApplicationNumber(), task.getAssignedToRole(), task.getDeadlineDate()),
                        task.getAssignedToUserId(),
                        serviceId,
                        recipientType,
                        true,
                        task.getApplicationNumber());
            }
        }

        log.info("Found {} overdue tasks.", overdueTasks.size());
    }

    /** APPLICANT (most flows) and PROMOTER (Surface Collection flows) are the only task-role
     *  literals this codebase uses to mean "assigned to the citizen, not staff". */
    private boolean isCitizenFacingRole(String role) {
        if (role == null) return false;
        String normalized = role.trim().toUpperCase();
        return normalized.equals("APPLICANT") || normalized.equals("PROMOTER");
    }

    /**
     * Resolves the sidebar menu id (permissions.id) for whichever (service, role) an
     * overdue task belongs to. Each case's (path, fallback) is copied from the owning
     * service's own resolveMenuIds()/PostConstruct block — see the class javadoc.
     */
    private String resolveMenuIdForRole(String serviceCode, String role) {
        if (serviceCode == null || role == null) {
            log.warn("Overdue task missing serviceCode or role (serviceCode={}, role={}) — " +
                    "notification will carry no serviceId badge.", serviceCode, role);
            return null;
        }
        String svc = serviceCode.trim().toUpperCase();
        String normalized = role.trim().toUpperCase();

        switch (svc) {
            case "MINING_LEASE":
                return switch (normalized) {
                    case "APPLICANT", "PROMOTER" -> menuIdResolver.resolve("/mininglease-application", "79");
                    case "MPCD_FOCAL" -> menuIdResolver.resolve("/mpcdminingleaseapplicationlist", "80");
                    case "GEOLOGIST" -> menuIdResolver.resolve("/miningleasegeologisapplicantlist", "81");
                    case "MINING_ENGINEER" -> menuIdResolver.resolve("/mingdivisionlist", "82");
                    case "MINING_CHIEF", "MINING_CHIEF_REVIEW" -> menuIdResolver.resolve("/approverejectlist", "83");
                    case "DIRECTOR" -> menuIdResolver.resolve("/mlaapplicationlist", "84");
                    default -> unresolvedRole(serviceCode, role);
                };
            case "MINING LEASE RENEWAL":
                return switch (normalized) {
                    case "APPLICANT" -> menuIdResolver.resolve("/renewalleaseapplicationlist", "86");
                    case "MINE_ENGINEER", "MINING_ENGINEER" -> menuIdResolver.resolve("/mdrenewalleaseapproverejectlist", "87");
                    case "GEOLOGIST" -> menuIdResolver.resolve("/reviewdepositreassetreportlist", "88");
                    case "MINING_CHIEF", "MINING_CHIEF_REVIEW" -> menuIdResolver.resolve("/cheifmdrenewallist", "89");
                    case "DIRECTOR", "DIRECTOR APPROVED FMFS" -> menuIdResolver.resolve("/directorrenewalleaselist", "90");
                    default -> unresolvedRole(serviceCode, role);
                };
            case "TERMINATION_SERVICE":
                return switch (normalized) {
                    case "CMS HEAD", "CMS_HEAD" -> menuIdResolver.resolve("/mtcdecisionlist", "114");
                    case "APPLICANT" -> menuIdResolver.resolve("/promoterrectificationlist", "115");
                    default -> unresolvedRole(serviceCode, role);
                };
            case "TEMPORARY CLOSURE SERVICE":
                return switch (normalized) {
                    case "APPLICANT" -> menuIdResolver.resolve("/temporaryclosurelist", "109");
                    case "RC" -> menuIdResolver.resolve("/rcapproverejectlist", "110");
                    case "MI" -> menuIdResolver.resolve("/miverifiactionlist", "111");
                    default -> unresolvedRole(serviceCode, role);
                };
            case "IMMEDIATE_SUSPENSION":
                return switch (normalized) {
                    case "APPLICANT" -> menuIdResolver.resolve("/Immediatesuspensionapplist", "117");
                    case "MI" -> menuIdResolver.resolve("/Immediatesuspensionmineinspectorapplist", "119");
                    default -> unresolvedRole(serviceCode, role);
                };
            case "SURFACE_COLLECTION_AUCTION":
                return switch (normalized) {
                    case "MPCD" -> menuIdResolver.resolve("/mpcdauctionsurfacecoltionlist", SC_MENU_ID_MPCD);
                    case "PROMOTER" -> menuIdResolver.resolve("/auctionsurfacecollectionlist", SC_MENU_ID_PROMOTER);
                    case "MINING_DIRECTOR" -> menuIdResolver.resolve("/mdauctionsurfacecollectionlist", SC_MENU_ID_MINING_DIRECTOR);
                    default -> unresolvedRole(serviceCode, role);
                };
            case "SAMPLE_TRANSPORT_CLEARANCE":
                return switch (normalized) {
                    case "APPLICANT" -> menuIdResolver.resolve("/SampleTransportClearancesappliantapplist", "150");
                    case "GSD_CHIEF" -> menuIdResolver.resolve("/SampleTransportClearancegsdapplist", "151");
                    case "GSD_FOCAL" -> menuIdResolver.resolve("/SampleTransportClearancegsdfocalapplist", "152");
                    default -> unresolvedRole(serviceCode, role);
                };
            case "RENEWAL_ENV_CLEARANCE":
                return switch (normalized) {
                    case "MPCD" -> menuIdResolver.resolve("/mpcdrenewaleclist", "94");
                    case "MINING ENGINEER", "MINING_ENGINEER" -> menuIdResolver.resolve("/mdrenewalecapprovedlist", "95");
                    case "APPLICANT" -> menuIdResolver.resolve("/renewalapplicationlist", "92");
                    case "RC" -> menuIdResolver.resolve("/rc-renewaleclist", "93");
                    case "MI" -> menuIdResolver.resolve("/mirenewallist", "96");
                    default -> unresolvedRole(serviceCode, role);
                };
            default:
                return unresolvedRole(serviceCode, role);
        }
    }

    private String unresolvedRole(String serviceCode, String role) {
        log.warn("No menu mapping known for serviceCode '{}' / assignedToRole '{}' — overdue " +
                "notification will carry no serviceId badge.", serviceCode, role);
        return null;
    }
}
