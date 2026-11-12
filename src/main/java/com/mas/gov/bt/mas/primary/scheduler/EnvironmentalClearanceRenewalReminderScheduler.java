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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * Reminds the promoter to apply for renewal of the environmental clearance (EC)
 * recorded on their mining lease once the EC is within six months of expiry.
 *
 * One reminder is sent per EC: the EC expiry date the reminder was sent for is
 * stored on the lease, so an EC renewal (which moves the expiry date) re-arms
 * the reminder for the new EC.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnvironmentalClearanceRenewalReminderScheduler {

    private static final List<String> ACTIVE_LEASE_STATUSES = List.of("MINING LEASE APPROVED");

    /** EC renewal statuses that mean no renewal is in progress any more. */
    private static final List<String> CLOSED_EC_RENEWAL_STATUSES =
            List.of("DRAFT", "REJECTED", "APPROVED", "EC_RENEWED");

    /** How long before EC expiry the promoter is reminded. */
    private static final int REMINDER_MONTHS_BEFORE_EXPIRY = 6;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final MiningLeaseApplicationRepository miningLeaseApplicationRepository;

    private final NotificationClient notificationClient;

    private final MenuIdResolver menuIdResolver;

    private String MENU_ID_APPLICANT = "92"; // "Promoter Application List" — EC renewal (/renewalapplicationlist)

    @jakarta.annotation.PostConstruct
    private void resolveMenuIds() {
        MENU_ID_APPLICANT = menuIdResolver.resolve("/renewalapplicationlist", MENU_ID_APPLICANT);
    }

    /*
     * Runs every day at 2:00 AM (server time).
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void sendEcRenewalReminders() {

        log.info("Starting Environmental Clearance renewal reminder job");

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now();

        // ecExpiryDate is a timestamp: cover today 00:00 up to the end of the cutoff day.
        Date from = Date.from(today.atStartOfDay(zone).toInstant());
        Date toExclusive = Date.from(
                today.plusMonths(REMINDER_MONTHS_BEFORE_EXPIRY)
                        .plusDays(1)
                        .atStartOfDay(zone)
                        .toInstant()
        );

        List<MiningLeaseApplication> leases =
                miningLeaseApplicationRepository.findLeasesDueForEcRenewalReminder(
                        from,
                        toExclusive,
                        ACTIVE_LEASE_STATUSES,
                        CLOSED_EC_RENEWAL_STATUSES
                );

        log.info("Found {} mining leases requiring EC renewal reminder", leases.size());

        for (MiningLeaseApplication lease : leases) {
            try {
                processReminder(lease, zone);
            } catch (Exception e) {
                log.error(
                        "Failed to send EC renewal reminder for application {}",
                        lease.getApplicationNumber(),
                        e
                );
            }
        }

        log.info("Completed Environmental Clearance renewal reminder job");
    }

    private void processReminder(MiningLeaseApplication lease, ZoneId zone) {

        Long promoterUserId = lease.getApplicantUserId() != null
                ? lease.getApplicantUserId()
                : lease.getCreatedBy();

        if (promoterUserId == null) {
            log.warn(
                    "Skipping EC renewal reminder for application {} because no promoter user is linked",
                    lease.getApplicationNumber()
            );
            return;
        }

        // new Date(getTime()) because Hibernate may hand back a java.sql.Timestamp/Date,
        // and java.sql.Date does not support toInstant().
        String expiry = new Date(lease.getEcExpiryDate().getTime())
                .toInstant()
                .atZone(zone)
                .toLocalDate()
                .format(DATE_FORMAT);

        String ecRef = lease.getEcNumber() != null && !lease.getEcNumber().isBlank()
                ? " (EC No. " + lease.getEcNumber() + ")"
                : "";

        String title = "Environmental Clearance Renewal Due";
        String message = "The environmental clearance"
                + ecRef
                + " for your mining lease (Application No. "
                + lease.getApplicationNumber()
                + ") will expire on "
                + expiry
                + ". Please apply for renewal of the environmental clearance before the expiry date.";

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
                        "EC renewal reminder email failed for application {}",
                        lease.getApplicationNumber(),
                        e
                );
            }
        }

        // Mark this EC as reminded so tomorrow's run does not repeat it.
        lease.setEcRenewalReminderSentForExpiryDate(lease.getEcExpiryDate());
        miningLeaseApplicationRepository.save(lease);

        log.info(
                "EC renewal reminder sent for application {} (EC expires {})",
                lease.getApplicationNumber(),
                expiry
        );
    }
}
