package com.mas.gov.bt.mas.primary.dto.request;

import jakarta.persistence.Column;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentClearanceRenewalRequestDTO {

    private Long id; // for draft update

    private String siteApplicationNo;

    private String serviceType;

    private String location;

    private String area;

    // uploaded earlier via file service
    private Long previousEcFileId;

    private Long selfMonitoringReportFileId;

    private String ecNumber;

    private Date ecExpiryDate;

    private String ecFileId;

}