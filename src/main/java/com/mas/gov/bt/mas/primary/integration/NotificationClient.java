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

import java.net.URI;

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

    // SMS config mirrors mas-backend-masters' SmsProperties/SmsService (same G2C
    // gateway) — duplicated here rather than shared cross-module, same reasoning
    // as PasswordGenerator: MiningLease is a separate deployable with no shared
    // dependency on masters.
    @Value("${app.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${app.sms.base-url:http://172.30.16.136/g2csms/push.php}")
    private String smsBaseUrl;

    @Value("${app.sms.country-code:975}")
    private String smsCountryCode;

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
     * Send the auto-generated login credentials to an applicant whose citizen
     * account was just provisioned from a manual-entry submission.
     */
    @Async
    public void sendApplicantAccountCredentialsEmail(String to, String fullName, String registrationType, String tempPassword) {
        if (!emailEnabled) {
            log.info("Email disabled. Would have sent applicant account credentials to: {}", to);
            return;
        }

        String typeDisplay = switch (registrationType) {
            case "REGISTERED_COMPANY" -> "Registered Company";
            case "BUSINESS_LICENSE" -> "Business License Holder";
            default -> "Individual";
        };

        String subject = "Welcome to MAS - Your Account Has Been Created";
        String htmlContent = String.format("""
                <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #28a745; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0;">
                        <h1 style="margin:0;">Welcome to MAS!</h1>
                    </div>
                    <div style="background-color: #f9f9f9; padding: 30px; border: 1px solid #ddd; border-radius: 0 0 5px 5px;">
                        <p>Dear %s,</p>
                        <p>Your %s account has been automatically created in the Mines Administration System (MAS)
                        following your recent manual-entry application submission.</p>
                        <div style="background-color: #fff; padding: 15px; border-left: 4px solid #28a745; margin: 20px 0;">
                            <h3 style="margin-top:0;">Your Login Credentials</h3>
                            <p><strong>Email:</strong><br><span style="font-family: monospace; color:#28a745;">%s</span></p>
                            <p><strong>Temporary Password:</strong><br><span style="font-family: monospace; color:#28a745;">%s</span></p>
                        </div>
                        <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0;">
                            <strong>Important Security Notice:</strong>
                            <ul>
                                <li>This is a <strong>temporary password</strong> that you must change upon first login</li>
                                <li>Please do not share this password with anyone</li>
                            </ul>
                        </div>
                        <p>You can log in to the system to track the status of your application.</p>
                    </div>
                </div>
                """, fullName, typeDisplay, to, tempPassword);

        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * Send a raw SMS via the G2C gateway.
     */
    @Async
    public void sendSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.info("SMS disabled. Would have sent to: {}", phoneNumber);
            return;
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.info("Skipping SMS - no phone number supplied");
            return;
        }

        try {
            String fullNumber = formatPhoneNumber(phoneNumber);

            URI uri = UriComponentsBuilder.fromUriString(smsBaseUrl)
                    .queryParam("to", fullNumber)
                    .queryParam("msg", message)
                    .build()
                    .toUri();

            String response = restTemplate.getForObject(uri, String.class);
            log.info("SMS sent successfully to: {}. Response: {}", fullNumber, response);
        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", phoneNumber, e);
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        String cleaned = phoneNumber.replaceAll("[\\s\\-()]+", "");
        if (cleaned.startsWith(smsCountryCode)) {
            return cleaned;
        }
        return smsCountryCode + cleaned;
    }

    /**
     * Send the auto-generated login credentials via SMS to an applicant whose
     * citizen account was just provisioned from a manual-entry submission.
     */
    @Async
    public void sendApplicantAccountCredentialsSms(String phoneNumber, String username, String tempPassword) {
        String message = "Welcome to MAS! Your username: " + username + ", Temporary password: " + tempPassword
                + ". Please change your password on first login.";
        sendSms(phoneNumber, message);
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
    public void sendEnvironmentalClearanceAssignmentNotification(String email, String officerName,
                                           String applicationNumber, String stepName) {
        String subject = "New Application Assigned - " + applicationNumber;
        String body = String.format("""
                A new environmental clearance application has been assigned to you for review.

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
    public void sendTaskRenewalEnvironmentalClearanceReassignmentNotification(String email, String officerName,
                                                 String applicationNumber, String role, String remarks) {
        if (email == null) {
            log.info("No email provided for task reassignment notification. Application: {}, Role: {}", applicationNumber, role);
            return;
        }
        String subject = "Task Reassigned - " + applicationNumber;
        String body = String.format("""
                A renewal environmental clearance application task has been reassigned to you.

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

    public void sendSurfaceCollectionAuctionMailToMDAssigned(String directorEmail, String directorName, String applicationNumber) {
        String subject = "New Application Assigned - " + applicationNumber;
        String body = String.format("""
                A new surface collection auction application has been assigned to you.
                Once the offline auction data are completed review the bank guarantor details. 

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

    public void sendSurfaceCollectionAuctionMailToMDBGSubmitted(String directorEmail, String directorName, String applicationNumber) {
        String subject = "BG Submitted  - " + applicationNumber;
        String body = String.format("""
                Bank guarantor details has been submitted for surface collection auction application.

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

    public void sendSurfaceCollectionAuctionMailToPromoterBGResubmit(String directorEmail, String directorName, String applicationNumber) {
        String subject = "BG Resubmit requested  - " + applicationNumber;
        String body = String.format("""
                Bank guarantor details has been reviewed for surface collection auction application. Please resubmit the bank guarantor details.

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

    public void sendSurfaceCollectionAuctionWinnerEmail(String directorEmail, String directorName, String applicationNumber) {
        String subject = "Congratulation. You are the Winner - " + applicationNumber;
        String body = String.format("""
                Surface collection auction application has been won by you. Congratulations
                Upload the bank guarantor details to proceed further.

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
