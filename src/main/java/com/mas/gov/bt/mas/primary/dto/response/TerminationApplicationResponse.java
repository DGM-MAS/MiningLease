package com.mas.gov.bt.mas.primary.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Data
public class TerminationApplicationResponse {

    private Long id;

    private String applicationNumber;

    private Long promoterUserId;

    private Long fileId;

    private String remarksChief;

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
