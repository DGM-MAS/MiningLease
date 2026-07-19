package com.mas.gov.bt.mas.primary.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentClearanceRenewalResponseDTO {

    private Long id;

    private String applicationNo;

    private String siteApplicationNo;

    private String serviceType;

    private String location;

    private String area;

    private Long previousEcFileId;

    private Long selfMonitoringReportFileId;

    private String ecNumber;

    private Date ecExpiryDate;

    private String ecFileId;

    private String status;

    // Stage that most recently requested a revision ("MPCD" | "RC" | "MI" | "MD")
    private String pendingRevisionStage;

    private Long assignedMPCDId;

    private String remarkMPCD;

    private Long mpcdSiteReportFileId;

    private LocalDateTime mpcdReportSubmittedOn;

    private LocalDateTime mpcdApprovedOn;

    private Long assignedRCId;

    private String remarkRC;

    private Long rcSiteReportFileId;

    private LocalDateTime rcReportSubmittedOn;

    private Long assignedMIId;

    private String remarkMI;

    private Long miSiteReportFileId;

    private LocalDateTime miReportSubmittedOn;

    private Long assignedMDId;

    private String remarkMD;

    private Long feeReceiptFileId;

    private LocalDateTime paymentCompletedOn;

    private LocalDateTime mdApprovedOn;

    private Long ecCertificateFileId;

    private LocalDateTime ecGeneratedOn;

    private Long iomFileId;

    private LocalDateTime iomSubmittedOn;

    private LocalDateTime submittedOn;

    private LocalDateTime createdOn;

    private Long createdBy;

    private LocalDateTime updatedOn;

    private String updatedBy;

    // Not entity-backed — set manually by payEcFee() when a payment redirect is needed
    private String redirectUrl;
}