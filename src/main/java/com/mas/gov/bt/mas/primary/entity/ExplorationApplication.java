package com.mas.gov.bt.mas.primary.entity;


import com.mas.gov.bt.mas.primary.utility.EPApplicantType;
import com.mas.gov.bt.mas.primary.utility.ExplorationApplicationStatus;
import com.mas.gov.bt.mas.primary.utility.FinanceSource;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exploration_application")
@Data
public class ExplorationApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String createdBy;

    // -------------------------
    // Applicant Details
    // -------------------------
    @Enumerated(EnumType.STRING)
    private EPApplicantType applicantType;

    // Application number
    private String applicationNo;

    // Individual
    private String cid;

    private String applicantName;
    // Company
    private String licenseNo;

    private String companyName;

    private String mobileNo;
    private String email;

    // Business License
    private String businessName;
    private String businessOwner;
    private String businessLicenseNo;

    // -------------------------
    // Eligibility Snapshot
    // -------------------------
    private Boolean taxClearanceValid;
    private Boolean securityClearanceValid;
    private Integer householdActiveLeases;
    private Integer companyActiveLeases;

    // -------------------------
    // Exploration Details
    // -------------------------
    private String mineralOfInterest;

    private String proposedDuration;

    @Enumerated(EnumType.STRING)
    private FinanceSource financeSource;

    @Column(length = 2000)
    private String technicalCompetence;

    @Column(length = 2000)
    private String workforceRequirement;

    // -------------------------
    // Land Ownership
    // -------------------------
    private Double srfAcres;
    private Double privateAcres;
    private Double totalAcres;

    // -------------------------
    // Location
    // -------------------------
    private String place;
    private String dungkhag;
    private String dzongkhag;
    private String gewog;
    private String nearestVillage;

    // -------------------------
    // Attachments (Reference IDs)
    // -------------------------
    private Long explorationProposalFileId;
    private Long cvFileId;
    private Long consentLetterFileId;
    private Long locationMapFileId;

    // -------------------------
    // Status & Audit
    // -------------------------
    @Enumerated(EnumType.STRING)
    private ExplorationApplicationStatus status;

    private Integer revisionNo;

    @CreationTimestamp
    private LocalDateTime createdOn;

    private LocalDateTime submittedOn;

    private LocalDateTime lastResubmittedOn;
    private String lastResubmittedBy;

    // ---- Assignment ----
    private Long assignedMpcdUserId;
    private Long assignedGeologistUserId;

    private LocalDateTime assignedMpcdOn;
    private LocalDateTime assignedGeologistOn;

    // --- Status for an application from both side --- //
    private Boolean mpcdApproved;
    private Boolean geologistApproved;

    @Column(name = "approved_date")
    private LocalDate approvedDate;

    @Column(name = "permit_end_date")
    private LocalDate permitEndDate;

    // Region_id Saved for reporting purpose
    @Column(name = "region_id")
    private Long regionId;

}
