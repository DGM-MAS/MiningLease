package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Partial read/write mapping onto mas_db.t_citizens, the applicant-account table
 * owned by mas-backend-masters (CitizenService/Citizen entity). MiningLease writes
 * into it directly here — same cross-module DB-access convention already used by
 * ManualMiningEntryRepository.findUserDetails() against mas_db.users — so that a
 * manual-entry submission can auto-provision the applicant's citizen login.
 *
 * Only the columns this module needs to read/write are mapped; the full column
 * set (login tracking, lockout, etc.) lives in masters' own Citizen entity.
 */
@Entity
@Table(name = "t_citizens", schema = "mas_db")
@Data
@NoArgsConstructor
public class ApplicantAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_type", length = 50)
    private String registrationType;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "contact", length = 20)
    private String contact;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "force_password_change")
    private Boolean forcePasswordChange;

    @Column(name = "cid", length = 20)
    private String cid;

    @Column(name = "license_no", length = 50)
    private String licenseNo;

    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(name = "business_owner", length = 200)
    private String businessOwner;

    @Column(name = "company_registration_number", length = 50)
    private String companyRegistrationNumber;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "company_type", length = 100)
    private String companyType;

    @Column(name = "account_status", length = 50)
    private String accountStatus;

    @Column(name = "citizen_level")
    private Integer citizenLevel;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
