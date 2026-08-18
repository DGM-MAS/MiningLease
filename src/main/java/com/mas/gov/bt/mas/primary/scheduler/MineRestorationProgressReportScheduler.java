package com.mas.gov.bt.mas.primary.scheduler;

import com.mas.gov.bt.mas.primary.entity.MineRestorationApplication;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.repository.MineRestorationApplicationRepository;
import com.mas.gov.bt.mas.primary.repository.MineRestorationProgressReportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MineRestorationProgressReportScheduler {

    private final MineRestorationApplicationRepository restorationApplicationRepository;

    private final MineRestorationProgressReportRepository progressReportRepository;

    private final NotificationClient notificationClient;

    private String MENU_ID_APPLICANT       = "121"; // "Promoter Application List" (/MineRestorationappliantlist)

    /*
     * Runs every day at 9:00 AM.
     *
     * Server timezone should be configured appropriately.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void sendProgressReportReminders() {

        log.info(
                "Starting Mine Restoration progress report reminder job"
        );

        LocalDate today = LocalDate.now();

        List<String> activeStatuses = List.of(
                "RESTORATION_IN_PROGRESS"
        );

        List<MineRestorationApplication> applications =
                restorationApplicationRepository
                        .findProgressReportsDue(
                                today,
                                activeStatuses
                        );

        log.info(
                "Found {} Mine Restoration applications requiring progress report reminder",
                applications.size()
        );

        for (MineRestorationApplication restoration : applications) {

            try {

                processReminder(restoration);

            } catch (Exception e) {

                log.error(
                        "Failed to send progress report reminder for application {}",
                        restoration.getApplicationNumber(),
                        e
                );
            }
        }

        log.info(
                "Completed Mine Restoration progress report reminder job"
        );
    }

    private void processReminder(
            MineRestorationApplication restoration) {

        if (restoration.getWorkOrderIssuedAt() == null) {

            log.warn(
                    "Skipping application {} because work order issue date is null",
                    restoration.getApplicationNumber()
            );

            return;
        }

        if (restoration.getNextProgressReportDueDate() == null) {

            log.warn(
                    "Skipping application {} because next progress report due date is null",
                    restoration.getApplicationNumber()
            );

            return;
        }

        /*
         * Determine which progress report is due.
         *
         * Example:
         *
         * 0 submitted -> Report #1
         * 1 submitted -> Report #2
         * 2 submitted -> Report #3
         */
        long submittedReports =
                progressReportRepository
                        .countSubmittedReports(
                                restoration.getApplicationNumber()
                        );

        int nextReportNumber =
                (int) submittedReports + 1;

        /*
         * Extra safety check.
         *
         * If the report for this cycle has already been submitted,
         * do not send another reminder.
         */
        boolean alreadySubmitted =
                progressReportRepository
                        .existsByRestorationApplicationNumberAndProgressReportNumberAndStatus(
                                restoration.getApplicationNumber(),
                                nextReportNumber,
                                "PROGRESS_REPORT_SUBMITTED"
                        );

        if (alreadySubmitted) {

            log.info(
                    "Progress Report #{} already submitted for application {}",
                    nextReportNumber,
                    restoration.getApplicationNumber()
            );

            return;
        }

        /*
         * Send notification to promoter.
         */
        notificationClient.sendUserNotification(
                "Mine Restoration Progress Report Due",
                "Progress Report #"
                        + nextReportNumber
                        + " for Mine Restoration application "
                        + restoration.getApplicationNumber()
                        + " is now due. Please submit the progress "
                        + "report for the restoration work.",
                restoration.getApplicantUserId(),
                MENU_ID_APPLICANT,
                "CITIZEN",
                false,
                restoration.getApplicationNumber()
        );

        /*
         * Also send email if the NotificationClient supports
         * sending email directly.
         *
         * Uncomment/adapt this according to your existing
         * NotificationClient method.
         */
        /*
        notificationClient.sendEmail(
                restoration.getApplicantEmail(),
                "Mine Restoration Progress Report Due",
                "Your progress report for application "
                        + restoration.getApplicationNumber()
                        + " is now due."
        );
        */

        /*
         * Mark reminder as sent.
         *
         * This prevents tomorrow's cron execution from
         * sending the same reminder again.
         */
        restoration.setProgressReportReminderSentAt(
                LocalDateTime.now()
        );

        restorationApplicationRepository.save(
                restoration
        );

        log.info(
                "Progress Report #{} reminder sent successfully for application {}",
                nextReportNumber,
                restoration.getApplicationNumber()
        );
    }
}