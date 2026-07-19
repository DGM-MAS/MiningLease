package com.mas.gov.bt.mas.primary.services;

import com.mas.gov.bt.mas.primary.entity.ApplicantAccount;
import com.mas.gov.bt.mas.primary.integration.NotificationClient;
import com.mas.gov.bt.mas.primary.repository.ApplicantAccountRepository;
import com.mas.gov.bt.mas.primary.utility.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Auto-provisions a citizen (applicant) login the first time an applicant is seen
 * in a manual-entry submission, and emails them the temporary credentials.
 *
 * Runs in its own transaction, isolated from the manual-entry submission that
 * triggers it: account provisioning is a best-effort side effect and must never
 * cause (or be caused to roll back by) the application submission itself.
 *
 * Also used synchronously (not just fire-and-forget) by callers that need the
 * resolved citizen account id immediately — e.g. attributing an issued permit
 * to the actual applicant instead of the staff member who processed it. Returns
 * the existing or newly-created account id, or null if it couldn't resolve one.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicantAccountProvisioningService {

    private static final String DEFAULT_ROLE = "Promoter";

    private final ApplicantAccountRepository applicantAccountRepository;
    private final NotificationClient notificationClient;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long provisionForApplicant(String applicantType, String applicantCid, String applicantName,
                                       String applicantContact, String applicantEmail,
                                       String licenseNo, String businessLicenseNo,
                                       String companyRegistrationNo, String companyName, String companyType) {
        try {
            String email = trim(applicantEmail);
            String contact = trim(applicantContact);

            String normalizedType = trim(applicantType).toLowerCase();
            String effectiveLicenseNo = firstNonBlank(businessLicenseNo, licenseNo);
            String companyRegNo = trim(companyRegistrationNo);

            String registrationType;
            String identifier;
            String fullName;

            if ("company".equals(normalizedType) || !companyRegNo.isEmpty()) {
                registrationType = "REGISTERED_COMPANY";
                identifier = companyRegNo;
                fullName = firstNonBlank(companyName, applicantName);
            } else if ("licensed".equals(normalizedType) || !effectiveLicenseNo.isEmpty()) {
                registrationType = "BUSINESS_LICENSE";
                identifier = effectiveLicenseNo;
                fullName = firstNonBlank(companyName, applicantName);
            } else {
                registrationType = "INDIVIDUAL";
                identifier = trim(applicantCid);
                fullName = applicantName;
            }

            if (identifier.isEmpty()) {
                log.info("Skipping applicant account provisioning - no CID/license/registration number identifying {}", email.isEmpty() ? contact : email);
                return null;
            }

            // Resolve an existing account first — doesn't need email/contact at all,
            // only needed below when we have to create a brand new account.
            Optional<ApplicantAccount> existing = switch (registrationType) {
                case "REGISTERED_COMPANY" -> applicantAccountRepository.findByCompanyRegistrationNumber(identifier);
                case "BUSINESS_LICENSE" -> applicantAccountRepository.findByLicenseNo(identifier);
                default -> applicantAccountRepository.findByCid(identifier);
            };
            if (existing.isPresent()) {
                log.info("Applicant account already exists ({}: {}), reusing id {}", registrationType, identifier, existing.get().getId());
                return existing.get().getId();
            }

            if (email.isEmpty() && contact.isEmpty()) {
                log.info("Skipping applicant account provisioning - no email or contact number supplied, no way to deliver credentials");
                return null;
            }
            if (!email.isEmpty() && applicantAccountRepository.existsByEmail(email)) {
                log.info("An account already exists with email {}, skipping auto-registration to avoid a conflicting duplicate", email);
                return null;
            }

            String tempPassword = PasswordGenerator.generatePassword(12);

            ApplicantAccount account = new ApplicantAccount();
            account.setRegistrationType(registrationType);
            account.setUsername(identifier);
            account.setEmail(email.isEmpty() ? null : email);
            account.setFullName(!trim(fullName).isEmpty() ? fullName : (!email.isEmpty() ? email : identifier));
            account.setContact(contact.isEmpty() ? null : contact);
            account.setPassword(passwordEncoder.encode(tempPassword));
            account.setForcePasswordChange(true);
            account.setAccountStatus("ACTIVE");
            account.setCitizenLevel(1);

            switch (registrationType) {
                case "REGISTERED_COMPANY" -> {
                    account.setCompanyRegistrationNumber(identifier);
                    account.setCompanyName(companyName);
                    account.setCompanyType(companyType);
                }
                case "BUSINESS_LICENSE" -> {
                    account.setLicenseNo(identifier);
                    account.setBusinessName(companyName);
                }
                default -> account.setCid(identifier);
            }

            ApplicantAccount saved = applicantAccountRepository.save(account);
            applicantAccountRepository.assignRole(saved.getId(), DEFAULT_ROLE);

            if (!email.isEmpty()) {
                notificationClient.sendApplicantAccountCredentialsEmail(email, account.getFullName(), registrationType, tempPassword, identifier);
            }
            if (!contact.isEmpty()) {
                notificationClient.sendApplicantAccountCredentialsSms(contact, identifier, tempPassword);
            }
            log.info("Auto-registered applicant account id {} ({}, username={}) from manual entry submission and queued credentials via {}",
                    saved.getId(), registrationType, identifier,
                    !email.isEmpty() && !contact.isEmpty() ? "email+SMS" : (!email.isEmpty() ? "email" : "SMS"));
            return saved.getId();
        } catch (Exception e) {
            log.error("Failed to auto-register applicant account for manual entry submission (email={})", applicantEmail, e);
            return null;
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }
}
