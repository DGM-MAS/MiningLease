package com.mas.gov.bt.mas.primary.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@Data
public class TerminationApplicationResponse {

    private Long id;

    private String applicationNumber;

    private List<String> applicationNumbers;

    private String terminationId;

    private Long promoterUserId;

    private Long promoterFileId;

    private String applicantEmail;

    private String applicantName;

    private Long fileId;

    private boolean permanentTermination;

    private LocalDate terminationEndDate;

    private String remarksChief;

    private LocalDateTime chiefReviewedAt;

    private String remarksCMSHead;

    private LocalDateTime cmsHeadReviewedAt;

    private Long cmsHeadFileId;

    // ========== Status & Workflow ==========
    private String currentStatus;

    // ========== Audit Fields ==========
    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;
}
