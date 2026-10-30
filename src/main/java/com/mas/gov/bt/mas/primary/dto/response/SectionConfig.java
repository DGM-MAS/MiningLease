package com.mas.gov.bt.mas.primary.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SectionConfig {
    private String sectionKey;
    private String label;
    private List<FieldMetadata> fields;
}
