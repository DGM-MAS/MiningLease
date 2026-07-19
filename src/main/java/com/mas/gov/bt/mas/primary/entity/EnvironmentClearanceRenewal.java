package com.mas.gov.bt.mas.primary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "t_environment_clearance_renewal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentClearanceRenewal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String siteApplicationNo;

    private String applicationNo;

    private String serviceType;

    private String location;

    private String area;

    // references to uploaded files
    private Long previousEcFileId;

    private Long selfMonitoringReportFileId;

    private String ecNumber;

    private Date ecExpiryDate;

    @Column(name = "ec_file_id")
    private String ecFileId;

    // Reference to master application
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_master_id")
    private ApplicationMaster applicationMaster;

    // Status Code
    private String status;

    // Stage that most recently requested a revision ("MPCD" | "RC" | "MI" | "MD"),
    // used to route a resubmitted application back to that same stage
    private String pendingRevisionStage;

    // Assigned focal
    private Long assignedMPCDId;

    @Column(name = "remark_mpcd", columnDefinition = "TEXT")
    private String remarkMPCD;


    // MPCD review document
    private Long mpcdSiteReportFileId;

    // MPCD report submitted timestamp
    private LocalDateTime mpcdReportSubmittedOn;

    // MPCD approval timestamp
    private LocalDateTime mpcdApprovedOn;

    // Assigned Regional Coordinator (RC)
    private Long assignedRCId;

    @Column(name = "remark_rc", columnDefinition = "TEXT")
    private String remarkRC;

    // RC site assessment report file reference
    private Long rcSiteReportFileId;

    // RC report submitted timestamp
    private LocalDateTime rcReportSubmittedOn;

    private Long assignedMIId;

    @Column(name = "remark_mi", columnDefinition = "TEXT")
    private String remarkMI;

    private Long miSiteReportFileId;

    private LocalDateTime miReportSubmittedOn;


    // MD stage

    private Long assignedMDId;

    @Column(name = "remark_md", columnDefinition = "TEXT")
    private String remarkMD;

    @Column(name = "md_fee_receipt_file_id")
    private Long feeReceiptFileId;

    private LocalDateTime paymentCompletedOn;

    private LocalDateTime mdApprovedOn;

    private Long ecCertificateFileId;

    private LocalDateTime ecGeneratedOn;

    // System generated IOM file reference
    private Long iomFileId;

    // IOM submitted to MD
    private LocalDateTime iomSubmittedOn;


    // Audit track
    private LocalDateTime submittedOn;

    private LocalDateTime createdOn;

    private Long createdBy;

    private LocalDateTime updatedOn;

    private String updatedBy;

    @PrePersist
    public void prePersist() {
        createdOn = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedOn = LocalDateTime.now();
    }
}