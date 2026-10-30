package com.mas.gov.bt.mas.primary.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MineRestorationCompletionReportResponse {

    private Long id;
    private String restorationApplicationNumber;

    // Background
    private String nameOfMine;
    private String leaseAreaAcres;
    private String nameOfLessee;
    private String locationImageDocId;

    // Activities undertaken (JSON string)
    private String activitiesUndertaken;

    // Attachments
    private String remarks;
    private String pictorialEvidenceDocId;
    private String mapsAndPlansDocId;
    private String otherDocId;

    // ME review
    private String meRemarks;
    private LocalDateTime meReviewedAt;

    private String status;
    private LocalDateTime createdOn;
}
