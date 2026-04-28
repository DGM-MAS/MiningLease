package com.mas.gov.bt.mas.primary.integration;

import com.mas.gov.bt.mas.primary.dto.request.EmailRequest;
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
    private static final String NOTIFICATION_API_URL_EMAIL_BUILDER =  "http://localhost:8082/api/notifications/send";
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
        String subject = "Mining Lease Application Submitted - " + applicationNumber;
        String body = String.format("""
                Your Mining lease application has been submitted successfully.

                Application Number: %s

                You can track your application status using this reference number.
                We will notify you of any updates on your application.
                """, applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }

    /**
     * Send application submitted notification to applicant.
     */
    @Async
    public void sendApplicationSubmittedManualEntryNotification(String email, String applicantName,
                                                     String applicationNumber) {
        String subject = "Manual Entry Application Submitted - " + applicationNumber;
        String body = String.format("""
                Your Manual entry application has been submitted successfully.

                Application Number: %s

                You can track your application status using this reference number.
                We will notify you of any updates on your application.
                """, applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }

    /**
     * Send status update notification to applicant.
     */
    @Async
    public void sendStatusUpdateNotification(String email, String applicantName,
                                              String applicationNumber, String newStatus, String remarks) {
        String subject = "Application Status Update - " + applicationNumber;
        String body = String.format("""
                Your Mining lease application status has been updated.

                Application Number: %s
                New Status: %s
                %s

                You can log in to the system to view more details.
                """,  applicationNumber, newStatus,
                remarks != null ? "Remarks: " + remarks : "");

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }

    /**
     * Send approval notification to applicant.
     */
    @Async
    public void sendApprovalNotification(String email, String applicantName,
                                          String applicationNumber) {
        String subject = "Congratulations! Mining Lease Application Approved - " + applicationNumber;
        String body = String.format("""
                We are pleased to inform you that your mining lease application has been approved.

                Application Number: %s

                Please log in to the system to:
                1. View your approved lease details
                2. Complete any pending payments
                3. Download your lease certificate

                Thank you for using our services.
                """, applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );

    }


    /**
     * Send approval notification to applicant.
     */
    @Async
    public void sendApprovalManualEntryNotification(String email, String applicantName,
                                         String applicationNumber) {
        String subject = "Congratulations! Manual Entry Application Approved - " + applicationNumber;
        String body = String.format("""
                We are pleased to inform you that your manual entry application has been approved.

                Application Number: %s

                Please log in to the system to:
                1. Upload MLA for Signing

                Thank you for using our services.
                """, applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );

    }

    /**
     * Send approval notification to applicant.
     */
    @Async
    public void sendApprovalSampleTransportNotification(String email, String applicantName,
                                         String applicationNumber) {
        String subject = "Congratulations! Sample Transport clearance Application Approved - " + applicationNumber;
        String body = String.format("""
                We are pleased to inform you that your sample transport clearance application has been approved.

                Application Number: %s

                Please log in to the system to:
                1. View your approved transport clearance details
                2. Download your trasport clearance.

                Thank you for using our services.
                """, applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );

    }


    @Async
    public void sendUpliftingSuspensionNotification(String email, String applicantName,
                                         String applicationNumber) {
        String subject = "Congratulations! Mining Lease Application uplifting suspension has be uplifted. - " + applicationNumber;
        String body = String.format("""
                We are pleased to inform you that your lease suspension application has been uplifted.

                Application Number: %s

                Please log in to the system to:
                1. View your immediate suspension application details
                
                Thank you for using our services.
                """, applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }

    /**
     * Send rejection notification to applicant.
     */
    @Async
    public void sendRejectionNotification(String email, String applicantName,
                                           String applicationNumber, String reason) {
        String subject = "Mining Lease Application Update - " + applicationNumber;
        String body = String.format("""
                We regret to inform you that your mining lease application could not be approved.

                Application Number: %s
                Reason: %s

                If you have any questions, please contact our support team.
                """, applicationNumber, reason);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }

    /**
     * Send rejection notification to applicant.
     */
    @Async
    public void sendRejectionSampleTransportNotification(String email, String applicantName,
                                          String applicationNumber, String reason) {
        String subject = "Sample Transport clearance Application Update - " + applicationNumber;
        String body = String.format("""
                We regret to inform you that your  application transport clearance could not be approved.

                Application Number: %s
                Reason: %s

                If you have any questions, please contact our support team.
                """, applicationNumber, reason);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }

    /**
     * Send rejection notification to applicant.
     */
    @Async
    public void sendRejectionManualEntryNotification(String email, String applicantName,
                                          String applicationNumber, String reason) {
        String subject = "Manual Entry Application Update - " + applicationNumber;
        String body = String.format("""
                We regret to inform you that your manual entry application could not be approved.

                Application Number: %s
                Reason: %s

                If you have any questions, please contact our support team.
                """, applicationNumber, reason);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }


    @Async
    public void GSDFocalAcceptedReviewedNotification(
            String email,
            String chiefName,
            String applicationNumber) {
        String subject = "Sample Transport clearance Application has been reviewed - " + applicationNumber;
        String body = String.format("""
                GSD Focal has accepted the application. Please log in to the system to review and take action.

                Application Number: %s

                """, applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(chiefName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }

    /**
     * Send assignment notification to officer.
     */
    @Async
    public void sendAssignmentNotification(String email, String officerName,
                                            String applicationNumber, String stepName) {
        String subject = "New Application Assigned - " + applicationNumber;
        String body = String.format("""
                A new quarry lease application has been assigned to you for review.

                Application Number: %s
                Current Step: %s

                Please log in to the system to review and take action.
                """, applicationNumber, stepName);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(officerName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }

    /**
     * Send assignment notification to officer.
     */
    @Async
    public void sendAssignmentManualEntryNotification(String email, String officerName,
                                           String applicationNumber, String stepName) {
        String subject = "New Application Assigned - " + applicationNumber;
        String body = String.format("""
                A new manual entry application has been assigned to you for review.

                Application Number: %s
                Current Step: %s

                Please log in to the system to review and take action.
                """, applicationNumber, stepName);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(officerName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }


    /**
     * Send application fee required notification to applicant.
     */
    @Async
    public void sendApplicationFeeRequiredNotification(String email,
                                                       String applicantName,
                                                       String applicationNumber) {

        String subject = "Application Fee Required - Mining Lease Application " + applicationNumber;

        String body = String.format("""
            Your mining lease application is pending for payment of the required application fee.

            Application Number: %s

            Please log in to the system and complete the payment to proceed with further processing of your application.

            Your application will only be processed after successful payment of the application fee.

            If you have already made the payment, please ignore this message.
            """, applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }

    @Async
    public void sendRevisionRequestNotification(String email, String applicantName,
                                                 String applicationNumber, String stage, String remarks) {
        String subject = "Additional Information Required - " + applicationNumber;
        String body = String.format("""
                Your temporary closure application requires additional information.

                Application Number: %s
                Review Stage: %s
                Remarks: %s

                Please log in to the system to provide the requested information and resubmit your application.
                """, applicationNumber, stage, remarks != null ? remarks : "N/A");

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );

    }

    @Async
    public void sendTerminationRevisionRequestNotification(String email, String applicantName,
                                                String applicationNumber, String stage, String remarks) {
        String subject = "Additional Information Required for termination process - " + applicationNumber;
        String body = String.format("""
                Your mining lease application are under termination review please provide requires additional information.

                Application Number: %s
                Review Stage: %s
                Remarks: %s

                Please log in to the system to provide the requested information and resubmit your additional information.
                """, applicationNumber, stage, remarks != null ? remarks : "N/A");

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }


    @Async
    public void sendImmediateSuspensionRevisionRequestPromoterNotification(String email, String applicantName,
                                                           String applicationNumber, String stage, String remarks) {
        String subject = "Rectification from promoter - " + applicationNumber;
        String body = String.format("""
                Additional information from promoter has been provided. Please login to review the application.

                Application Number: %s
                Review Stage: %s
                Remarks: %s

                Please log in to the system to provide the requested information and resubmit your additional information.

                """, applicationNumber, stage, remarks != null ? remarks : "N/A");

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );

    }

    @Async
    public void sendGRSubmissionDeadlineNotification(String email, String applicantName,
                                                      String applicationNumber, String deadline) {
        String subject = "Geological Report Submission Deadline - " + applicationNumber;
        String body = String.format("""
                This is a reminder that the geological report for your mining lease application is due.

                Application Number: %s
                Deadline: %s

                Please ensure the report is submitted before the deadline.
                """, applicationNumber, deadline);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );

    }

    @Async
    public void sendFMFSSubmissionNotification(String email, String applicantName,
                                                String applicationNumber) {
        String subject = "FMFS Submission Required - " + applicationNumber;
        String body = String.format("""
                Your mining lease application requires submission of the Final Mine Feasibility Study (FMFS).

                Application Number: %s

                Please log in to the system to upload the required document.

                """, applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );

    }

    @Async
    public void sendMLASigningNotification(String email, String applicantName,
                                            String applicationNumber) {
        String subject = "MLA Signed - Mining Lease Application " + applicationNumber;
        String body = String.format("""
                The Mining Lease Agreement (MLA) for your mining lease application has been signed.

                Application Number: %s

                Please log in to the system to download your signed MLA document.

                """, applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );

    }

    @Async
    public void sendWorkOrderNotification(String email, String applicantName,
                                           String applicationNumber) {
        String subject = "Work Order Issued - Mining Lease Application " + applicationNumber;
        String body = String.format("""
                A work order has been issued for your mining lease application.

                Application Number: %s

                Please log in to the system to view and download your work order.
                """,  applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );

    }

    @Async
    public void sendTaskReassignmentNotification(String email, String officerName,
                                                  String applicationNumber, String role, String remarks) {
        if (email == null) {
            log.info("No email provided for task reassignment notification. Application: {}, Role: {}", applicationNumber, role);
            return;
        }
        String subject = "Task Reassigned - " + applicationNumber;
        String body = String.format("""
                A mining lease application task has been reassigned to you.

                Application Number: %s
                Remarks: %s
                Role: %s

                Please log in to the system to review and take action.
                """, applicationNumber, remarks, role);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(officerName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );

    }

    @Async
    public void sendTaskReassignmentNotificationTermination(String email, String officerName,
                                                 String applicationNumber, String role) {
        if (email == null) {
            log.info("No email provided for task reassignment termination notification. Application: {}, Role: {}", applicationNumber, role);
            return;
        }
        String subject = "Task Reassigned - " + applicationNumber;
        String body = String.format("""
                A termination application task has been reassigned to you.

                Application Number: %s
                Role: %s

                Please log in to the system to review and take action.

                """, applicationNumber, role);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(officerName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );

    }

    @Async
    public void sendTerminationNotification(String email, String officerName,
                                                 String applicationNumber) {
        if (email == null) {
            log.info("No email provided for termination notification. Application: {}", applicationNumber );
            return;
        }
        String subject = "Termination Notification - " + applicationNumber;
        String body = String.format("""
                The termination of mining has been approved by CMS Head.

                Application Number: %s

                Please log in to the system to review the details of termination.
                """,  applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(officerName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }

    @Async
    public void sendTerminationCancellationNotification(String email, String officerName,
                                            String applicationNumber) {
        if (email == null) {
            log.info("No email provided for termination notification. Application: {}", applicationNumber );
            return;
        }
        String subject = "Termination Cancelled  - " + applicationNumber;
        String body = String.format("""
                The termination of mining lease has been cancelled by CMS Head.

                Application Number: %s

                Please log in to the system to review the details.

                """, applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(officerName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );

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

    public void sendMiningLeaseMailToDirectorAssigned(String directorEmail, String directorName, String applicationNumber) {
        String subject = "New Application Assigned - " + applicationNumber;
        String body = String.format("""
                A new mining lease application has been assigned to you.
                Assign MPCD and Geologist focal respectively.

                Application Number: %s

                Please log in to the system to review and take action.
                """,  applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(directorEmail);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(directorName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }

    public void sendTerminationMailToCMSHeadAssigned(String directorEmail, String directorName, String applicationNumber) {
        String subject = "New Application Assigned - " + applicationNumber;
        String body = String.format("""
                A new termination application has been assigned to you by the system.
                
                Application Number: %s

                Please log in to the system to review and take action.

                """, applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(directorEmail);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(directorName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );
    }

    /**
     * Send application submitted notification to applicant.
     */
    @Async
    public void sendApplicationSuspendedNotification(String email, String applicantName,
                                                     String applicationNumber) {
        String subject = "Mining Lease Application has been suspended - " + applicationNumber;
        String body = String.format("""
                Your Mining lease application has been suspended.

                Application Number: %s

                """, applicationNumber);

        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject(subject);
        request.setBody(body);
        request.setRecipientName(applicantName);

        restTemplate.postForObject(
                NOTIFICATION_API_URL_EMAIL_BUILDER,
                request,
                String.class
        );

    }
}
