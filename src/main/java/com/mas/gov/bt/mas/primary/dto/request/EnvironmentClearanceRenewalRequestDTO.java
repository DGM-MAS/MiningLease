package com.mas.gov.bt.mas.primary.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentClearanceRenewalRequestDTO {

    private Long id; // for draft update

    private String serviceType;

    private String location;

    private String area;

    // uploaded earlier via file service
    private Long previousEcFileId;

    private Long selfMonitoringReportFileId;
}