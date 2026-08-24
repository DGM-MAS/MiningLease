package com.mas.gov.bt.mas.primary.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class TerminationApplicationDataRequest {

    private String applicationNo;

    private String terminationId;
}
