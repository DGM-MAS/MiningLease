package com.mas.gov.bt.mas.primary.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleMineralDTO {

    private String rockMineralName;
    private String rockMineralNameSpecify;

    private Integer sampleCount;

    private String sampleForm;
    private String sampleFormSpecify;

    private Double totalWeight;
    private String weightUnit;
}