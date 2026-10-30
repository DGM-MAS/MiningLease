package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MineRestorationCompletionReportRequest {

    @NotBlank(message = "Restoration application number is required")
    private String restorationApplicationNumber;

    // Background
    private String locationImageDocId;

    // Activities undertaken as JSON: [{slNo, activityDescription, duration, cost}]
    private String activitiesUndertaken;

    // Attachments
    private String remarks;
    private String pictorialEvidenceDocId;
    private String mapsAndPlansDocId;
    private String otherDocId;

    // "DRAFT" or "SUBMITTED"
    private String status;
}
