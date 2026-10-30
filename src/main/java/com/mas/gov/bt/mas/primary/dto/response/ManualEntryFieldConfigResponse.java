package com.mas.gov.bt.mas.primary.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ManualEntryFieldConfigResponse {
    private String activityType;
    private String label;
    private List<SectionConfig> sections;
}
