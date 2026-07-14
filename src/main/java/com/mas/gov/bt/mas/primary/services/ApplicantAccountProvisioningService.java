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

/**
 * Auto-provisions a citizen (applicant) login the first time an applicant is seen
 * in a manual-entry submission, and emails them the temporary credentials.
 *
 * Runs in its own transaction, isolated from the manual-entry submission that
 * triggers it: account provisioning is a best-effort side effect and must never
 * cause (or be caused to roll back by) the application submission itself.
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
    public void provisionForApplicant(String applicantType, String applicantCid, String applicantName,
                                       String applicantContact, String applicantEmail,
                                       String licenseNo, String businessLicenseNo,
                                       String companyRegistrationNo, String companyName, String companyType) {
        try {
            String email = trim(applicantEmail);
            if (email.isEmpty()) {
                log.info("Skipping applicant account provisioning - no applicant email supplied");
                return;
            }

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
                log.info("Skipping applicant account provisioning - no CID/license/registration number identifying {}", email);
                return;
            }

            boolean alreadyExists = switch (registrationType) {
                case "REGISTERED_COMPANY" -> applicantAccountRepository.existsByCompanyRegistrationNumber(identifier);
                case "BUSINESS_LICENSE" -> applicantAccountRepository.existsByLicenseNo(identifier);
                default -> applicantAccountRepository.existsByCid(identifier);
            };
            if (alreadyExists) {
                log.info("Applicant account already exists ({}: {}), skipping auto-registration", registrationType, identifier);
                return;
            }
            if (applicantAccountRepository.existsByEmail(email)) {
                log.info("An account already exists with email {}, skipping auto-registration to avoid a conflicting duplicate", email);
                return;
            }

            String tempPassword = PasswordGenerator.generatePassword(12);

            ApplicantAccount account = new ApplicantAccount();
            account.setRegistrationType(registrationType);
            account.setEmail(email);
            account.setFullName(!trim(fullName).isEmpty() ? fullName : email);
            account.setContact(applicantContact);
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

            notificationClient.sendApplicantAccountCredentialsEmail(email, account.getFullName(), registrationType, tempPassword);
            notificationClient.sendApplicantAccountCredentialsSms(applicantContact, email, tempPassword);
            log.info("Auto-registered applicant account id {} ({}) from manual entry submission and queued credentials email/SMS",
                    saved.getId(), registrationType);
        } catch (Exception e) {
            log.error("Failed to auto-register applicant account for manual entry submission (email={})", applicantEmail, e);
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
