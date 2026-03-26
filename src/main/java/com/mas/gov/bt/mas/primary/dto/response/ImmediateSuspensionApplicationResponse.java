package com.mas.gov.bt.mas.primary.dto.response;

import jakarta.persistence.Column;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Data
public class ImmediateSuspensionApplicationResponse {

    private Long id;

    private String applicationNumber;

    private Long promoterUserId;

    private String applicantEmail;

    private String applicantName;

    private String remarksRcMi;

    private LocalDateTime rcMiReviewedAt;

    private LocalDateTime promoterReviewedAt;

    private Long promoterFileId;

    private LocalDateTime miReviewedAt;

    private Long miFileId;

    private String currentStatus;
    private String currentStatusDisplayName;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;

}
