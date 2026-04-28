package com.mas.gov.bt.mas.primary.dto.request;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualMiningEntryRequestDTO {

    private String details;
    private String activityType;

    private Long promoterId;
    // multiple uploaded file references
    private List<String> fileIds;
}