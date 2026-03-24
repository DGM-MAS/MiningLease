package com.mas.gov.bt.mas.primary.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemporaryClosureNotificationRequest {
    private Long fileId;
    private String applicationId;
    private String remarksApplicant;
    private String reasonForClosure;
    private Long numberOfMonthsForClosure;
}
