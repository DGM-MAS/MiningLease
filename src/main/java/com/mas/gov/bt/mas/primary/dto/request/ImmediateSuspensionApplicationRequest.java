package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ImmediateSuspensionApplicationRequest {

    private String applicationNumber;
    private String applicationFrom;
    private Long suspensionReasonId;
    private String rcMiRemark;
}
