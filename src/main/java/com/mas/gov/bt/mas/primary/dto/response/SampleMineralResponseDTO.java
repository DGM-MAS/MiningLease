package com.mas.gov.bt.mas.primary.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleMineralResponseDTO {

    private Long id;

    private String rockMineralName;
    private String rockMineralNameSpecify;

    private Integer sampleCount;

    private String sampleForm;
    private String sampleFormSpecify;

    private Double totalWeight;
    private String weightUnit;
}