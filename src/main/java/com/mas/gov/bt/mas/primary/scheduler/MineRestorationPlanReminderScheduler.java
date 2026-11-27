package com.mas.gov.bt.mas.primary.scheduler;

import com.mas.gov.bt.mas.primary.entity.MiningLeaseApplication;
import com.mas.gov.bt.mas.primary.entity.QuarryLeaseApplication;
import com.mas.gov.bt.mas.primary.entity.StockLiftingApplication;
import com.mas.gov.bt.mas.primary.entity.SurfaceCollectionPermitEntity;
import com.mas.gov.bt.mas.primary.integration.MenuIdResolver;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.integration.SmsClient;
import com.mas.gov.bt.mas.primary.repository.MiningLeaseApplicationRepository;
import com.mas.gov.bt.mas.primary.repository.QuarryLeaseApplicationRepository;
import com.mas.gov.bt.mas.primary.repository.StockLiftingRepository;
import com.mas.gov.bt.mas.primary.repository.SurfaceCollectionPermitRepository;
import com.mas.gov.bt.mas.primary.services.MineRestorationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * Reminds the promoter, 3 years before their mining lease, quarry lease, surface
 * collection permit or stock lifting permit ends, that they
 * may apply for Mine Restoration. The Mine Restoration Plan must be submitted at
 * least 2.5 years before the lease end date (see MineRestorationService), so the
 * reminder leaves them 6 months to apply. Promoters who do not apply simply move
 * on to lease renewal; nothing further is done for them here.
 *
 * Sent once per lease term by in-app notification, email and SMS. The lease end
 * date the reminder was sent for is stored on the lease, so a renewal (which moves
 * the end date) re-arms the reminder for the next term. Leases that already have a
 * submitted Mine Restoration Plan are skipped.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MineRestorationPlanReminderScheduler {

    /** How long before the lease end date the promoter is reminded. */
    private static final int REMINDER_MONTHS_BEFORE_LEASE_END = 36;

    /** The MRP must be submitted at least this long before the lease end date. */
    private static final int SUBMISSION_MONTHS_BEFORE_LEASE_END = 30;

    private static final String SMS_STAGE = "Mine Restoration Plan Due";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final MiningLeaseApplicationRepository miningLeaseApplicationRepository;

    private final QuarryLeaseApplicationRepository quarryLeaseApplicationRepository;

    private final SurfaceCollectionPermitRepository surfaceCollectionPermitRepository;

    private final StockLiftingRepository stockLiftingRepository;

    private final NotificationClient notificationClient;

    private final SmsClient smsClient;

    private final MenuIdResolver menuIdResolver;

    private String MENU_ID_APPLICANT = "121"; // "Promoter Application List" — Mine Restoration (/MineRestorationappliantlist)

    @jakarta.annotation.PostConstruct
    private void resolveMenuIds() {
        MENU_ID_APPLICANT = menuIdResolver.resolve("/MineRestorationappliantlist", MENU_ID_APPLICANT);
    }

    /*
     * Runs every day at 2:30 AM (server time).
     */
    @Scheduled(cron = "0 30 2 * * *")
    public void sendMineRestorationPlanReminders() {

        log.info("Starting Mine Restoration Plan reminder job");

        LocalDate today = LocalDate.now();

        // Lease end dates whose MRP submission deadline (end - 30 months) has not
        // passed yet, up to 3 years out. A lease enters this window exactly 3 years
        // before it ends; leases already inside it are reminded once on the next run.
        LocalDate from = today.plusMonths(SUBMISSION_MONTHS_BEFORE_LEASE_END);
        LocalDate to = today.plusMonths(REMINDER_MONTHS_BEFORE_LEASE_END);

        List<MiningLeaseApplication> miningLeases =
                miningLeaseApplicationRepository.findLeasesDueForMrpReminder(
                        from, to, MineRestorationService.STATUS_MRP_DRAFT);

        log.info("Found {} mining leases requiring Mine Restoration Plan reminder", miningLeases.size());

        for (MiningLeaseApplication lease : miningLeases) {
            try {
                boolean sent = sendReminder(
                        "mining lease",
                        lease.getApplicationNumber(),
                        lease.getApplicantUserId() != null ? lease.getApplicantUserId() : lease.getCreatedBy(),
                        lease.getApplicantName(),
                        lease.getApplicantEmail(),
                        lease.getLeaseEndDate()
                );
                if (sent) {
                    // Mark this lease term as reminded so tomorrow's run does not repeat it.
                    lease.setMrpReminderSentForEndDate(lease.getLeaseEndDate());
                    miningLeaseApplicationRepository.save(lease);
                }
            } catch (Exception e) {
                log.error("Failed to send Mine Restoration Plan reminder for mining lease {}",
                        lease.getApplicationNumber(), e);
            }
        }

        List<QuarryLeaseApplication> quarryLeases =
                quarryLeaseApplicationRepository.findLeasesDueForMrpReminder(
                        from, to, MineRestorationService.STATUS_MRP_DRAFT);

        log.info("Found {} quarry leases requiring Mine Restoration Plan reminder", quarryLeases.size());

        for (QuarryLeaseApplication lease : quarryLeases) {
            try {
                boolean sent = sendReminder(
                        "quarry lease",
                        lease.getApplicationNumber(),
                        lease.getApplicantUserId() != null ? lease.getApplicantUserId() : lease.getCreatedBy(),
                        lease.getApplicantName(),
                        lease.getApplicantEmail(),
                        lease.getLeaseEndDate()
                );
                if (sent) {
                    lease.setMrpReminderSentForEndDate(lease.getLeaseEndDate());
                    quarryLeaseApplicationRepository.save(lease);
                }
            } catch (Exception e) {
                log.error("Failed to send Mine Restoration Plan reminder for quarry lease {}",
                        lease.getApplicationNumber(), e);
            }
        }

        // Surface collection and stock lifting store their end date as a timestamp:
        // cover [from 00:00, end of the `to` day).
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime fromStart = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        List<SurfaceCollectionPermitEntity> permits =
                surfaceCollectionPermitRepository.findPermitsDueForMrpReminder(
                        Date.from(fromStart.atZone(zone).toInstant()),
                        Date.from(toExclusive.atZone(zone).toInstant()),
                        MineRestorationService.STATUS_MRP_DRAFT);

        log.info("Found {} surface collection permits requiring Mine Restoration Plan reminder", permits.size());

        for (SurfaceCollectionPermitEntity permit : permits) {
            try {
                // new Date(getTime()) because Hibernate may hand back a java.sql.Timestamp/Date,
                // and java.sql.Date does not support toInstant().
                LocalDate endDate = new Date(permit.getPermitValidityTo().getTime())
                        .toInstant().atZone(zone).toLocalDate();
                boolean sent = sendReminder(
                        "surface collection permit",
                        permit.getApplicationNo(),
                        permit.getCreatedBy(),
                        permit.getApplicantName(),
                        permit.getEmail(),
                        endDate
                );
                if (sent) {
                    permit.setMrpReminderSentForEndDate(permit.getPermitValidityTo());
                    surfaceCollectionPermitRepository.save(permit);
                }
            } catch (Exception e) {
                log.error("Failed to send Mine Restoration Plan reminder for surface collection permit {}",
                        permit.getApplicationNo(), e);
            }
        }

        List<StockLiftingApplication> stockLiftings =
                stockLiftingRepository.findApplicationsDueForMrpReminder(
                        fromStart, toExclusive, MineRestorationService.STATUS_MRP_DRAFT);

        log.info("Found {} stock lifting applications requiring Mine Restoration Plan reminder", stockLiftings.size());

        for (StockLiftingApplication stockLifting : stockLiftings) {
            try {
                boolean sent = sendReminder(
                        "stock lifting permit",
                        stockLifting.getApplicationNo(),
                        stockLifting.getCreatedBy(),
                        stockLifting.getApplicantName(),
                        stockLifting.getApplicantEmail(),
                        stockLifting.getValidityDate().toLocalDate()
                );
                if (sent) {
                    stockLifting.setMrpReminderSentForEndDate(stockLifting.getValidityDate());
                    stockLiftingRepository.save(stockLifting);
                }
            } catch (Exception e) {
                log.error("Failed to send Mine Restoration Plan reminder for stock lifting application {}",
                        stockLifting.getApplicationNo(), e);
            }
        }

        log.info("Completed Mine Restoration Plan reminder job");
    }

    /**
     * Sends the in-app notification (the reminder of record), then best-effort email
     * and SMS.
     *
     * @return false when the lease has no promoter user to notify
     */
    private boolean sendReminder(String leaseType,
                                 String applicationNumber,
                                 Long promoterUserId,
                                 String applicantName,
                                 String applicantEmail,
                                 LocalDate leaseEndDate) {

        if (promoterUserId == null) {
            log.warn("Skipping Mine Restoration Plan reminder for {} {} because no promoter user is linked",
                    leaseType, applicationNumber);
            return false;
        }

        String endDate = leaseEndDate.format(DATE_FORMAT);
        String deadline = leaseEndDate.minusMonths(SUBMISSION_MONTHS_BEFORE_LEASE_END).format(DATE_FORMAT);

        String title = "Mine Restoration Plan Submission";
        String message = "Your " + leaseType + " (Application No. "
                + applicationNumber
                + ") will end on "
                + endDate
                + ". If you wish to apply for Mine Restoration, please submit your Mine Restoration Plan on or before "
                + deadline
                + " (2.5 years before the lease end date). If you do not wish to apply for Mine Restoration,"
                + " you may proceed with lease renewal.";

        notificationClient.sendUserNotification(
                title,
                message,
                promoterUserId,
                MENU_ID_APPLICANT,
                "CITIZEN",
                true,
                applicationNumber
        );

        // Email is best-effort: the in-app notification above is the reminder of record.
        if (applicantEmail != null && !applicantEmail.isBlank()) {
            try {
                notificationClient.sendEmail(
                        applicantEmail,
                        title + " - " + applicationNumber,
                        "Dear " + (applicantName != null ? applicantName : "Promoter")
                                + ",\n\n" + message
                );
            } catch (Exception e) {
                log.warn("Mine Restoration Plan reminder email failed for {} {}", leaseType, applicationNumber, e);
            }
        }

        // SMS is fire-and-forget; masters resolves the promoter's phone number from the user id.
        smsClient.sendApplicationStatusSms(promoterUserId, applicationNumber, SMS_STAGE);

        log.info("Mine Restoration Plan reminder sent for {} {} (lease ends {}, MRP deadline {})",
                leaseType, applicationNumber, endDate, deadline);
        return true;
    }
}
