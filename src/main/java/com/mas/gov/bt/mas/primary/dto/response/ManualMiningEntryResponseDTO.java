package com.mas.gov.bt.mas.primary.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualMiningEntryResponseDTO {

    private Long id;
    private String applicationNo;
    private String details;
    private String activityType;

    private List<String> fileIds;
}