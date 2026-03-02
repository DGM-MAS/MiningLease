package com.mas.gov.bt.mas.primary.integration;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Client for sending notifications (Email/SMS).
 */
@Service
@Slf4j
public class NotificationClient {

    private final JavaMailSender mailSender;

    @Value("${app.email.from-address:mas@systems.gov.bt}")
    private String fromAddress;

    @Value("${app.email.from-name:Mines Administrative System}")
    private String fromName;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    private static final Logger logger = LoggerFactory.getLogger(NotificationClient.class);

    private static final String NOTIFICATION_API_URL =  "http://localhost:8082/api/notifications";
    private final RestTemplate restTemplate;

    public NotificationClient(JavaMailSender mailSender, RestTemplate restTemplate) {
        this.mailSender = mailSender;
        this.restTemplate = restTemplate;
    }

    /**
     * Send a simple text email.
     */
    @Async
    public void sendEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            log.info("Email disabled. Would have sent to: {} with subject: {}", to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
        }
    }

    /**
     * Send an HTML email.
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!emailEnabled) {
            log.info("Email disabled. Would have sent HTML email to: {} with subject: {}", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
        } catch (Exception e) {
            log.error("Unexpected error sending HTML email to: {}", to, e);
        }
    }

    /**
     * Send application submitted notification to applicant.
     */
    @Async
    public void sendApplicationSubmittedNotification(String email, String applicantName,
                                                      String applicationNumber) {
        String subject = "Quarry Lease Application Submitted - " + applicationNumber;
        String body = String.format("""
                Dear %s,

                Your quarry lease application has been submitted successfully.

                Application Number: %s

                You can track your application status using this reference number.
                We will notify you of any updates on your application.

                Thank you,
                Mines Administrative System
                Ministry of Energy and Natural Resources
                """, applicantName, applicationNumber);

        sendEmail(email, subject, body);
    }

    /**
     * Send status update notification to applicant.
     */
    @Async
    public void sendStatusUpdateNotification(String email, String applicantName,
                                              String applicationNumber, String newStatus, String remarks) {
        String subject = "Application Status Update - " + applicationNumber;
        String body = String.format("""
                Dear %s,

                Your quarry lease application status has been updated.

                Application Number: %s
                New Status: %s
                %s

                You can log in to the system to view more details.

                Thank you,
                Mines Administrative System
                Ministry of Energy and Natural Resources
                """, applicantName, applicationNumber, newStatus,
                remarks != null ? "Remarks: " + remarks : "");

        sendEmail(email, subject, body);
    }

    /**
     * Send approval notification to applicant.
     */
    @Async
    public void sendApprovalNotification(String email, String applicantName,
                                          String applicationNumber) {
        String subject = "Congratulations! Quarry Lease Application Approved - " + applicationNumber;
        String body = String.format("""
                Dear %s,

                We are pleased to inform you that your quarry lease application has been approved.

                Application Number: %s

                Please log in to the system to:
                1. View your approved lease details
                2. Complete any pending payments
                3. Download your lease certificate

                Thank you for using our services.

                Best regards,
                Mines Administrative System
                Ministry of Energy and Natural Resources
                """, applicantName, applicationNumber);

        sendEmail(email, subject, body);
    }

    /**
     * Send rejection notification to applicant.
     */
    @Async
    public void sendRejectionNotification(String email, String applicantName,
                                           String applicationNumber, String reason) {
        String subject = "Quarry Lease Application Update - " + applicationNumber;
        String body = String.format("""
                Dear %s,

                We regret to inform you that your quarry lease application could not be approved.

                Application Number: %s
                Reason: %s

                If you have any questions, please contact our support team.

                Thank you,
                Mines Administrative System
                Ministry of Energy and Natural Resources
                """, applicantName, applicationNumber, reason);

        sendEmail(email, subject, body);
    }

    /**
     * Send assignment notification to officer.
     */
    @Async
    public void sendAssignmentNotification(String email, String officerName,
                                            String applicationNumber, String stepName) {
        String subject = "New Application Assigned - " + applicationNumber;
        String body = String.format("""
                Dear %s,

                A new quarry lease application has been assigned to you for review.

                Application Number: %s
                Current Step: %s

                Please log in to the system to review and take action.

                Thank you,
                Mines Administrative System
                """, officerName, applicationNumber, stepName);

        sendEmail(email, subject, body);
    }


    /**
     * Send application fee required notification to applicant.
     */
    @Async
    public void sendApplicationFeeRequiredNotification(String email,
                                                       String applicantName,
                                                       String applicationNumber) {

        String subject = "Application Fee Required - Quarry Lease Application " + applicationNumber;

        String body = String.format("""
            Dear %s,

            Your quarry lease application is pending for payment of the required application fee.

            Application Number: %s

            Please log in to the system and complete the payment to proceed with further processing of your application.

            Your application will only be processed after successful payment of the application fee.

            If you have already made the payment, please ignore this message.

            Thank you,
            Mines Administrative System
            Ministry of Energy and Natural Resources
            """, applicantName, applicationNumber);

        sendEmail(email, subject, body);
    }

    @Async
    public void sendRevisionRequestNotification(String email, String applicantName,
                                                 String applicationNumber, String stage, String remarks) {
        String subject = "Additional Information Required - " + applicationNumber;
        String body = String.format("""
                Dear %s,

                Your quarry lease application requires additional information.

                Application Number: %s
                Review Stage: %s
                Remarks: %s

                Please log in to the system to provide the requested information and resubmit your application.

                Thank you,
                Mines Administrative System
                Ministry of Energy and Natural Resources
                """, applicantName, applicationNumber, stage, remarks != null ? remarks : "N/A");

        sendEmail(email, subject, body);
    }

    @Async
    public void sendGRSubmissionDeadlineNotification(String email, String applicantName,
                                                      String applicationNumber, String deadline) {
        String subject = "Geological Report Submission Deadline - " + applicationNumber;
        String body = String.format("""
                Dear %s,

                This is a reminder that the geological report for your quarry lease application is due.

                Application Number: %s
                Deadline: %s

                Please ensure the report is submitted before the deadline.

                Thank you,
                Mines Administrative System
                Ministry of Energy and Natural Resources
                """, applicantName, applicationNumber, deadline);

        sendEmail(email, subject, body);
    }

    @Async
    public void sendFMFSSubmissionNotification(String email, String applicantName,
                                                String applicationNumber) {
        String subject = "FMFS Submission Required - " + applicationNumber;
        String body = String.format("""
                Dear %s,

                Your quarry lease application requires submission of the Final Mine Feasibility Study (FMFS).

                Application Number: %s

                Please log in to the system to upload the required document.

                Thank you,
                Mines Administrative System
                Ministry of Energy and Natural Resources
                """, applicantName, applicationNumber);

        sendEmail(email, subject, body);
    }

    @Async
    public void sendMLASigningNotification(String email, String applicantName,
                                            String applicationNumber) {
        String subject = "MLA Signed - Quarry Lease Application " + applicationNumber;
        String body = String.format("""
                Dear %s,

                The Mining Lease Agreement (MLA) for your quarry lease application has been signed.

                Application Number: %s

                Please log in to the system to download your signed MLA document.

                Thank you,
                Mines Administrative System
                Ministry of Energy and Natural Resources
                """, applicantName, applicationNumber);

        sendEmail(email, subject, body);
    }

    @Async
    public void sendWorkOrderNotification(String email, String applicantName,
                                           String applicationNumber) {
        String subject = "Work Order Issued - Quarry Lease Application " + applicationNumber;
        String body = String.format("""
                Dear %s,

                A work order has been issued for your quarry lease application.

                Application Number: %s

                Please log in to the system to view and download your work order.

                Thank you,
                Mines Administrative System
                Ministry of Energy and Natural Resources
                """, applicantName, applicationNumber);

        sendEmail(email, subject, body);
    }

    @Async
    public void sendTaskReassignmentNotification(String email, String officerName,
                                                  String applicationNumber, String role) {
        if (email == null) {
            log.info("No email provided for task reassignment notification. Application: {}, Role: {}", applicationNumber, role);
            return;
        }
        String subject = "Task Reassigned - " + applicationNumber;
        String body = String.format("""
                Dear %s,

                A quarry lease application task has been reassigned to you.

                Application Number: %s
                Role: %s

                Please log in to the system to review and take action.

                Thank you,
                Mines Administrative System
                """, officerName != null ? officerName : "Officer", applicationNumber, role);

        sendEmail(email, subject, body);
    }

    public void sendUserNotification(String title, String message, Long userId, String serviceId) {

        String url = UriComponentsBuilder
                .fromHttpUrl(NOTIFICATION_API_URL + "/user/{userId}")
                .queryParam("title", title)
                .queryParam("message", message)
                .queryParam("serviceId", serviceId)
                .buildAndExpand(userId)
                .toUriString();

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, null, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("✅ Notification sent successfully to user {} | Response: {}",
                        userId, response.getBody());
            } else {
                logger.warn("⚠️ Notification API returned non-success status: {} | Body: {}",
                        response.getStatusCode(), response.getBody());
            }

        } catch (HttpStatusCodeException e) {
            logger.error("❌ Notification API responded with error: {} | Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("❌ Failed to send notification to user {} | Reason: {}",
                    userId, e.getMessage(), e);
        }
    }

    public void sendMailToDirectorAssigned(String directorEmail, String directorName, String applicationNumber) {
        String subject = "New Application Assigned - " + applicationNumber;
        String body = String.format("""
                Dear %s,

                A new quarry lease application has been assigned to you.
                Assign MPCD and Geologist focal respectively. 

                Application Number: %s

                Please log in to the system to review and take action.

                Thank you,
                Mines Administrative System
                """, directorName, applicationNumber);

        sendEmail(directorEmail, subject, body);
    }
}
