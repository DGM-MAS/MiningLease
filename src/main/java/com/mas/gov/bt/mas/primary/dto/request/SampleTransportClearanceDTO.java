package com.mas.gov.bt.mas.primary.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleTransportClearanceDTO {

    private String applicantName;
    private String contactNo;
    private String emailAddress;
    private String rockMineralName;
    private Integer sampleCount;
    private String sampleForm;
    private String sampleFormSpecify;
    private Double totalWeight;
    private String weightUnit;
    private String shippingPurpose;
    private String shippingMode;
    private String destination;
}