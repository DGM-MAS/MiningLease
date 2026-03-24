package com.mas.gov.bt.mas.primary.dto.response;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class TemporaryClosureNotificationResponse {

    private Long id;
    private String applicationId;
    private Long applicantUserId;
    private String applicantType;
    private String applicantCid;
    private String applicantName;
    private String applicantEmail;
    private Long applicantFileId;

    private String reasonForClosure;

    private Long numberOfMonthsForClosure;

    private String remarksApplicant;

    // Status
    private String currentStatus;
    private String currentStatusDisplayName;

    // Timestamps
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;

    // MI Review
    private String remarksMI;
    private Long fileIdMI;

    // RC Review
    private String remarksRC;
    private Long fileUploadIdRC;







}
