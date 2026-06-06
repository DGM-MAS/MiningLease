package com.mas.gov.bt.mas.primary.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FieldMetadata {
    private String name;
    private String label;
    // TEXT | EMAIL | PHONE | NUMBER | DECIMAL | DATE | BOOLEAN | FILE_ID | TEXTAREA | MULTI_SELECT
    private String type;
    private boolean required;
    private String hint;
}
