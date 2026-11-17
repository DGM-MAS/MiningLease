package com.mas.gov.bt.mas.primary.scheduler;

import com.mas.gov.bt.mas.primary.entity.MiningLeaseApplication;
import com.mas.gov.bt.mas.primary.integration.MenuIdResolver;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.repository.MiningLeaseApplicationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reminds the promoter to apply for mining lease renewal once the lease is
 * within one year of its expiry date.
 *
 * One reminder is sent per lease term: the lease end date the reminder was sent
 * for is stored on the application, so a renewal (which moves the end date)
 * re-arms the reminder for the next term.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MiningLeaseRenewalReminderScheduler {

    /** Only an active lease can be renewed (see MiningLeaseRenewalService.createRenewalApplication). */
    private static final List<String> ACTIVE_LEASE_STATUSES = List.of("MINING LEASE APPROVED");

    /** How long before expiry the promoter is reminded. */
    private static final int REMINDER_YEARS_BEFORE_EXPIRY = 1;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final MiningLeaseApplicationRepository miningLeaseApplicationRepository;

    private final NotificationClient notificationClient;

    private final MenuIdResolver menuIdResolver;

    private String MENU_ID_APPLICANT = "86"; // "APPLICANT" — MINING LEASE RENEWAL (/renewalleaseapplicationlist)

    @jakarta.annotation.PostConstruct
    private void resolveMenuIds() {
        MENU_ID_APPLICANT = menuIdResolver.resolve("/renewalleaseapplicationlist", MENU_ID_APPLICANT);
    }

    /*
     * Runs every day at 1:30 AM (server time).
     */
    @Scheduled(cron = "0 30 1 * * *")
    public void sendLeaseRenewalReminders() {

        log.info("Starting Mining Lease renewal reminder job");

        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusYears(REMINDER_YEARS_BEFORE_EXPIRY);

        List<MiningLeaseApplication> leases =
                miningLeaseApplicationRepository.findLeasesDueForRenewalReminder(
                        today,
                        cutoff,
                        ACTIVE_LEASE_STATUSES
                );

        log.info("Found {} mining leases requiring renewal reminder", leases.size());

        for (MiningLeaseApplication lease : leases) {
            try {
                processReminder(lease);
            } catch (Exception e) {
                log.error(
                        "Failed to send lease renewal reminder for application {}",
                        lease.getApplicationNumber(),
                        e
                );
            }
        }

        log.info("Completed Mining Lease renewal reminder job");
    }

    private void processReminder(MiningLeaseApplication lease) {

        Long promoterUserId = lease.getApplicantUserId() != null
                ? lease.getApplicantUserId()
                : lease.getCreatedBy();

        if (promoterUserId == null) {
            log.warn(
                    "Skipping lease renewal reminder for application {} because no promoter user is linked",
                    lease.getApplicationNumber()
            );
            return;
        }

        String expiry = lease.getLeaseEndDate().format(DATE_FORMAT);
        String title = "Mining Lease Renewal Due";
        String message = "Your mining lease (Application No. "
                + lease.getApplicationNumber()
                + ") will expire on "
                + expiry
                + ". Please apply for renewal of the lease before the expiry date.";

        notificationClient.sendUserNotification(
                title,
                message,
                promoterUserId,
                MENU_ID_APPLICANT,
                "CITIZEN",
                true,
                lease.getApplicationNumber()
        );

        // Email is best-effort: the in-app notification above is the reminder of record.
        if (lease.getApplicantEmail() != null && !lease.getApplicantEmail().isBlank()) {
            try {
                notificationClient.sendEmail(
                        lease.getApplicantEmail(),
                        title + " - " + lease.getApplicationNumber(),
                        "Dear " + (lease.getApplicantName() != null ? lease.getApplicantName() : "Promoter")
                                + ",\n\n" + message
                );
            } catch (Exception e) {
                log.warn(
                        "Lease renewal reminder email failed for application {}",
                        lease.getApplicationNumber(),
                        e
                );
            }
        }

        // Mark this lease term as reminded so tomorrow's run does not repeat it.
        lease.setRenewalReminderSentForEndDate(lease.getLeaseEndDate());
        miningLeaseApplicationRepository.save(lease);

        log.info(
                "Lease renewal reminder sent for application {} (expires {})",
                lease.getApplicationNumber(),
                lease.getLeaseEndDate()
        );
    }
}
