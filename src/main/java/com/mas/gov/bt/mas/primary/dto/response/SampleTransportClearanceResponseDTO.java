package com.mas.gov.bt.mas.primary.dto.response;

import jakarta.persistence.Column;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleTransportClearanceResponseDTO {

    private Long id;
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
    private Long createdBy;

    private LocalDateTime createdOn;

    private Long updatedBy;

    private LocalDateTime updatedOn;

    private String status;
}