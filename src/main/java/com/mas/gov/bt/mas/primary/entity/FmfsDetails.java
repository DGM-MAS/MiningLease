package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_fmfs_details", schema = "mas_db")
@Getter
@Setter
public class FmfsDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String applicantCid;
    private String applicantContact;
    private String applicantEmail;
    private String applicantName;
    private String applicantType;
    private Long applicantUserId;

    @Column(nullable = false, length = 30)
    private String applicationNumber;

    private LocalDateTime approvedAt;

    private String businessLicenseNo;
    private String companyName;

    private LocalDateTime createdAt;
    private Long createdBy;

    private String currentStatus;
    private String dungkhag;
    private String dzongkhag;
    private String gewog;

    private Boolean isActive;

    private String landPrivate;

    private LocalDate leaseEndDate;
    private Integer leasePeriodYears;
    private LocalDate leaseStartDate;

    private String licenseNo;
    private String nearestVillage;
    private String placeOfMiningActivity;

    @Column(length = 500)
    private String postalAddress;

    private String proposedLeasePeriod;
    private LocalDateTime rejectedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    private String requiredInvestment;
    private String sourceOfFinance;
    private String srf;

    private LocalDateTime submittedAt;

    @Column(columnDefinition = "TEXT")
    private String technicalCompetenceExperience;

    private String telephoneNo;
    private String totalLand;
    private String typeOfMineralsProducts;
    private String typeOfMines;

    private LocalDateTime updatedAt;
    private Long updatedBy;

    @Column(columnDefinition = "TEXT")
    private String workforceRequirementRecruitment;

    private Long applicationMasterId;
    private String applicationType;
    private Boolean applicationFeesRequired;

    private String consentLetterDocId;
    private String explorationReportDocId;
    private String financialCapabilityDocId;
    private String fmfsDocId;
    private String geologicalReportDocId;
    private String locationMapDocId;
    private String pfsDocId;

    private Long mpcdFileUploadIdPa;

    private String remarksMpcd;
    private String remarksGeologist;

    private String approvedArea;
    private String approvedErb;
    private String approvedLeasePeriod;
    private String approvedMineral;

    private LocalDateTime chiefReviewedAt;
    private LocalDateTime directorReviewedAt;

    private String fmfsStatus;
    private String geologicalReportStatus;

    private LocalDateTime geologistReviewedAt;

    private String llcDocId;

    private LocalDateTime meReviewedAt;

    private String mlaDocId;

    private LocalDateTime mlaSignedAt;

    private String mlaSignedDocId;
    private String mlaStatus;

    private LocalDateTime mpcdReviewedAt;

    private String notesheetDocId;

    @Column(columnDefinition = "TEXT")
    private String remarksChief;

    @Column(columnDefinition = "TEXT")
    private String remarksDirector;

    @Column(columnDefinition = "TEXT")
    private String remarksMe;

    private Long mpcdFileUploadIdMa;

    private Long fileUploadIdGr;

    private String fileUploadIdPaFc;

    private String fmfsId;

    private String companyRegistrationNo;
    private String companyType;

    private String ecStatus;

    private LocalDateTime ecExpiryDate;

    private Long bankGurantorDocId;

    @Column(precision = 38, scale = 2)
    private BigDecimal upfrontPaymentAmount;

    private Long workOrderDocId;

    private String workOrderRemarks;

    private String dzongkhagId;

    private Integer gewogId;

    private Integer villageId;

    @Column(precision = 38, scale = 2)
    private BigDecimal applicationFeesAmount;

    private String fileUploadIdFc;

    private String fileUploadIdPa;

    private String fileUploadIdPublicClearance;

    @Column(length = 500)
    private String ecNo;

    @Column(columnDefinition = "TEXT")
    private String description;
}