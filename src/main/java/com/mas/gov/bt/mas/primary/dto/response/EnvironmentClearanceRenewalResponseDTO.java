package com.mas.gov.bt.mas.primary.dto.response;

import lombok.*;

import java.time.LocalDateTime;

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

    private String status;

    private LocalDateTime submittedOn;
}