package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PromoterImmediateSuspensionRequest {

    private Long id;
    private Long fileId;
    private String status;
    private String remarks;
}
